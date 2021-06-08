package io.harness.steps.cf;

import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("AddSegmentToVariationTargetMap")
@TypeAlias("AddSegmentToVariationTargetMapYaml")
public class AddSegmentToVariationTargetMapYaml implements PatchInstruction {
  @Builder.Default @NotNull private PatchInstruction.Type type = Type.ADD_TARGETS_TO_VARIATION_TARGET_MAP;
  @NotNull private AddSegmentToVariationTargetMapYamlSpec spec;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AddSegmentToVariationTargetMapYamlSpec {
    private ParameterField<String> variation;
    private ParameterField<List<String>> segments;
    private ParameterField<String> identifier;
    private ParameterField<String> name;
  }
}
