package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.cdstepinfo.K8sScaleStepInfoVisitorHelper;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

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
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.K8S_SCALE)
@SimpleVisitorHelper(helperClass = K8sScaleStepInfoVisitorHelper.class)
@TypeAlias("k8sScale")
public class K8sScaleStepInfo extends K8sScaleBaseStepInfo implements CDStepInfo, Visitable {
  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Builder(builderMethodName = "infoBuilder")
  public K8sScaleStepInfo(ParameterField<Boolean> skipDryRun, ParameterField<Boolean> skipSteadyStateCheck,
      InstanceSelectionWrapper instanceSelection, ParameterField<String> workload) {
    super(instanceSelection, workload, skipDryRun, skipSteadyStateCheck);
  }

  @Override
  public StepType getStepType() {
    return K8sScaleStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.K8S_SCALE).build();
  }

  @Override
  public SpecParameters getSpecParameters() {
    return K8sScaleStepParameter.infoBuilder()
        .instanceSelection(instanceSelection)
        .workload(workload)
        .skipDryRun(skipDryRun)
        .skipSteadyStateCheck(skipSteadyStateCheck)
        .build();
  }
}
