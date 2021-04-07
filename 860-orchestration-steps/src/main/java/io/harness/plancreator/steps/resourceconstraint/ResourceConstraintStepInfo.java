package io.harness.plancreator.steps.resourceconstraint;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.internal.PMSStepInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.resourcerestraint.ResourceRestraintFacilitator;
import io.harness.steps.resourcerestraint.ResourceRestraintStep;
import io.harness.steps.resourcerestraint.ResourceRestraintStepParameters;
import io.harness.steps.resourcerestraint.beans.AcquireMode;
import io.harness.steps.resourcerestraint.beans.HoldingScope;
import io.harness.yaml.schema.YamlSchemaIgnoreSubtype;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@Builder
@EqualsAndHashCode
@JsonTypeName(StepSpecTypeConstants.RESOURCE_CONSTRAINT)
@TypeAlias("resourceConstraintStepInfo")
@YamlSchemaIgnoreSubtype
public class ResourceConstraintStepInfo implements PMSStepInfo {
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String identifier;

  @NotNull String name;
  @NotNull String resourceUnit;
  @NotNull AcquireMode acquireMode;
  @NotNull int permits;
  @NotNull HoldingScope holdingScope;

  @Override
  public StepType getStepType() {
    return ResourceRestraintStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return ResourceRestraintFacilitator.FACILITATOR_TYPE.getType();
  }

  @Override
  public StepParameters getStepParameters() {
    return ResourceRestraintStepParameters.builder()
        .name(name)
        .resourceUnit(resourceUnit)
        .acquireMode(acquireMode)
        .holdingScope(holdingScope)
        .permits(permits)
        .build();
  }
}
