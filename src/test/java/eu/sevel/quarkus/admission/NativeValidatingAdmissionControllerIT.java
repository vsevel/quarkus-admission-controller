package eu.sevel.quarkus.admission;

import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
public class NativeValidatingAdmissionControllerIT extends ValidatingAdmissionControllerTest {

    // Execute the same tests but in native mode.
}