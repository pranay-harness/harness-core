package io.harness.steps.cf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.data.annotation.TypeAlias;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", visible = true, defaultImpl = SetFeatureFlagStateYaml.class)
@JsonSubTypes({
  @JsonSubTypes.Type(value = SetFeatureFlagStateYaml.class, name = "SetFeatureFlagState")
  , @JsonSubTypes.Type(value = AddTargetsToVariationTargetMapYaml.class, name = "AddTargetsToVariationTargetMap"),
      @JsonSubTypes.Type(
          value = RemoveTargetsToVariationTargetMapYaml.class, name = "RemoveTargetsToVariationTargetMap"),
      @JsonSubTypes.Type(value = AddSegmentToVariationTargetMapYaml.class, name = "AddSegmentToVariationTargetMap"),
      @JsonSubTypes.Type(
          value = RemoveSegmentToVariationTargetMapYaml.class, name = "RemoveSegmentToVariationTargetMap"),
})
public interface PatchInstruction {
  @TypeAlias("instruction_kind")
  enum Type {
    @JsonProperty("SetFeatureFlagState") SET_FEATURE_FLAG_STATE("SetFeatureFlagState"),
    @JsonProperty("AddTargetsToVariationTargetMap")
    ADD_TARGETS_TO_VARIATION_TARGET_MAP("AddTargetsToVariationTargetMap"),
    @JsonProperty("RemoveTargetsToVariationTargetMap")
    REMOVE_TARGETS_TO_VARIATION_MAP("RemoveTargetsToVariationTargetMap"),
    @JsonProperty("AddSegmentsToVariationTargetMap") ADD_SEGMENT_TO_VARIATION_MAP("AddSegmentToVariationTargetMap"),
    @JsonProperty("RemoveSegmentsToVariationTargetMap")
    REMOVE_SEGMENT_TO_VARIATION_TARGET_MAP("RemoveSegmentToVariationTargetMap");
    private final String yamlName;

    Type(String yamlName) {
      this.yamlName = yamlName;
    }

    @JsonValue
    public String getYamlName() {
      return yamlName;
    }
  }
  Type getType();
}
