package eu.sevel.quarkus.admission;

import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.AdmissionReviewBuilder;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

@Path("/admission")
public class AdmissionController {

    @GET
    @Produces(TEXT_PLAIN)
    public String hello() {
        return "hello";
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public AdmissionReview validate(AdmissionReview review) {
        return new AdmissionReviewBuilder()
                .withResponse(new AdmissionResponseBuilder()
                        .withAllowed(true)
                        .withUid(review.getRequest().getUid())
                        .build())
                .build();
    }
}