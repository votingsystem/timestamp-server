package org.votingsystem.timestampserver.jaxrs;

import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.crypto.TimeStampResponseGeneratorHelper;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.http.ContentType;
import org.votingsystem.model.TimeStamp;
import org.votingsystem.timestampserver.ejb.ConfigEJB;
import org.votingsystem.util.FileUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/timestamp")
@Stateless
public class TimeStampResourceEJB {

    private static final Logger log = Logger.getLogger(TimeStampResourceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @EJB ConfigEJB config;

    @POST @Path("/")
    public void getTimestampResponse(@Context HttpServletRequest req, @Context HttpServletResponse res) 
            throws ServletException, IOException {
        processTimestampRequest(req, res, false);
    }

    @POST @Path("/discrete")
    public void getTimestampResponseDiscrete(@Context HttpServletRequest req, @Context HttpServletResponse res)
            throws ServletException, IOException {
        processTimestampRequest(req, res, true);
    }

    @GET @Path("/token/{id}")
    public Response lookupTimeStampById(@PathParam("id") long timeStampId) {
        TimeStamp timeStamp = em.find(TimeStamp.class, timeStampId);
        if(timeStamp != null) {
            return Response.ok(timeStamp.getTokenBytes()).type(ContentType.TIMESTAMP_RESPONSE.getName()).build();

        } else throw new NotFoundException("TimeStamp id '" + timeStampId + "' not found");
    }

    private void processTimestampRequest(@Context HttpServletRequest req, @Context HttpServletResponse res,
            boolean isDiscrete) throws ServletException, IOException {
        PrintWriter writer = null;
        String contentEncoding = req.getHeader("Content-Encoding");
        try {
            TimeStampResponseGeneratorHelper responseGenerator = null;
            InputStream requestInputStream = null;
            if("base64".equals(contentEncoding)) {
                byte[] requestBytesBase64 =  FileUtils.getBytesFromStream(req.getInputStream());
                byte[] requestBytes = Base64.getDecoder().decode(requestBytesBase64);
                requestInputStream = new ByteArrayInputStream(requestBytes);
            } else
                requestInputStream = req.getInputStream();
            if(isDiscrete)
                responseGenerator = getResponseGeneratorDiscrete(requestInputStream);
            else
                responseGenerator = getResponseGenerator(requestInputStream);

            TimeStampResponse timeStampResponse = responseGenerator.getTimeStampResponse();
            em.persist(new TimeStamp(responseGenerator.getSerialNumber().longValue(),
                    timeStampResponse.getTimeStampToken().getEncoded(), TimeStamp.State.OK));
            res.setContentType(ContentType.TIMESTAMP_RESPONSE.getName());
            final ServletOutputStream out = res.getOutputStream();

            if("base64".equals(contentEncoding)) {
                out.write(Base64.getEncoder().encode(timeStampResponse.getTimeStampToken().getEncoded()));
            } else out.write(timeStampResponse.getEncoded());
            out.flush();
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            res.setContentType(MediaType.TEXT_PLAIN);
            res.setStatus(ResponseDto.SC_ERROR_REQUEST);
            if(writer == null) writer = res.getWriter();
            writer.println(ex.getMessage());
        }
        if(writer != null) 
            writer.close();
    }
    
    public byte[] getSigningCertPEMBytes() throws IOException {
        return config.getTimeStampService().getSigningCertPEMBytes();
    }

    public TimeStampResponseGeneratorHelper getResponseGenerator(InputStream inputStream) throws Exception {
        return config.getTimeStampService().getResponseGenerator(inputStream);
    }

    public TimeStampResponseGeneratorHelper getResponseGeneratorDiscrete(InputStream inputStream) throws Exception {
        return config.getTimeStampService().getResponseGeneratorDiscrete(inputStream);
    }

    public byte[] getSigningCertChainPEMBytes() throws IOException {
        return config.getTimeStampService().getSigningCertChainPEMBytes();
    }

    public void validateToken(TimeStampToken timeStampToken) throws TSPException, IOException {
        config.getTimeStampService().validateToken(timeStampToken);
    }

    public byte[] getTimeStampRequest(byte[] digest) throws IOException {
        return config.getTimeStampService().getTimeStampRequest(digest);
    }

    public byte[] getTimeStampResponse(InputStream inputStream) throws Exception {
        return config.getTimeStampService().getTimeStampResponse(inputStream);
    }

}
