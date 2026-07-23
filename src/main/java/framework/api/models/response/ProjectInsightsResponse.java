package framework.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Typed response for {@code GET /qe/test_suite/project_insights}. The spec declares
 * {@code insights} as a free-form object with no fixed schema, so it maps to a {@code Map}
 * rather than a further nested POJO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectInsightsResponse {

    private Map<String, Object> insights;

    public Map<String, Object> getInsights() {
        return insights;
    }

    public void setInsights(Map<String, Object> insights) {
        this.insights = insights;
    }
}
