package io.harness.plancreator.steps.barrier;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.plancreator.steps.internal.PMSStepInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.async.AsyncFacilitator;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.barriers.BarrierStep;
import io.harness.steps.barriers.BarrierStepParameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.beans.ConstructorProperties;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@EqualsAndHashCode
@JsonTypeName(StepSpecTypeConstants.BARRIER)
@TypeAlias("barrierStepInfo")
@OwnedBy(PIPELINE)
public class BarrierStepInfo implements PMSStepInfo {
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String name;
  @JsonProperty("barrierRef") @NotNull String identifier;

  @Builder
  @ConstructorProperties({"name", "identifier"})
  public BarrierStepInfo(String name, String identifier) {
    this.name = name;
    this.identifier = identifier;
  }

  @Override
  public StepType getStepType() {
    return BarrierStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return AsyncFacilitator.FACILITATOR_TYPE.getType();
  }

  @Override
  public StepParameters getStepParametersInfo(StepElementConfig stepElementConfig) {
    return BarrierStepParameters.builder().identifier(identifier).build();
  }
}
