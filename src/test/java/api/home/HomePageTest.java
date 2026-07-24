package api.home;

import framework.api.models.response.ProjectsResponse;
import framework.api.models.response.TestSuitesResponse;
import framework.api.models.response.TestSuitesV2Response;
import framework.api.services.HomePageService;
import framework.base.BaseApiTest;
import framework.data.customer.CustomerData;
import io.restassured.response.Response;
import org.testng.annotations.Test;

/**
 * Tests for the Home Page API group. The auth token is read from
 * {@code customerData.get().getToken()} — set once per test by {@code BaseApiTest}, not fetched
 * by the service itself. Every other input also comes from {@code customerData.get()}, loaded
 * from {@code testdata/<testName>.json} (one file per test, same convention as the UI side).
 * Responses are deserialized into typed models and asserted via the shared {@code assertions}.
 */
public class HomePageTest extends BaseApiTest {

    @Test(groups = {"smoke", "regression"})
    public void getProjectsReturnsProjects() {
        CustomerData data = customerData.get();
        Response response = new HomePageService().getProjects(
                data.getToken(), data.getUserId(), data.getPersonaId(), data.getModuleId());
        ProjectsResponse body = response.as(ProjectsResponse.class);

        assertions.softAssertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.softAssertNotNull(body.getProjects(), "response should contain projects");
    }

    @Test(groups = {"smoke", "regression"})
    public void getTestSuitesReturnsList() {
        CustomerData data = customerData.get();
        Response response = new HomePageService().getTestSuites(
                data.getToken(), "ALL", 1, 10, "created_date", "desc", data.getProjectId());
        TestSuitesResponse body = response.as(TestSuitesResponse.class);

        assertions.softAssertEquals(response.getStatusCode(), 200, "valid request should return 200");
        assertions.softAssertNotNull(body.getTestSuites(), "response should contain test_suites");
        assertions.softAssertEquals(body.getPage(), 1, "page should be echoed back");
        assertions.softAssertEquals(body.getPageSize(), 10, "page_size should be echoed back");
    }

    @Test(groups = {"regression"})
    public void getTestSuitesWithoutTokenReturns401() {
        CustomerData data = customerData.get();
        Response response = new HomePageService().getTestSuitesWithoutAuth(
                "ALL", 1, 10, "created_date", "desc", data.getProjectId());

        assertions.softAssertEquals(response.getStatusCode(), 401, "missing auth cookie should return 401");
    }

    @Test(groups = {"regression"})
    public void getTestSuitesInvalidStatusReturns422() {
        CustomerData data = customerData.get();
        Response response = new HomePageService().getTestSuites(
                data.getToken(), "INVALID", 1, 10, "created_date", "desc", data.getProjectId());

        assertions.softAssertEquals(response.getStatusCode(), 422, "invalid status enum should return 422");
    }

    @Test(groups = {"regression"})
    public void getTestSuitesV2ReturnsOk() {
        Response response = new HomePageService().getTestSuitesV2(customerData.get().getToken());
        TestSuitesV2Response body = response.as(TestSuitesV2Response.class);

        assertions.softAssertEquals(response.getStatusCode(), 200, "v2 should return 200");
        assertions.softAssertNotNull(body, "response body should deserialize");
    }
}
