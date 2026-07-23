package api.home;

import framework.api.models.response.TestSuitesResponse;
import framework.api.services.HomePageService;
import framework.base.BaseApiTest;
import framework.data.customer.CustomerData;
import io.restassured.response.Response;
import org.testng.annotations.Test;

/**
 * Tests for the Home Page API group. Reference style: instantiate the service, call the
 * endpoint, deserialize with {@code response.as(...)} where the contract is typed, and assert
 * via the shared {@code assertions}. Inputs come from {@code customerData.get()} (loaded from
 * {@code testdata/apiData.json}). Covers positive, negative (auth) and edge/validation.
 */
public class HomePageTest extends BaseApiTest {

    @Test(groups = {"smoke", "regression"})
    public void getProjectsReturnsProjects() {
        CustomerData data = customerData.get();
        Response response = new HomePageService().getProjects(
                data.getUserId(), data.getPersonaId(), data.getModuleId());

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.assertNotNull(response.jsonPath().get("projects"), "response should contain projects");
    }

    @Test(groups = {"smoke", "regression"})
    public void getTestSuitesReturnsList() {
        Response response = new HomePageService().getTestSuites(
                "ALL", 1, 10, "created_date", "desc", customerData.get().getProjectId());
        TestSuitesResponse body = response.as(TestSuitesResponse.class);

        assertions.assertEquals(response.getStatusCode(), 200, "valid request should return 200");
        assertions.assertNotNull(body.getTestSuites(), "response should contain test_suites");
        assertions.assertEquals(body.getPage(), 1, "page should be echoed back");
        assertions.assertEquals(body.getPageSize(), 10, "page_size should be echoed back");
    }

    @Test(groups = {"regression"})
    public void getTestSuitesWithoutTokenReturns401() {
        Response response = new HomePageService().getTestSuitesWithoutAuth(
                "ALL", 1, 10, "created_date", "desc", customerData.get().getProjectId());

        assertions.assertEquals(response.getStatusCode(), 401, "missing auth cookie should return 401");
    }

    @Test(groups = {"regression"})
    public void getTestSuitesInvalidStatusReturns422() {
        Response response = new HomePageService().getTestSuites(
                "INVALID", 1, 10, "created_date", "desc", customerData.get().getProjectId());

        assertions.assertEquals(response.getStatusCode(), 422, "invalid status enum should return 422");
    }

    @Test(groups = {"regression"})
    public void getTestSuitesV2ReturnsOk() {
        Response response = new HomePageService().getTestSuitesV2();

        assertions.assertEquals(response.getStatusCode(), 200, "v2 should return 200");
    }
}
