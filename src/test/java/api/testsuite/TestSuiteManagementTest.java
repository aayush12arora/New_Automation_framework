package api.testsuite;

import framework.api.models.request.AddTestcasesRequest;
import framework.api.models.request.CreateSuiteRequest;
import framework.api.models.response.CreateSuiteResponse;
import framework.api.models.response.ProjectInsightsResponse;
import framework.api.models.response.SuccessMessageResponse;
import framework.api.models.response.TcmToolResponse;
import framework.api.services.TestSuiteService;
import framework.base.BaseApiTest;
import framework.data.customer.CustomerData;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Tests for the Test Suite Management API group. The auth token comes from
 * {@code customerData.get().getToken()}; every other input from the same object, loaded per
 * test from {@code testdata/<testName>.json}.
 */
public class TestSuiteManagementTest extends BaseApiTest {

    @Test(groups = {"smoke", "regression"})
    public void getProjectInsightsReturnsInsights() {
        CustomerData data = customerData.get();
        Response response = new TestSuiteService().getProjectInsights(data.getToken(), data.getProjectId());
        ProjectInsightsResponse body = response.as(ProjectInsightsResponse.class);

        assertions.softAssertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.softAssertNotNull(body.getInsights(), "response should contain insights");
    }

    @Test(groups = {"regression"})
    public void getTcmToolReturnsOk() {
        CustomerData data = customerData.get();
        Response response = new TestSuiteService().getTcmTool(data.getToken(), data.getProjectId());
        TcmToolResponse body = response.as(TcmToolResponse.class);

        assertions.softAssertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.softAssertNotNull(body, "response body should deserialize");
    }

    @Test(groups = {"regression"})
    public void addTestcasesToSuite() {
        CustomerData data = customerData.get();
        AddTestcasesRequest body = new AddTestcasesRequest(
                data.getProjectId(), data.getTestSuiteId(), List.of(data.getTestCaseId()));

        Response response = new TestSuiteService().addTestcases(data.getToken(), body);
        SuccessMessageResponse result = response.as(SuccessMessageResponse.class);

        assertions.softAssertEquals(response.getStatusCode(), 200, "adding test cases should return 200");
        assertions.softAssertEquals(result.getSuccess(), Boolean.TRUE, "success should be true");
    }

    @Test(groups = {"regression"})
    public void deleteTestcaseFromSuite() {
        CustomerData data = customerData.get();
        Response response = new TestSuiteService().deleteTestcase(
                data.getToken(), data.getProjectId(), data.getTestSuiteId(), data.getTestCaseId());
        SuccessMessageResponse result = response.as(SuccessMessageResponse.class);

        assertions.softAssertEquals(response.getStatusCode(), 200, "deleting a test case should return 200");
        assertions.softAssertEquals(result.getSuccess(), Boolean.TRUE, "success should be true");
    }

    @Test(groups = {"smoke", "regression"})
    public void createTestSuite() {
        CustomerData data = customerData.get();
        CreateSuiteRequest body = new CreateSuiteRequest.Builder()
                .testSuiteName("TestSuite_Sample123")
                .createdBy("Automation User")
                .updatedBy("Automation User")
                .projectId(String.valueOf(data.getProjectId()))
                .description("created by API automation")
                .tool("PLAYWRIGHT")
                .browser("CHROME")
                .language("PYTHON")
                .build();

        Response response = new TestSuiteService().createSuite(data.getToken(), body);
        CreateSuiteResponse result = response.as(CreateSuiteResponse.class);

        assertions.softAssertEquals(response.getStatusCode(), 201, "creating a suite should return 201");
        assertions.softAssertNotNull(result.getData(), "response should contain data");
        assertions.softAssertNotNull(result.getData().getTestSuiteId(), "data should contain test_suite_id");
    }
}
