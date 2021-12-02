package io.harness.cdng.helm.rollback;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.helm.HelmRollbackStep;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
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
@JsonTypeName(StepSpecTypeConstants.HELM_ROLLBACK)
@SimpleVisitorHelper(helperClass = HelmRollbackStepInfoVisitorHelper.class)
@TypeAlias("HelmRollbackStepInfo")
@RecasterAlias("io.harness.cdng.helm.rollback.HelmRollbackStepInfo")
public class HelmRollbackStepInfo extends HelmRollbackBaseStepInfo implements CDStepInfo, Visitable {
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Builder(builderMethodName = "infoBuilder")
  public HelmRollbackStepInfo(ParameterField<List<TaskSelectorYaml>> delegateSelectors, String helmRollbackFqn) {
    super(delegateSelectors, helmRollbackFqn);
  }

  @Override
  public StepType getStepType() {
    return HelmRollbackStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK;
  }

  @Override
  public SpecParameters getSpecParameters() {
    return HelmRollbackStepParams.infoBuilder().delegateSelectors(delegateSelectors).build();
  }
}
