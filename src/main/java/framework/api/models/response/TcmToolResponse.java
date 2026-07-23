package framework.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/** Typed response for {@code GET /ftg/v1/functional-testcase/fetch-tcm-tool}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TcmToolResponse {

    private String tool;
    private Map<String, Object> configuration;

    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }
}
