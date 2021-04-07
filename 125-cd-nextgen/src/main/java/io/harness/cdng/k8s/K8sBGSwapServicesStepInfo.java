package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.common.SwaggerConstants;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.steps.io.BaseStepParameterInfo;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode
@JsonTypeName(StepSpecTypeConstants.K8S_BG_SWAP_SERVICES)
@TypeAlias("k8sBGSwapServicesStepInfo")
public class K8sBGSwapServicesStepInfo implements CDStepInfo, Visitable {
  @JsonIgnore private String name;
  @JsonIgnore private String identifier;
  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH) ParameterField<Boolean> skipDryRun;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Builder(builderMethodName = "infoBuilder")
  public K8sBGSwapServicesStepInfo(String name, String identifier) {
    this.name = name;
    this.identifier = identifier;
  }

  @Override
  public String getDisplayName() {
    return name;
  }

  @Override
  public StepType getStepType() {
    return K8sBGSwapServicesStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.K8S_BG_SWAP_SERVICES).build();
  }

  @Override
  public StepParameters getStepParametersWithRollbackInfo(BaseStepParameterInfo stepParameterInfo) {
    return K8sBGSwapServicesStepParameters.infoBuilder()
        .timeout(stepParameterInfo.getTimeout())
        .name(stepParameterInfo.getName())
        .identifier(stepParameterInfo.getIdentifier())
        .description(stepParameterInfo.getDescription())
        .skipCondition(stepParameterInfo.getSkipCondition())
        .skipDryRun(skipDryRun)
        .build();
  }
}
