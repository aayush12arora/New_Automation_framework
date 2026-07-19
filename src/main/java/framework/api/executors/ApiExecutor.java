package framework.api.executors;

import framework.utilities.LoggerUtil;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.slf4j.Logger;

/**
 * Minimal REST Assured executor. Wraps common HTTP verbs so tests
 * issue requests without repeating REST Assured boilerplate.
 */
public class ApiExecutor {

    private static final Logger LOG = LoggerUtil.getLogger(ApiExecutor.class);

    public Response get(String url) {
        LOG.info("GET {}", url);
        return RestAssured.given().when().get(url).then().extract().response();
    }

    public Response post(String url, Object body) {
        LOG.info("POST {}", url);
        return RestAssured.given()
                .contentType("application/json")
                .body(body)
                .when()
                .post(url)
                .then()
                .extract()
                .response();
    }
}
