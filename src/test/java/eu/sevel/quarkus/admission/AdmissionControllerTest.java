package eu.sevel.quarkus.admission;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class AdmissionControllerTest {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/admission")
          .then()
             .statusCode(200)
             .body(is("hello"));
    }

}