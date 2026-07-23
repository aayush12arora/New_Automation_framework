package framework.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Typed response for {@code GET /qe-defect/defect-workflow/get_similar_defects}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimilarDefectsResponse {

    @JsonProperty("similar_defects")
    private List<SimilarDefect> similarDefects;

    public List<SimilarDefect> getSimilarDefects() {
        return similarDefects;
    }

    public void setSimilarDefects(List<SimilarDefect> similarDefects) {
        this.similarDefects = similarDefects;
    }

    /** One entry in the {@code similar_defects} array. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SimilarDefect {

        @JsonProperty("defect_id")
        private String defectId;

        @JsonProperty("similarity_score")
        private Double similarityScore;

        public String getDefectId() {
            return defectId;
        }

        public void setDefectId(String defectId) {
            this.defectId = defectId;
        }

        public Double getSimilarityScore() {
            return similarityScore;
        }

        public void setSimilarityScore(Double similarityScore) {
            this.similarityScore = similarityScore;
        }
    }
}
