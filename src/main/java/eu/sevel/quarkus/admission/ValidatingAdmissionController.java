package eu.sevel.quarkus.admission;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.AdmissionReviewBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/validate")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class ValidatingAdmissionController {

    private static final Logger log = LoggerFactory.getLogger(ValidatingAdmissionController.class);

    @POST
    public AdmissionReview validate(AdmissionReview review) {

        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().setProperty(JsonbConfig.FORMATTING, true));
        log.info("received admission review: {}", jsonb.toJson(review));

        return new AdmissionReviewBuilder()
                .withResponse(new AdmissionResponseBuilder()
                        .withAllowed(true)
                        .withUid(review.getRequest().getUid())
                        .build())
                .build();
    }
}