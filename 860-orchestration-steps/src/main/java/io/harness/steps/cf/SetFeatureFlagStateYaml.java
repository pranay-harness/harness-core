package io.harness.steps.cf;

import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
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
@JsonTypeName("SetFeatureFlagState")
@TypeAlias("SetFeatureFlagStateYaml")
public class SetFeatureFlagStateYaml implements PatchInstruction {
  @Builder.Default @NotNull private PatchInstruction.Type type = Type.SET_FEATURE_FLAG_STATE;
  @NotNull private SetFeatureFlagStateYamlSpec spec;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SetFeatureFlagStateYamlSpec {
    private ParameterField<String> state;
    private ParameterField<String> identifier;
    private ParameterField<String> name;
  }
}
