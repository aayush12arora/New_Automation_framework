package api.defect;

import framework.api.models.request.AddDefectsRequest;
import framework.api.models.request.NewDefectRequest;
import framework.api.models.request.UpdateDefectRequest;
import framework.api.models.response.AddDefectsResponse;
import framework.api.models.response.GetDefectsResponse;
import framework.api.models.response.NewDefectResponse;
import framework.api.models.response.SimilarDefectsResponse;
import framework.api.models.response.SuccessMessageResponse;
import framework.api.models.response.TemplateResponse;
import framework.api.services.DefectService;
import framework.base.BaseApiTest;
import framework.data.customer.CustomerData;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Tests for the Defect Management API group. The auth token comes from
 * {@code customerData.get().getToken()}; every other input from the same object, loaded per
 * test from {@code testdata/<testName>.json}.
 */
public class DefectManagementTest extends BaseApiTest {

    @Test(groups = {"regression"})
    public void addDefects() {
        CustomerData data = customerData.get();
        AddDefectsRequest requestBody = new AddDefectsRequest(
                data.getTestCaseIds(), "5c7efd94-517b-4bad-bd2d-4a117a22fe9d", data.getTestSuiteId());

        Response response = new DefectService().addDefects(data.getToken(), requestBody);
        AddDefectsResponse body = response.as(AddDefectsResponse.class);

        assertions.softAssertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.softAssertEquals(body.getSuccess(), Boolean.TRUE, "success should be true");
    }

    @Test(groups = {"smoke", "regression"})
    public void getDefectsReturnsList() {
        CustomerData data = customerData.get();
        Response response = new DefectService().getDefects(
                data.getToken(), data.getTestSuiteId(), data.getProjectId(), data.getPage(), data.getPageSize());
        GetDefectsResponse body = response.as(GetDefectsResponse.class);

        assertions.softAssertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.softAssertNotNull(body.getDefects(), "response should contain defects");
    }

    @Test(groups = {"regression"})
    public void getSimilarDefects() {
        CustomerData data = customerData.get();
        Response response = new DefectService().getSimilarDefects(
                data.getToken(), data.getTestSuiteId(), data.getTestCaseIds());
        SimilarDefectsResponse body = response.as(SimilarDefectsResponse.class);

        assertions.softAssertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.softAssertNotNull(body.getSimilarDefects(), "response should contain similar_defects");
    }

    @Test(groups = {"regression"})
    public void getTemplate() {
        CustomerData data = customerData.get();
        Response response = new DefectService().getTemplate(
                data.getToken(), data.getTestSuiteId(), data.getTestCaseId(), data.getSessionId(), data.getProjectName());
        TemplateResponse body = response.as(TemplateResponse.class);

        assertions.softAssertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.softAssertNotNull(body.getTemplate(), "response should contain template");
    }

    @Test(groups = {"regression"})
    public void createNewDefect() {
        CustomerData data = customerData.get();
        NewDefectRequest requestBody = new NewDefectRequest.Builder()
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

        Response response = new DefectService().addNewDefect(data.getToken(), data.getProjectId(),
                data.getTestSuiteId(), requestBody);
        NewDefectResponse body = response.as(NewDefectResponse.class);

        assertions.softAssertEquals(response.getStatusCode(), 201, "creating a defect should return 201");
        assertions.softAssertNotNull(body.getDefectId(), "response should contain defect_id");
    }

    @Test(groups = {"regression"})
    public void updateDefect() {
        CustomerData data = customerData.get();
        UpdateDefectRequest requestBody = new UpdateDefectRequest(
                List.of(new UpdateDefectRequest.DefectData("QA-41", "https://eh-de.atlassian.net/browse/QA-41")),
                data.getTestCaseId(), data.getTestSuiteId());

        Response response = new DefectService().updateDefect(data.getToken(), requestBody);
        SuccessMessageResponse body = response.as(SuccessMessageResponse.class);

        assertions.softAssertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.softAssertEquals(body.getSuccess(), Boolean.TRUE, "success should be true");
    }
}
