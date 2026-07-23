package framework.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Typed response for {@code POST /qe/test_suite/rerun}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RerunResponse {

    private String status;

    @JsonProperty("new_run_id")
    private String newRunId;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNewRunId() {
        return newRunId;
    }

    public void setNewRunId(String newRunId) {
        this.newRunId = newRunId;
    }
}
