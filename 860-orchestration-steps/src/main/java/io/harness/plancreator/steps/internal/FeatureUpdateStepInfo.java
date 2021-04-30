package io.harness.plancreator.steps.internal;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.sync.SyncFacilitator;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.cf.FeatureUpdateStep;
import io.harness.steps.cf.FeatureUpdateStepParameters;
import io.harness.steps.cf.PatchInstruction;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.beans.ConstructorProperties;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CF)
@Data
@NoArgsConstructor
@EqualsAndHashCode
@JsonTypeName("FeatureUpdate")
@TypeAlias("featureUpdateStepInfo")
public class FeatureUpdateStepInfo implements PMSStepInfo {
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String name;
  @JsonProperty("featureUpdateRef") @NotNull String identifier;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> feature;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> environment;
  @NotNull List<PatchInstruction> instructions;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> state;

  @Builder
  @ConstructorProperties({"name", "identifier", "feature", "environment", "instructions", "state"})
  public FeatureUpdateStepInfo(String name, String identifier, ParameterField<String> feature,
      ParameterField<String> environment, List<PatchInstruction> instructions, ParameterField<String> state) {
    this.name = name;
    this.identifier = identifier;
    this.feature = feature;
    this.environment = environment;
    this.instructions = instructions;
    this.state = state;
  }

  @Override
  public StepType getStepType() {
    return FeatureUpdateStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return SyncFacilitator.FACILITATOR_TYPE.getType();
  }

  @Override
  public SpecParameters getSpecParameters() {
    return FeatureUpdateStepParameters.builder()
        .identifier(identifier)
        .name(name)
        .feature(feature)
        .environment(environment)
        .state(state)
        .instructions(instructions)
        .build();
  }
}
