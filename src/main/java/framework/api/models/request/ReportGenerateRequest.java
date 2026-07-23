package framework.api.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Request body for {@code POST /qe/report/generate}. */
public class ReportGenerateRequest {

    @JsonProperty("project_id")
    private final Integer projectId;

    @JsonProperty("suite_id")
    private final Integer suiteId;

    @JsonProperty("run_id")
    private final String runId;

    public ReportGenerateRequest(Integer projectId, Integer suiteId, String runId) {
        this.projectId = projectId;
        this.suiteId = suiteId;
        this.runId = runId;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public Integer getSuiteId() {
        return suiteId;
    }

    public String getRunId() {
        return runId;
    }
}
