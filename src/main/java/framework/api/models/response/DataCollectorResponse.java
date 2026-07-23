package framework.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Typed response for {@code POST /data-collector/data-collector/execute}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataCollectorResponse {

    private String status;
    private String message;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
