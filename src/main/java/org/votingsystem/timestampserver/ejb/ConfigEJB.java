package org.votingsystem.timestampserver.ejb;

import eu.europa.esig.dss.token.AbstractSignatureTokenConnection;
import eu.europa.esig.dss.token.JKSSignatureToken;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.votingsystem.crypto.KeyGenerator;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.dto.metadata.MetadataUtils;
import org.votingsystem.dto.metadata.TrustedEntitiesDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.SystemEntityType;
import org.votingsystem.service.TimeStampService;
import org.votingsystem.service.impl.TimeStampServiceImpl;
import org.votingsystem.util.*;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Named;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
@Named(value="config")
@Startup
public class ConfigEJB {

    private static final Logger log = Logger.getLogger(ConfigEJB.class.getName());

    public static final String DEFAULT_APP_HOME = "/var/local/timestamp-server";
    public static final Integer DEFAULT_METADATA_LIVE_IN_HOURS = 1;

    private String entityId;
    private String timestampServiceURL;
    private String applicationDirPath;

    private AbstractSignatureTokenConnection signingToken;
    private TimeStampService timeStampService;
    private MetadataDto metadata;
    private X509Certificate signingCert;
    private TrustedEntitiesDto trustedEntities;

    public ConfigEJB() {
        try {
            Security.addProvider(new BouncyCastleProvider());
            org.apache.xml.security.Init.init();
            KeyGenerator.INSTANCE.init(Constants.SIG_NAME, Constants.PROVIDER, Constants.KEY_SIZE, Constants.ALGORITHM_RNG);
            HttpConn.init(HttpConn.HTTPS_POLICY.ALL, null);

            applicationDirPath = System.getProperty("timestamp_server_dir");
            if(StringUtils.isEmpty(applicationDirPath))
                applicationDirPath = DEFAULT_APP_HOME;
            log.info("applicationDirPath: " + applicationDirPath);
            Properties properties = new Properties();
            properties.load(new FileInputStream(new File(applicationDirPath + "/config.properties")));

            timestampServiceURL = OperationType.TIMESTAMP_REQUEST.getUrl((String)properties.get("timestampServerURL"));
            entityId = (String)properties.get("entityId");
            log.info("entityId: " + entityId + " - timestampServiceURL: " + timestampServiceURL +
                    " - defaultMetadataLiveInHours: " + DEFAULT_METADATA_LIVE_IN_HOURS);

            properties = new Properties();
            properties.load(new FileInputStream(new File(applicationDirPath + "/sec/keystore.properties")));

            String keyStorePassword = properties.getProperty("keyStorePassword");
            String keyStoreFileName = properties.getProperty("keyStoreFileName");
            byte[] keyStoreBytes = FileUtils.getBytesFromFile(new File(applicationDirPath + "/sec/"+ keyStoreFileName));
            timeStampService = new TimeStampServiceImpl(keyStoreBytes, keyStorePassword);

            signingToken = new JKSSignatureToken(new FileInputStream(applicationDirPath + "/sec/" + keyStoreFileName),
                   new KeyStore.PasswordProtection(keyStorePassword.toCharArray()));
            signingCert = signingToken.getKeys().get(0).getCertificate().getCertificate();
            trustedEntities = TrustedEntitiesDto.loadTrustedEntities(applicationDirPath + "/sec/trusted-entities.xml");
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public String getEntityId() {
        return entityId;
    }

    public String getApplicationDirPath() {
        return applicationDirPath;
    }

    public TimeStampService getTimeStampService() {
        return timeStampService;
    }

    public AbstractSignatureTokenConnection getSigningToken() {
        return signingToken;
    }

    public MetadataDto getMetadata() {
        try {
            if(metadata == null) {
                Properties properties = new Properties();
                properties.load(new FileInputStream(new File(applicationDirPath + "/config.properties")));
                metadata = MetadataUtils.initMetadata(SystemEntityType.TIMESTAMP_SERVER, entityId, properties,
                        signingCert, signingCert);
                metadata.setTrustedEntities(trustedEntities);
            }
            metadata.setValidUntil(ZonedDateTime.now().plus(DEFAULT_METADATA_LIVE_IN_HOURS, ChronoUnit.HOURS)
                    .toInstant().toString());
            return metadata;
        } catch (Exception ex) {
            throw new RuntimeException(Messages.currentInstance().get("invalidMetadataMsg") + " - " + ex.getMessage());
        }
    }

    public String getTimestampServiceURL() {
        return timestampServiceURL;
    }
}
