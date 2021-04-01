package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.visitor.YamlTypes;
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
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.K8S_DELETE)
@SimpleVisitorHelper(helperClass = K8sDeleteStepInfoVisitorHelper.class)
@TypeAlias("K8sDeleteStepInfo")
public class K8sDeleteStepInfo extends K8sDeleteBaseStepInfo implements CDStepInfo, Visitable {
  @JsonIgnore private String name;
  @JsonIgnore private String identifier;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Builder(builderMethodName = "infoBuilder")
  public K8sDeleteStepInfo(
      DeleteResourcesWrapper deleteResources, ParameterField<Boolean> skipDryRun, String name, String identifier) {
    super(deleteResources, skipDryRun);
    this.name = name;
    this.identifier = identifier;
  }

  public K8sDeleteStepInfo(String name, String identifier) {
    this.name = name;
    this.identifier = identifier;
  }

  @Override
  public String getDisplayName() {
    return name;
  }

  @Override
  public StepType getStepType() {
    return K8sDeleteStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK_CHAIN;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.K8S_DELETE).build();
  }

  @NotNull
  @Override
  public String getIdentifier() {
    return identifier;
  }

  @Override
  public StepParameters getStepParametersWithRollbackInfo(BaseStepParameterInfo stepParameterInfo) {
    return K8sDeleteStepParameters.infoBuilder()
        .deleteResources(this.getDeleteResources())
        .skipDryRun(this.getSkipDryRun())
        .rollbackInfo(stepParameterInfo.getRollbackInfo())
        .timeout(stepParameterInfo.getTimeout())
        .name(stepParameterInfo.getName())
        .description(stepParameterInfo.getDescription())
        .identifier(stepParameterInfo.getIdentifier())
        .skipCondition(stepParameterInfo.getSkipCondition())
        .build();
  }
}
