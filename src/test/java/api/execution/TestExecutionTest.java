package api.execution;

import framework.api.models.request.DataCollectorRequest;
import framework.api.models.request.ReportGenerateRequest;
import framework.api.models.request.RerunRequest;
import framework.api.models.response.DataCollectorResponse;
import framework.api.models.response.GithubFilesResponse;
import framework.api.models.response.ReportGenerateResponse;
import framework.api.models.response.RerunResponse;
import framework.api.models.response.RunHistoryResponse;
import framework.api.models.response.RunIdResponse;
import framework.api.models.response.SuiteDetailsResponse;
import framework.api.models.response.TestcaseDetailsResponse;
import framework.api.services.TestExecutionService;
import framework.base.BaseApiTest;
import framework.data.customer.CustomerData;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Tests for the Test Execution API group. The auth token comes from
 * {@code customerData.get().getToken()}; every other input from the same object, loaded per
 * test from {@code testdata/<testName>.json}.
 */
public class TestExecutionTest extends BaseApiTest {

    @Test(groups = {"smoke", "regression"})
    public void getRunIdReturnsRunId() {
        CustomerData data = customerData.get();
        Response response = new TestExecutionService().getRunId(
                data.getToken(), data.getProjectId(), data.getTestSuiteId());
        RunIdResponse body = response.as(RunIdResponse.class);

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.assertNotNull(body.getRunId(), "response should contain run_id");
    }

    @Test(groups = {"regression"})
    public void getRunHistoryReturnsHistory() {
        CustomerData data = customerData.get();
        Response response = new TestExecutionService().getRunHistory(data.getToken(),
                data.getUserId(), data.getProjectId(), data.getTestSuiteId(), data.getPage(), data.getPageSize());
        RunHistoryResponse body = response.as(RunHistoryResponse.class);

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.assertNotNull(body.getHistory(), "response should contain history");
    }

    @Test(groups = {"regression"})
    public void getSuiteDetailsReturnsDetails() {
        CustomerData data = customerData.get();
        Response response = new TestExecutionService().getSuiteDetails(data.getToken(),
                data.getProjectId(), data.getUserId(), data.getTestSuiteId(),
                data.getPageSize(), data.getPage(), data.getRunId());
        SuiteDetailsResponse body = response.as(SuiteDetailsResponse.class);

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.assertNotNull(body.getDetails(), "response should contain details");
    }

    @Test(groups = {"regression"})
    public void generateReport() {
        CustomerData data = customerData.get();
        ReportGenerateRequest requestBody = new ReportGenerateRequest(
                data.getProjectId(), data.getTestSuiteId(), data.getRunId());

        Response response = new TestExecutionService().generateReport(data.getToken(), requestBody);
        ReportGenerateResponse body = response.as(ReportGenerateResponse.class);

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.assertNotNull(body.getStatus(), "response should contain status");
    }

    @Test(groups = {"regression"})
    public void getGithubFiles() {
        CustomerData data = customerData.get();
        Response response = new TestExecutionService().getGithubFiles(
                data.getToken(), data.getProjectId(), data.getTestSuiteId(), data.getTestCaseId());
        GithubFilesResponse body = response.as(GithubFilesResponse.class);

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.assertNotNull(body.getFiles(), "response should contain files");
    }

    @Test(groups = {"regression"})
    public void rerunTestCase() {
        CustomerData data = customerData.get();
        RerunRequest requestBody = new RerunRequest(
                data.getProjectId(), data.getTestSuiteId(), List.of(data.getTestCaseId()), data.getRunId());

        Response response = new TestExecutionService().rerun(data.getToken(), requestBody);
        RerunResponse body = response.as(RerunResponse.class);

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.assertNotNull(body.getStatus(), "response should contain status");
    }

    @Test(groups = {"regression"})
    public void dataCollectorExecute() {
        CustomerData data = customerData.get();
        DataCollectorRequest requestBody = new DataCollectorRequest(data.getTestCaseId());

        Response response = new TestExecutionService().dataCollectorExecute(
                data.getToken(), data.getProjectId(), data.getTestSuiteId(), data.getRunId(), requestBody);
        DataCollectorResponse body = response.as(DataCollectorResponse.class);

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.assertNotNull(body.getStatus(), "response should contain status");
    }

    @Test(groups = {"regression"})
    public void getTestcaseDetails() {
        CustomerData data = customerData.get();
        Response response = new TestExecutionService().getTestcaseDetails(
                data.getToken(), data.getProjectId(), data.getTestCaseId(), data.getTestSuiteId());
        TestcaseDetailsResponse body = response.as(TestcaseDetailsResponse.class);

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.assertNotNull(body.getTestCase(), "response should contain test_case");
    }
}
