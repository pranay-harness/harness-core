package io.harness.cdng.k8s;

import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.cdstepinfo.K8sRollingStepInfoVisitorHelper;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.steps.io.BaseStepParameterInfo;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
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

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.K8S_ROLLING_DEPLOY)
@SimpleVisitorHelper(helperClass = K8sRollingStepInfoVisitorHelper.class)
@TypeAlias("k8sRollingStepInfo")
public class K8sRollingStepInfo extends K8sRollingBaseStepInfo implements CDStepInfo, Visitable {
  @JsonIgnore private String name;
  @JsonIgnore private String identifier;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Builder(builderMethodName = "infoBuilder")
  public K8sRollingStepInfo(ParameterField<Boolean> skipDryRun, String name, String identifier) {
    super(skipDryRun);
    this.name = name;
    this.identifier = identifier;
  }

  public K8sRollingStepInfo(String name, String identifier) {
    this.name = name;
    this.identifier = identifier;
  }

  @Override
  public String getDisplayName() {
    return name;
  }

  @Override
  public StepType getStepType() {
    return K8sRollingStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK_CHAIN;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.K8S_ROLLING_DEPLOY).build();
  }

  @Override
  public StepParameters getStepParametersWithRollbackInfo(BaseStepParameterInfo baseStepParameterInfo) {
    return K8sRollingStepParameters.infoBuilder()
        .timeout(baseStepParameterInfo.getTimeout())
        .rollbackInfo(baseStepParameterInfo.getRollbackInfo())
        .name(baseStepParameterInfo.getName())
        .identifier(baseStepParameterInfo.getIdentifier())
        .skipCondition(baseStepParameterInfo.getSkipCondition())
        .description(baseStepParameterInfo.getDescription())
        .skipDryRun(skipDryRun)
        .build();
  }
}
