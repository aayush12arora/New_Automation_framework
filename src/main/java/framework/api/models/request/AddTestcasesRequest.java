package framework.api.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Request body for {@code POST /qe/test-suite/add-testcases}. */
public class AddTestcasesRequest {

    @JsonProperty("project_id")
    private final Integer projectId;

    @JsonProperty("test_suite_id")
    private final Integer testSuiteId;

    @JsonProperty("test_case_ids")
    private final List<String> testCaseIds;

    public AddTestcasesRequest(Integer projectId, Integer testSuiteId, List<String> testCaseIds) {
        this.projectId = projectId;
        this.testSuiteId = testSuiteId;
        this.testCaseIds = testCaseIds;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public Integer getTestSuiteId() {
        return testSuiteId;
    }

    public List<String> getTestCaseIds() {
        return testCaseIds;
    }
}
