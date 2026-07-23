package framework.api.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Request body for {@code POST /qe/test_suite/rerun}. */
public class RerunRequest {

    @JsonProperty("project_id")
    private final Integer projectId;

    @JsonProperty("test_suite_id")
    private final Integer testSuiteId;

    @JsonProperty("test_case_ids")
    private final List<String> testCaseIds;

    @JsonProperty("run_id")
    private final String runId;

    public RerunRequest(Integer projectId, Integer testSuiteId, List<String> testCaseIds, String runId) {
        this.projectId = projectId;
        this.testSuiteId = testSuiteId;
        this.testCaseIds = testCaseIds;
        this.runId = runId;
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

    public String getRunId() {
        return runId;
    }
}
