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
@JsonTypeName("RemoveTargetsToVariationTargetMap")
@TypeAlias("RemoveTargetsToVariationTargetMapYaml")
public class RemoveTargetsToVariationTargetMapYaml implements PatchInstruction {
  @Builder.Default @NotNull private PatchInstruction.Type type = Type.REMOVE_TARGETS_TO_VARIATION_MAP;
  @NotNull private RemoveTargetsToVariationTargetMapYamlSpec spec;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RemoveTargetsToVariationTargetMapYamlSpec {
    private ParameterField<String> variation;
    private ParameterField<List<String>> targets;
    private ParameterField<String> identifier;
    private ParameterField<String> name;
  }
}
