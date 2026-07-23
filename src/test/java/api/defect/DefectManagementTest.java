package api.defect;

import framework.api.models.request.AddDefectsRequest;
import framework.api.models.request.NewDefectRequest;
import framework.api.models.request.UpdateDefectRequest;
import framework.api.services.DefectService;
import framework.base.BaseApiTest;
import framework.data.customer.CustomerData;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;

/** Tests for the Defect Management API group. */
public class DefectManagementTest extends BaseApiTest {

    @Test(groups = {"regression"})
    public void addDefects() {
        CustomerData data = customerData.get();
        AddDefectsRequest body = new AddDefectsRequest(
                data.getTestCaseIds(), "5c7efd94-517b-4bad-bd2d-4a117a22fe9d", data.getTestSuiteId());

        Response response = new DefectService().addDefects(body);

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
    }

    @Test(groups = {"smoke", "regression"})
    public void getDefectsReturnsList() {
        CustomerData data = customerData.get();
        Response response = new DefectService().getDefects(
                data.getTestSuiteId(), data.getProjectId(), data.getPage(), data.getPageSize());

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.assertNotNull(response.jsonPath().get("defects"), "response should contain defects");
    }

    @Test(groups = {"regression"})
    public void getSimilarDefects() {
        CustomerData data = customerData.get();
        Response response = new DefectService().getSimilarDefects(data.getTestSuiteId(), data.getTestCaseIds());

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.assertNotNull(response.jsonPath().get("similar_defects"), "response should contain similar_defects");
    }

    @Test(groups = {"regression"})
    public void getTemplate() {
        CustomerData data = customerData.get();
        Response response = new DefectService().getTemplate(
                data.getTestSuiteId(), data.getTestCaseId(), data.getSessionId(), data.getProjectName());

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.assertNotNull(response.jsonPath().get("template"), "response should contain template");
    }

    @Test(groups = {"regression"})
    public void createNewDefect() {
        CustomerData data = customerData.get();
        NewDefectRequest body = new NewDefectRequest.Builder()
                .issueType("Bug")
                .title("API automation - execution data missing")
                .description("Created by API automation")
                .severity("Low")
                .priority("P3")
                .accountId("712020:0df3e2dc-c9e1-4fa7-b5b7-d93e4cbe9390")
                .projectKey("QA")
                .testCaseId(data.getTestCaseId())
                .labels(List.of("UI", "Automated_Test"))
                .build();

        Response response = new DefectService().addNewDefect(data.getProjectId(), data.getTestSuiteId(), body);

        assertions.assertEquals(response.getStatusCode(), 201, "creating a defect should return 201");
    }

    @Test(groups = {"regression"})
    public void updateDefect() {
        CustomerData data = customerData.get();
        UpdateDefectRequest body = new UpdateDefectRequest(
                List.of(new UpdateDefectRequest.DefectData("QA-41", "https://eh-de.atlassian.net/browse/QA-41")),
                data.getTestCaseId(), data.getTestSuiteId());

        Response response = new DefectService().updateDefect(body);

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
    }
}
