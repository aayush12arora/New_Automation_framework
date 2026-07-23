package framework.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Typed response for {@code GET /qe/test-suite/details}. The spec declares {@code details} as
 * a free-form object with no fixed schema, so it maps to a {@code Map}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SuiteDetailsResponse {

    private Map<String, Object> details;

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}
