package framework.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Typed response for {@code POST /qe-defect/defect-management/add_defects}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddDefectsResponse {

    private Boolean success;

    @JsonProperty("defect_ids")
    private List<String> defectIds;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public List<String> getDefectIds() {
        return defectIds;
    }

    public void setDefectIds(List<String> defectIds) {
        this.defectIds = defectIds;
    }
}
