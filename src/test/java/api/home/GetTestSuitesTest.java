package api.home;

import framework.api.models.response.TestSuitesResponse;
import framework.api.services.HomePageService;
import framework.base.BaseApiTest;
import io.restassured.response.Response;
import org.testng.annotations.Test;

/**
 * Tests for {@code GET /qe/project/test_suites}, in the reference-framework style:
 * instantiate the service, call the endpoint method, deserialize the response into a typed
 * model with {@code response.as(...)}, and assert on getters.
 *
 * <p>Framework upgrades over the reference: inputs come from {@code customerData.get()} (loaded
 * from {@code testdata/<testName>.json}) instead of being hard-coded, and verification goes
 * through the shared {@code assertions} helper. Covers positive, negative (auth) and
 * edge/validation scenarios.</p>
 */
public class GetTestSuitesTest extends BaseApiTest {

    @Test(groups = {"smoke", "regression"})
    public void getTestSuitesReturnsList() {
        HomePageService service = new HomePageService();

        Response response = service.getTestSuites(
                "ALL", 1, 10, "created_date", "desc", customerData.get().getProjectId());
        TestSuitesResponse body = response.as(TestSuitesResponse.class);

        assertions.assertEquals(response.getStatusCode(), 200, "valid request should return 200");
        assertions.assertNotNull(body.getTestSuites(), "response should contain test_suites");
        assertions.assertEquals(body.getPage(), 1, "page should be echoed back");
        assertions.assertEquals(body.getPageSize(), 10, "page_size should be echoed back");
    }

    @Test(groups = {"regression"})
    public void getTestSuitesWithoutTokenReturns401() {
        HomePageService service = new HomePageService();

        Response response = service.getTestSuitesWithoutAuth(
                "ALL", 1, 10, "created_date", "desc", customerData.get().getProjectId());

        assertions.assertEquals(response.getStatusCode(), 401, "missing bearer token should return 401");
    }

    @Test(groups = {"regression"})
    public void getTestSuitesInvalidStatusReturns422() {
        HomePageService service = new HomePageService();

        Response response = service.getTestSuites(
                "INVALID", 1, 10, "created_date", "desc", customerData.get().getProjectId());

        assertions.assertEquals(response.getStatusCode(), 422, "invalid status enum should return 422");
    }
}
