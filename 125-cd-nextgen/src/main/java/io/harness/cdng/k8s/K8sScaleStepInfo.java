package io.harness.cdng.k8s;

import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.cdstepinfo.K8sScaleStepInfoVisitorHelper;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.steps.io.RollbackInfo;
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

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.K8S_SCALE)
@SimpleVisitorHelper(helperClass = K8sScaleStepInfoVisitorHelper.class)
@TypeAlias("k8sScale")
public class K8sScaleStepInfo extends K8sScaleBaseStepInfo implements CDStepInfo, Visitable {
  @JsonIgnore private String name;
  @JsonIgnore private String identifier;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Builder(builderMethodName = "infoBuilder")
  public K8sScaleStepInfo(ParameterField<Boolean> skipDryRun, ParameterField<Boolean> skipSteadyStateCheck,
      InstanceSelectionWrapper instanceSelection, ParameterField<String> workload, String name, String identifier) {
    super(instanceSelection, workload, skipDryRun, skipSteadyStateCheck);
    this.name = name;
    this.identifier = identifier;
  }

  public K8sScaleStepInfo(String name, String identifier) {
    this.name = name;
    this.identifier = identifier;
  }

  @Override
  public String getDisplayName() {
    return name;
  }

  @Override
  public StepType getStepType() {
    return K8sScaleStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK;
  }

  @NotNull
  @Override
  public String getIdentifier() {
    return identifier;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.K8S_SCALE).build();
  }

  @Override
  public StepParameters getStepParametersWithRollbackInfo(RollbackInfo rollbackInfo, ParameterField<String> timeout) {
    return K8sScaleStepParameter.infoBuilder()
        .instanceSelection(instanceSelection)
        .rollbackInfo(rollbackInfo)
        .timeout(timeout)
        .skipDryRun(skipDryRun)
        .skipSteadyStateCheck(skipSteadyStateCheck)
        .build();
  }
}
