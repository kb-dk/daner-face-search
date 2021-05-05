package dk.kb.facesearch.api.v1.impl;

import dk.kb.facesearch.WolframFaces;
import dk.kb.facesearch.api.v1.DanerFaceSearchApi;
import dk.kb.facesearch.model.v1.BoundingBoxDto;
import dk.kb.facesearch.model.v1.FaceDto;
import dk.kb.facesearch.model.v1.SimilarDto;
import dk.kb.facesearch.model.v1.SimilarResponseDto;
import dk.kb.facesearch.webservice.exception.InternalServiceException;
import dk.kb.facesearch.webservice.exception.InvalidArgumentServiceException;
import dk.kb.facesearch.webservice.exception.ServiceException;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import java.util.ArrayList;
import java.util.List;

/**
 * daner-face-search
 *
 * <p>Wolfram backed visual similiarity based face search. Implemented specifically for the DANER project. 
 *
 */
public class FaceSearchImpl implements DanerFaceSearchApi {
    private Logger log = LoggerFactory.getLogger(this.toString());



    /* How to access the various web contexts. See https://cxf.apache.org/docs/jax-rs-basics.html#JAX-RSBasics-Contextannotations */

    @Context
    private transient UriInfo uriInfo;

    @Context
    private transient SecurityContext securityContext;

    @Context
    private transient HttpHeaders httpHeaders;

    @Context
    private transient Providers providers;

    @Context
    private transient Request request;

    @Context
    private transient ContextResolver contextResolver;

    @Context
    private transient HttpServletRequest httpServletRequest;

    @Context
    private transient HttpServletResponse httpServletResponse;

    @Context
    private transient ServletContext servletContext;

    @Context
    private transient ServletConfig servletConfig;

    @Context
    private transient MessageContext messageContext;


    /**
     * Request images similar to the uploaded image
     * 
     * @param imageURL: URl to the image to use as source for the similarity search
     * 
     * @param maxMatches: The maximum number of similar images to return
     * 
     * @return <ul>
      *   <li>code = 200, message = "An array of arrays of image IDs and distances", response = SimilarResponseDto.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public SimilarResponseDto findSimilarFaces(String imageURL, Integer maxMatches) throws ServiceException {
        int realMaxMatches = maxMatches == null || maxMatches < 1 || maxMatches > 100 ? 10 : maxMatches;
        if (imageURL == null) {
            throw new InvalidArgumentServiceException("No ImageURL provided");
        }
        // TODO: Implement getSimilarFaces and use that instead of the mock
        return WolframFaces.getSimilarFaces(imageURL, "JPG", realMaxMatches);

        //return getSimilarResponseDtoMock();
    }

    private SimilarResponseDto getSimilarResponseDtoMock() {
        try{
            SimilarResponseDto response = new SimilarResponseDto();
        response.setImageURL("B6XAP");
        List<FaceDto> faces = new ArrayList<>();
        FaceDto faces2 = new FaceDto();
        faces2.setIndex(-20084269);
        BoundingBoxDto boundingBox = new BoundingBoxDto();
        boundingBox.setX(-768335271);
        boundingBox.setY(-1835550552);
        boundingBox.setWidth(-1928721994);
        boundingBox.setHeight(-20494705);
        faces2.setBoundingBox(boundingBox);
        List<SimilarDto> similars = new ArrayList<>();
        SimilarDto similars2 = new SimilarDto();
        similars2.setDistance(5797723607844708765.3727446001647705D);
        similars2.setId("X5HT3e7T");
        similars.add(similars2);
        faces2.setSimilars(similars);
        faces.add(faces2);
        response.setFaces(faces);
        response.setTechnote("cLlQ5");
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    }

    /**
     * Ping the server to check if the server is reachable.
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK", response = String.class</li>
      *   <li>code = 406, message = "Not Acceptable", response = ErrorDto.class</li>
      *   <li>code = 500, message = "Internal Error", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public String ping() throws ServiceException {
        // TODO: Implement...
    
        
        try{ 
            String response = "uhx4aJHLM";
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
    }


    /**
    * This method simply converts any Exception into a Service exception
    * @param e: Any kind of exception
    * @return A ServiceException
    * @see dk.kb.facesearch.webservice.ServiceExceptionMapper
    */
    private ServiceException handleException(Exception e) {
        if (e instanceof ServiceException) {
            return (ServiceException) e; // Do nothing - this is a declared ServiceException from within module.
        } else {// Unforseen exception (should not happen). Wrap in internal service exception
            log.error("ServiceException(HTTP 500):", e); //You probably want to log this.
            return new InternalServiceException(e.getMessage());
        }
    }

}
