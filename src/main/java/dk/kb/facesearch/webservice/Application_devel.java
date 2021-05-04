package dk.kb.facesearch.webservice;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import dk.kb.facesearch.api.devel.impl.DanerFaceSearchApiServiceImpl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class Application_devel extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<>(Arrays.asList(
                JacksonJsonProvider.class,
                DanerFaceSearchApiServiceImpl.class,
                ServiceExceptionMapper.class
        ));
    }


}
