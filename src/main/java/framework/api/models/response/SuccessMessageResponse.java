package framework.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Typed response for the several endpoints whose response is exactly
 * {@code { success: boolean, message: string } } — {@code add-testcases},
 * {@code delete-testcase}, and {@code update_defect} all share this shape, so one model
 * covers all three (matching the actual JSON schema rather than duplicating an identical class).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SuccessMessageResponse {

    private Boolean success;
    private String message;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
