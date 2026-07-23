package api.execution;

import framework.api.models.request.DataCollectorRequest;
import framework.api.models.request.ReportGenerateRequest;
import framework.api.models.request.RerunRequest;
import framework.api.services.TestExecutionService;
import framework.base.BaseApiTest;
import framework.data.customer.CustomerData;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;

/** Tests for the Test Execution API group. */
public class TestExecutionTest extends BaseApiTest {

    @Test(groups = {"smoke", "regression"})
    public void getRunIdReturnsRunId() {
        CustomerData data = customerData.get();
        Response response = new TestExecutionService().getRunId(data.getProjectId(), data.getTestSuiteId());

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.assertNotNull(response.jsonPath().get("run_id"), "response should contain run_id");
    }

    @Test(groups = {"regression"})
    public void getRunHistoryReturnsHistory() {
        CustomerData data = customerData.get();
        Response response = new TestExecutionService().getRunHistory(
                data.getUserId(), data.getProjectId(), data.getTestSuiteId(), data.getPage(), data.getPageSize());

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.assertNotNull(response.jsonPath().get("history"), "response should contain history");
    }

    @Test(groups = {"regression"})
    public void getSuiteDetailsReturnsDetails() {
        CustomerData data = customerData.get();
        Response response = new TestExecutionService().getSuiteDetails(
                data.getProjectId(), data.getUserId(), data.getTestSuiteId(),
                data.getPageSize(), data.getPage(), data.getRunId());

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
    }

    @Test(groups = {"regression"})
    public void generateReport() {
        CustomerData data = customerData.get();
        ReportGenerateRequest body = new ReportGenerateRequest(
                data.getProjectId(), data.getTestSuiteId(), data.getRunId());

        Response response = new TestExecutionService().generateReport(body);

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
    }

    @Test(groups = {"regression"})
    public void getGithubFiles() {
        CustomerData data = customerData.get();
        Response response = new TestExecutionService().getGithubFiles(
                data.getProjectId(), data.getTestSuiteId(), data.getTestCaseId());

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
    }

    @Test(groups = {"regression"})
    public void rerunTestCase() {
        CustomerData data = customerData.get();
        RerunRequest body = new RerunRequest(
                data.getProjectId(), data.getTestSuiteId(), List.of(data.getTestCaseId()), data.getRunId());

        Response response = new TestExecutionService().rerun(body);

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
    }

    @Test(groups = {"regression"})
    public void dataCollectorExecute() {
        CustomerData data = customerData.get();
        DataCollectorRequest body = new DataCollectorRequest(data.getTestCaseId());

        Response response = new TestExecutionService().dataCollectorExecute(
                data.getProjectId(), data.getTestSuiteId(), data.getRunId(), body);

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
    }

    @Test(groups = {"regression"})
    public void getTestcaseDetails() {
        CustomerData data = customerData.get();
        Response response = new TestExecutionService().getTestcaseDetails(
                data.getProjectId(), data.getTestCaseId(), data.getTestSuiteId());

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.assertNotNull(response.jsonPath().get("test_case"), "response should contain test_case");
    }
}
