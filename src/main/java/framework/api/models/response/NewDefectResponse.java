package framework.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Typed response for {@code POST /qe-defect/defect-management/add_new_defect} (201). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewDefectResponse {

    @JsonProperty("defect_id")
    private String defectId;

    private String message;

    public String getDefectId() {
        return defectId;
    }

    public void setDefectId(String defectId) {
        this.defectId = defectId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
