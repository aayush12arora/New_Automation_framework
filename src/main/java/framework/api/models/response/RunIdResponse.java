package framework.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Typed response for {@code GET /qe/rerun_id}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RunIdResponse {

    @JsonProperty("run_id")
    private String runId;

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }
}
