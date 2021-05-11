package io.harness.steps.cf;

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
@JsonTypeName("AddTargetsToVariationTargetMap")
@TypeAlias("AddTargetsToVariationTargetMapYaml")
public class AddTargetsToVariationTargetMapYaml implements PatchInstruction {
  @Builder.Default @NotNull private PatchInstruction.Type type = Type.ADD_TARGETS_TO_VARIATION_TARGET_MAP;
  @NotNull private AddTargetsToVariationTargetMapYamlSpec spec;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AddTargetsToVariationTargetMapYamlSpec {
    private String variation;
    private List<String> targets;
  }
}
