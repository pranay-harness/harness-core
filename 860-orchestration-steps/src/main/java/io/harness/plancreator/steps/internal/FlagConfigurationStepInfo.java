package io.harness.plancreator.steps.internal;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.cf.FlagConfigurationStep;
import io.harness.steps.cf.FlagConfigurationStepParameters;
import io.harness.steps.cf.PatchInstruction;

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
@JsonTypeName("FlagConfiguration")
@TypeAlias("flagConfigurationStepInfo")
public class FlagConfigurationStepInfo implements PMSStepInfo {
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String name;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> feature;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> environment;
  @NotNull List<PatchInstruction> instructions;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> state;

  @Builder
  @ConstructorProperties({"name", "feature", "environment", "instructions", "state"})
  public FlagConfigurationStepInfo(String name, ParameterField<String> feature, ParameterField<String> environment,
      List<PatchInstruction> instructions, ParameterField<String> state) {
    this.name = name;
    this.feature = feature;
    this.environment = environment;
    this.instructions = instructions;
    this.state = state;
  }

  @Override
  public StepType getStepType() {
    return FlagConfigurationStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.SYNC;
  }

  @Override
  public SpecParameters getSpecParameters() {
    return FlagConfigurationStepParameters
        .builder()
        //.identifier(identifier)
        .name(name)
        .feature(feature)
        .environment(environment)
        .state(state)
        .instructions(instructions)
        .build();
  }
}
