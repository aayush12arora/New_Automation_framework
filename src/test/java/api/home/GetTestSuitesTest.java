package api.home;

import framework.api.client.HomeClient;
import framework.base.BaseApiTest;
import io.restassured.response.Response;
import org.testng.annotations.Test;

/**
 * Tests for {@code GET /qe/project/test_suites}.
 *
 * <p>Mirrors the UI style: extends {@link BaseApiTest}, instantiates the client directly
 * ({@code new HomeClient()}), reads inputs from {@code customerData.get()} (loaded from
 * {@code testdata/<testName>.json}), and verifies through {@code assertions}. Covers positive,
 * negative (auth), and edge/validation scenarios.</p>
 */
public class GetTestSuitesTest extends BaseApiTest {

    private final HomeClient home = new HomeClient();

    @Test(groups = {"smoke", "regression"})
    public void getTestSuitesReturnsList() {
        Response response = home.getTestSuites(
                "ALL", 1, 10, "created_date", "desc", customerData.get().getProjectId());

        assertions.assertEquals(response.getStatusCode(), 200, "valid request should return 200");
        assertions.assertNotNull(response.jsonPath().getList("test_suites"), "response should contain test_suites");
        assertions.assertEquals(response.jsonPath().getInt("page"), 1, "page should be echoed back");
    }

    @Test(groups = {"regression"})
    public void getTestSuitesWithoutTokenReturns401() {
        Response response = home.getTestSuitesNoAuth(
                "ALL", 1, 10, "created_date", "desc", customerData.get().getProjectId());

        assertions.assertEquals(response.getStatusCode(), 401, "missing bearer token should return 401");
    }

    @Test(groups = {"regression"})
    public void getTestSuitesInvalidStatusReturns422() {
        Response response = home.getTestSuites(
                "INVALID", 1, 10, "created_date", "desc", customerData.get().getProjectId());

        assertions.assertEquals(response.getStatusCode(), 422, "invalid status enum should return 422");
    }
}
