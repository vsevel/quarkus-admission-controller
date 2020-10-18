package eu.sevel.quarkus.admission;

import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.client.utils.Serialization;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Provider
@Consumes(APPLICATION_JSON)
public class AdmissionReviewMessageBodyReader implements MessageBodyReader<AdmissionReview> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == AdmissionReview.class;
    }

    @Override
    public AdmissionReview readFrom(Class<AdmissionReview> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        //without the extra step of wrapping the unmarshal will hit an error.
        return Serialization.unmarshal(new String(entityStream.readAllBytes()), AdmissionReview.class);
    }
}
