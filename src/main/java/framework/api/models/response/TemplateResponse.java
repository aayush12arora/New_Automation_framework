package framework.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Typed response for {@code GET /qe-defect/defect-workflow/get_template}. The spec declares
 * {@code template} as a free-form object with no fixed schema, so it maps to a {@code Map}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateResponse {

    private Map<String, Object> template;

    public Map<String, Object> getTemplate() {
        return template;
    }

    public void setTemplate(Map<String, Object> template) {
        this.template = template;
    }
}
