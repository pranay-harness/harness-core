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
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@JsonTypeName(StepSpecTypeConstants.K8S_CANARY_DELETE)
@TypeAlias("k8sCanaryDeleteStepInfo")
public class K8sCanaryDeleteStepInfo implements CDStepInfo, Visitable {
  @JsonIgnore private String name;
  @JsonIgnore private String identifier;
  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH) ParameterField<Boolean> skipDryRun;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Builder(builderMethodName = "infoBuilder")
  public K8sCanaryDeleteStepInfo(String name, String identifier) {
    this.name = name;
    this.identifier = identifier;
  }

  @Override
  public String getDisplayName() {
    return name;
  }

  @Override
  public StepType getStepType() {
    return K8sCanaryDeleteStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK;
  }

  @Override
  public StepParameters getStepParametersWithRollbackInfo(BaseStepParameterInfo baseStepParameterInfo) {
    return K8sCanaryDeleteStepParameters.infoBuilder()
        .name(baseStepParameterInfo.getName())
        .description(baseStepParameterInfo.getDescription())
        .skipCondition(baseStepParameterInfo.getSkipCondition())
        .timeout(baseStepParameterInfo.getTimeout())
        .skipDryRun(skipDryRun)
        .build();
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.K8S_CANARY_DELETE).build();
  }
}
