package io.harness.plancreator.approval;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.stages.GenericStagePlanCreator;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.steps.approval.stage.ApprovalStageSpecParameters;
import io.harness.steps.approval.stage.ApprovalStageStep;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public class ApprovalStagePlanCreator extends GenericStagePlanCreator {
  @Override
  public Set<String> getSupportedStageTypes() {
    return Collections.singleton("Approval");
  }

  @Override
  public StepType getStepType(StageElementConfig stageElementConfig) {
    return ApprovalStageStep.STEP_TYPE;
  }

  @Override
  public SpecParameters getSpecParameters(String childNodeId) {
    return ApprovalStageSpecParameters.getStepParameters(childNodeId);
  }

  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, StageElementConfig field) {
    LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();
    Map<String, YamlField> dependenciesNodeMap = new HashMap<>();

    // Add dependency for execution
    YamlField executionField =
        Objects.requireNonNull(ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.SPEC))
            .getNode()
            .getField(YAMLFieldNameConstants.EXECUTION);
    if (executionField == null) {
      throw new InvalidRequestException("Execution section is required in Approval stage");
    }
    dependenciesNodeMap.put(executionField.getNode().getUuid(), executionField);

    planCreationResponseMap.put(
        executionField.getNode().getUuid(), PlanCreationResponse.builder().dependencies(dependenciesNodeMap).build());
    return planCreationResponseMap;
  }
}
