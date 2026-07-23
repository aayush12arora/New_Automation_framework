package api.testsuite;

import framework.api.models.request.AddTestcasesRequest;
import framework.api.models.request.CreateSuiteRequest;
import framework.api.services.TestSuiteService;
import framework.base.BaseApiTest;
import framework.data.customer.CustomerData;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Tests for the Test Suite Management API group. Shared IDs come from {@code customerData};
 * request bodies are built with typed models (and the {@code CreateSuiteRequest.Builder}).
 */
public class TestSuiteManagementTest extends BaseApiTest {

    @Test(groups = {"smoke", "regression"})
    public void getProjectInsightsReturnsInsights() {
        Response response = new TestSuiteService().getProjectInsights(customerData.get().getProjectId());

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.assertNotNull(response.jsonPath().get("insights"), "response should contain insights");
    }

    @Test(groups = {"regression"})
    public void getTcmToolReturnsOk() {
        Response response = new TestSuiteService().getTcmTool(customerData.get().getProjectId());

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
    }

    @Test(groups = {"regression"})
    public void addTestcasesToSuite() {
        CustomerData data = customerData.get();
        AddTestcasesRequest body = new AddTestcasesRequest(
                data.getProjectId(), data.getTestSuiteId(), List.of(data.getTestCaseId()));

        Response response = new TestSuiteService().addTestcases(body);

        assertions.assertEquals(response.getStatusCode(), 200, "adding test cases should return 200");
    }

    @Test(groups = {"regression"})
    public void deleteTestcaseFromSuite() {
        CustomerData data = customerData.get();
        Response response = new TestSuiteService().deleteTestcase(
                data.getProjectId(), data.getTestSuiteId(), data.getTestCaseId());

        assertions.assertEquals(response.getStatusCode(), 200, "deleting a test case should return 200");
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

        Response response = new TestSuiteService().createSuite(body);

        assertions.assertEquals(response.getStatusCode(), 201, "creating a suite should return 201");
        assertions.assertNotNull(response.jsonPath().get("data"), "response should contain data");
    }
}
