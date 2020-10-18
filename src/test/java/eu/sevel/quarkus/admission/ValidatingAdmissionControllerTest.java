package eu.sevel.quarkus.admission;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
public class ValidatingAdmissionControllerTest {
    InputStream whichFileToSendAsBody =
            getClass().getClassLoader().getResourceAsStream("example-review.json");

    @Test
    public void validate() {
        given()
                .when()
                .body(whichFileToSendAsBody)
                .contentType(APPLICATION_JSON)
                .post("/validate")
                .then()
                .statusCode(200)
                .body(containsString("\"allowed\":true"));
    }

}