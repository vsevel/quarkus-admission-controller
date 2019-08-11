package eu.sevel.quarkus.admission;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class ValidatingAdmissionControllerTest {

    String review = "{\"additionalProperties\":{},\"apiVersion\":\"admission.k8s.io/v1beta1\",\"kind\":\"AdmissionReview\",\"request\":{\"additionalProperties\":{},\"kind\":{\"additionalProperties\":{},\"group\":\"extensions\",\"kind\":\"Deployment\",\"version\":\"v1beta1\"},\"name\":\"httpbin\",\"namespace\":\"test-admission\",\"operation\":\"UPDATE\",\"resource\":{\"additionalProperties\":{},\"group\":\"extensions\",\"resource\":\"deployments\",\"version\":\"v1beta1\"},\"uid\":\"75a55056-bc03-11e9-82d4-025000000001\",\"userInfo\":{\"additionalProperties\":{},\"groups\":[\"system:masters\",\"system:authenticated\"],\"username\":\"docker-for-desktop\"}}}";

    @Test
    public void validate() {
        given()
                .when()
                .body(review)
                .contentType(APPLICATION_JSON)
                .post("/validate")
                .then()
                .statusCode(200)
                .body(containsString("\"allowed\":true"));
    }

}