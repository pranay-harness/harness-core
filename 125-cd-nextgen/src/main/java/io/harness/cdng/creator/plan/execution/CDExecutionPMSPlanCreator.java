package io.harness.cdng.creator.plan.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.advisers.RollbackCustomAdviser;
import io.harness.cdng.creator.plan.rollback.RollbackPlanCreator;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.beans.OrchestrationConstants;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.steps.common.NGSectionStep;
import io.harness.steps.common.NGSectionStepParameters;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CDC)
public class CDExecutionPMSPlanCreator {
  public PlanCreationResponse createPlanForExecution(YamlField executionField) {
    YamlField stepsField = executionField.getNode().getField(YAMLFieldNameConstants.STEPS);
    if (stepsField == null || stepsField.getNode().asArray().size() == 0) {
      throw new InvalidRequestException("Execution section cannot have empty steps in a pipeline");
    }
    StepParameters stepParameters = NGSectionStepParameters.builder()
                                        .childNodeId(stepsField.getNode().getUuid())
                                        .logMessage("Execution Element")
                                        .build();
    PlanNode executionPlanNode =
        PlanNode.builder()
            .uuid(executionField.getNode().getUuid())
            .identifier(OrchestrationConstants.EXECUTION_NODE_IDENTIFIER)
            .stepType(NGSectionStep.STEP_TYPE)
            .group(StepOutcomeGroup.EXECUTION.name())
            .name(OrchestrationConstants.EXECUTION_NODE_NAME)
            .stepParameters(stepParameters)
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                    .build())
            .skipExpressionChain(false)
            .build();

    PlanCreationResponse executionPlanResponse =
        PlanCreationResponse.builder().node(executionPlanNode.getUuid(), executionPlanNode).build();
    LinkedHashMap<String, PlanCreationResponse> planForChildrenNodes = createPlanForChildrenNodes(executionField);
    for (PlanCreationResponse childPlan : planForChildrenNodes.values()) {
      executionPlanResponse.merge(childPlan);
    }
    return executionPlanResponse;
  }

  private LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(YamlField executionField) {
    LinkedHashMap<String, PlanCreationResponse> responseMap = new LinkedHashMap<>();
    List<YamlField> stepYamlFields = getStepYamlFields(executionField);
    for (YamlField stepYamlField : stepYamlFields) {
      Map<String, YamlField> stepYamlFieldMap = new HashMap<>();
      stepYamlFieldMap.put(stepYamlField.getNode().getUuid(), stepYamlField);
      responseMap.put(stepYamlField.getNode().getUuid(),
          PlanCreationResponse.builder().dependencies(DependenciesUtils.toDependenciesProto(stepYamlFieldMap)).build());
    }
    // Add Steps Node
    if (EmptyPredicate.isNotEmpty(stepYamlFields)) {
      YamlField stepsField =
          Preconditions.checkNotNull(executionField.getNode().getField(YAMLFieldNameConstants.STEPS));
      PlanNode stepsNode =
          getStepsPlanNode(stepsField.getNode().getUuid(), stepYamlFields.get(0).getNode().getUuid(), "Steps Element");
      responseMap.put(stepsNode.getUuid(), PlanCreationResponse.builder().node(stepsNode.getUuid(), stepsNode).build());
    }

    YamlField executionStepsField = executionField.getNode().getField(YAMLFieldNameConstants.STEPS);

    PlanCreationResponse planForRollback = RollbackPlanCreator.createPlanForRollback(executionField);
    if (EmptyPredicate.isNotEmpty(planForRollback.getNodes())) {
      responseMap.put(
          Objects.requireNonNull(executionStepsField).getNode().getUuid() + "_combinedRollback", planForRollback);
    }
    return responseMap;
  }

  private List<YamlField> getStepYamlFields(YamlField executionField) {
    List<YamlNode> yamlNodes =
        Optional
            .of(Preconditions.checkNotNull(executionField.getNode().getField(YAMLFieldNameConstants.STEPS))
                    .getNode()
                    .asArray())
            .orElse(Collections.emptyList());

    return PlanCreatorUtils.getStepYamlFields(yamlNodes);
  }

  public PlanNode getStepsPlanNode(String nodeUuid, String childNodeId, String logMessage) {
    StepParameters stepParameters =
        NGSectionStepParameters.builder().childNodeId(childNodeId).logMessage(logMessage).build();
    return PlanNode.builder()
        .uuid(nodeUuid)
        .identifier(YAMLFieldNameConstants.STEPS)
        .stepType(NGSectionStep.STEP_TYPE)
        .name(YAMLFieldNameConstants.STEPS)
        .stepParameters(stepParameters)
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                .build())
        .adviserObtainment(AdviserObtainment.newBuilder().setType(RollbackCustomAdviser.ADVISER_TYPE).build())
        .skipGraphType(SkipType.SKIP_NODE)
        .build();
  }
}
