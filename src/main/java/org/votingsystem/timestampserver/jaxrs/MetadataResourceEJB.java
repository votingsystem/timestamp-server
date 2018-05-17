package org.votingsystem.timestampserver.jaxrs;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import eu.europa.esig.dss.token.AbstractSignatureTokenConnection;
import org.votingsystem.crypto.TSPHttpSource;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.throwable.XMLValidationException;
import org.votingsystem.timestampserver.ejb.ConfigEJB;
import org.votingsystem.util.Messages;
import org.votingsystem.xades.XAdESSignature;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/metadata")
@Stateless
public class MetadataResourceEJB {

    private static final Logger log = Logger.getLogger(MetadataResourceEJB.class.getName());


    @EJB private ConfigEJB config;

    @GET @Path("/")
    @Produces({"application/xml"})
    public Response getMetadata(@Context HttpServletRequest req) throws Exception {
        byte[] metadataSigned = getMetadataSigned();
        return Response.ok().entity(metadataSigned).build();
    }

    public byte[] getMetadataSigned() throws Exception {
        MetadataDto metadata = config.getMetadata();
        byte[] metadataBytes = new XmlMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(metadata);
        AbstractSignatureTokenConnection signingToken = config.getSigningToken();
        return new XAdESSignature().sign(metadataBytes, signingToken,
                new TSPHttpSource(config.getTimestampServiceURL()));
    }

    public MetadataDto getMetadataFromURL(String metadataURL) throws IOException, XMLValidationException {
        ResponseDto response = HttpConn.getInstance().doGetRequest(metadataURL, null);
        MetadataDto metadata = null;
        try {
            metadata = new XmlMapper().readValue(response.getMessageBytes(), MetadataDto.class);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new XMLValidationException(Messages.currentInstance().get("invalidMetadataMsg"));
        }
        return metadata;
    }

}
