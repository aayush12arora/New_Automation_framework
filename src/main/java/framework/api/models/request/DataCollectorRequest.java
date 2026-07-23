package framework.api.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Request body for {@code POST /data-collector/data-collector/execute}. */
public class DataCollectorRequest {

    @JsonProperty("user_request")
    private final String userRequest;

    public DataCollectorRequest(String userRequest) {
        this.userRequest = userRequest;
    }

    public String getUserRequest() {
        return userRequest;
    }
}
