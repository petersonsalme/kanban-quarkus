package org.seariver.kanbanboard.write.adapter.in;

import com.github.jsontemplate.JsonTemplate;
import helper.BlankStringValueProducer;
import helper.IntegrationHelper;
import helper.UuidStringValueProducer;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.seariver.kanbanboard.write.adapter.out.WriteBucketRepositoryImpl;

import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@QuarkusTest
class BucketMoveIT extends IntegrationHelper {

    public static final String ENDPOINT_PATH = "/v1/buckets/{bucketExternalId}/move";

    @Test
    void GIVEN_ValidPayload_MUST_UpdateSuccessful() {

        // setup
        var existentBucketExternalId = "3731c747-ea27-42e5-a52b-1dfbfa9617db";
        var newPosition = 1.23;
        var template = "{ position : $position }";
        var payload = new JsonTemplate(template)
                .withVar("position", newPosition)
                .prettyString();

        // verify
        given()
                .contentType(JSON)
                .body(payload).log().body()
                .when()
                .patch(ENDPOINT_PATH, existentBucketExternalId)
                .then()
                .statusCode(NO_CONTENT.getStatusCode());

        var repository = new WriteBucketRepositoryImpl(dataSource);
        var actualBucket = repository.findByExternalId(UUID.fromString(existentBucketExternalId)).get();
        assertThat(actualBucket.getPosition()).isEqualTo(newPosition);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidPositions")
    void GIVEN_InvalidData_MUST_ReturnBadRequest(String jsonTemplate,
                                                 String errorMessage,
                                                 String[] errorsFields,
                                                 String[] errorsDetails) {
        // setup
        String existentBucketExternalId = "3731c747-ea27-42e5-a52b-1dfbfa9617db";

        var payload = new JsonTemplate(jsonTemplate)
                .withValueProducer(new UuidStringValueProducer())
                .withValueProducer(new BlankStringValueProducer())
                .prettyString();

        // verify
        given()
                .contentType(JSON)
                .body(payload).log().body()
                .when()
                .patch(ENDPOINT_PATH, existentBucketExternalId)
                .then()
                .statusCode(BAD_REQUEST.getStatusCode())
                .contentType(JSON)
                .assertThat()
                .log().body()
                .body("message", is(errorMessage),
                        "errors.field", containsInAnyOrder(errorsFields),
                        "errors.detail", containsInAnyOrder(errorsDetails));
    }

    @Test
    void GIVEN_NotExistentExternalId_MUST_ReturnBadRequest() {

        // setup
        var notExistentBucketExternalId = "effce142-1a08-49d4-9fe6-3fe728b17a41";

        var template = "{" +
                "  position : @f" +
                "}";

        var payload = new JsonTemplate(template).prettyString();

        // verify
        given()
                .contentType(JSON)
                .body(payload).log().body()
                .when()
                .patch(ENDPOINT_PATH, notExistentBucketExternalId)
                .then()
                .statusCode(NOT_FOUND.getStatusCode())
                .contentType(JSON)
                .assertThat()
                .log().body()
                .body("message", is(NOT_FOUND.getReasonPhrase()),
                        "errors.field", containsInAnyOrder("code"),
                        "errors.detail", containsInAnyOrder("1001"));
    }

    @Test
    void GIVEN_DuplicatedPosition_MUST_ReturnBadRequest() {

        // setup
        var existentBucketExternalId = "3731c747-ea27-42e5-a52b-1dfbfa9617db";
        var alreadyInUsePosition = 100.15;

        var template = String.format("{" +
                "  position : %s" +
                "}", alreadyInUsePosition);

        var payload = new JsonTemplate(template).prettyString();

        // verify
        given()
                .contentType(JSON)
                .body(payload).log().body()
                .when()
                .patch(ENDPOINT_PATH, existentBucketExternalId)
                .then()
                .statusCode(BAD_REQUEST.getStatusCode())
                .contentType(JSON)
                .assertThat()
                .log().body()
                .body("message", is("Invalid parameter"),
                        "errors.field", containsInAnyOrder("code"),
                        "errors.detail", containsInAnyOrder("1000"));
    }

    private static Stream<Arguments> provideInvalidPositions() {

        return Stream.of(
                arguments("{position:null}", "Invalid parameter",
                        args("position"), args("must be greater than 0")),
                arguments("{position:@s}", "Invalid format",
                        args("position"), args("double")),
                arguments("{position:0}", "Invalid parameter",
                        args("position"), args("must be greater than 0")),
                arguments("{position:-1}", "Invalid parameter",
                        args("position"), args("must be greater than 0")),
                arguments("{notExistent:@f}", "Invalid parameter",
                        args("position"), args("must be greater than 0"))
        );
    }
}
