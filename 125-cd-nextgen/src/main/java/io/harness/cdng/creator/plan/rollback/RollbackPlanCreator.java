package io.harness.cdng.creator.plan.rollback;

import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters.RollbackOptionalChildChainStepParametersBuilder;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildChainStep;
import io.harness.data.structure.EmptyPredicate;
import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class RollbackPlanCreator implements PartialPlanCreator<YamlField> {
  @Override
  public Class<YamlField> getFieldClass() {
    return YamlField.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("rollbackSteps", Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, YamlField executionRollbackSteps) {
    YamlNode stageNode = YamlUtils.getGivenYamlNodeFromParentPath(ctx.getCurrentField().getNode(), "stage");
    RollbackOptionalChildChainStepParametersBuilder stepParametersBuilder =
        RollbackOptionalChildChainStepParameters.builder();

    YamlField executionStepsField =
        YamlUtils.getGivenYamlNodeFromParentPath(ctx.getCurrentField().getNode(), "execution").getField("steps");
    PlanCreationResponse stepGroupsRollbackPlanNode =
        StepGroupsRollbackPMSPlanCreator.createStepGroupsRollbackPlanNode(executionStepsField);

    String executionNodeFullIdentifier = String.join(".", PlanCreatorConstants.STAGES_NODE_IDENTIFIER,
        stageNode.getIdentifier(), PlanCreatorConstants.EXECUTION_NODE_IDENTIFIER);
    if (EmptyPredicate.isNotEmpty(stepGroupsRollbackPlanNode.getNodes())) {
      stepParametersBuilder.childNode(RollbackNode.builder()
                                          .nodeId(executionStepsField.getNode().getUuid() + "_rollback")
                                          .dependentNodeIdentifier(executionNodeFullIdentifier)
                                          .build());
    }
    PlanCreationResponse executionRollbackPlanNode =
        ExecutionRollbackPMSPlanCreator.createExecutionRollbackPlanNode(executionRollbackSteps);
    if (EmptyPredicate.isNotEmpty(executionRollbackPlanNode.getNodes())) {
      stepParametersBuilder.childNode(RollbackNode.builder()
                                          .nodeId(executionRollbackSteps.getNode().getUuid() + "_executionrollback")
                                          .dependentNodeIdentifier(executionNodeFullIdentifier)
                                          .build());
    }

    PlanNode deploymentStageRollbackNode =
        PlanNode.builder()
            .uuid(executionRollbackSteps.getNode().getUuid() + "_section")
            .name(stageNode.getNameOrIdentifier() + ":Rollback")
            .identifier(stageNode.getIdentifier() + "Rollback")
            .stepType(RollbackOptionalChildChainStep.STEP_TYPE)
            .stepParameters(stepParametersBuilder.build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD_CHAIN).build())
                    .build())
            .skipExpressionChain(true)
            .build();

    PlanCreationResponse finalResponse =
        PlanCreationResponse.builder().node(deploymentStageRollbackNode.getUuid(), deploymentStageRollbackNode).build();
    finalResponse.merge(stepGroupsRollbackPlanNode);

    return finalResponse;
  }
}
