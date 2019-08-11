package eu.sevel.quarkus.admission;

import io.fabric8.kubernetes.api.model.admission.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.AdmissionReviewBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonPatch;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Base64;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/mutate")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class MutatingAdmissionController {

    private static final Logger log = LoggerFactory.getLogger(MutatingAdmissionController.class);

    @POST
    public AdmissionReview validate(AdmissionReview review) {

        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().setProperty(JsonbConfig.FORMATTING, true));
        log.info("received admission review: {}", jsonb.toJson(review));


        AdmissionRequest request = review.getRequest();
        if (request.getOperation().equals("CREATE") && request.getObject() instanceof Deployment) {

            Map<String, String> labels = request.getObject().getMetadata().getLabels();
            if (!labels.containsKey("tutu")) {
//                JsonObject source = Json.createParser(new StringReader(jsonb.toJson(review))).getObject();
//                labels.put("my", "name");
//                JsonObject target = Json.createParser(new StringReader(jsonb.toJson(review))).getObject();
//                JsonPatch patch = Json.createDiff(source, target);
//                JsonArray jsonArray = patch.toJsonArray();
//                String jsonPatch = jsonArray.toString();
//                log.info("jsonPatch = {}", jsonPatch);

                String patch = "[{\"op\": \"add\", \"path\": \"/metadata/labels/tutu\", \"value\": \"tata\"}]";
                String encoded = Base64.getEncoder().encodeToString(patch.getBytes());
                log.info("patching with {} => {}", patch, encoded);

                return new AdmissionReviewBuilder()
                        .withResponse(new AdmissionResponseBuilder()
                                .withAllowed(true)
                                .withUid(request.getUid())
                                .withPatchType("JSONPatch")
                                .withPatch(encoded)
                                .build())
                        .build();
            }

        }

        return new AdmissionReviewBuilder()
                .withResponse(new AdmissionResponseBuilder()
                        .withAllowed(true)
                        .withUid(request.getUid())
                        .build())
                .build();
    }
}