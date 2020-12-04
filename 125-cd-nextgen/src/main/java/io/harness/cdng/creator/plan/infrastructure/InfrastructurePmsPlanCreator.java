package io.harness.cdng.creator.plan.infrastructure;

import io.harness.cdng.infra.steps.InfraStepParameters;
import io.harness.cdng.infra.steps.InfrastructureStep;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.steps.SkipType;
import io.harness.steps.section.chain.SectionChainStepParameters;

public class InfrastructurePmsPlanCreator {
  public static PlanNode getInfraStepPlanNode(String uuid, PipelineInfrastructure pipelineInfrastructure) {
    final String infraIdentifier = "infrastructureDefinition";

    return PlanNode.builder()
        .uuid(uuid)
        .name(PlanCreatorConstants.INFRA_NODE_NAME)
        .identifier(infraIdentifier)
        .stepType(InfrastructureStep.STEP_TYPE)
        .skipExpressionChain(true)
        .stepParameters(InfraStepParameters.builder().pipelineInfrastructure(pipelineInfrastructure).build())
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                .build())
        .build();
  }

  public static PlanNode getInfraSectionPlanNode(String uuid, String infraStepNodeUuid) {
    return PlanNode.builder()
        .uuid(uuid)
        .name(PlanCreatorConstants.INFRA_SECTION_NODE_IDENTIFIER)
        .identifier(PlanCreatorConstants.INFRA_SECTION_NODE_IDENTIFIER)
        .stepType(InfrastructureStep.STEP_TYPE)
        .stepParameters(SectionChainStepParameters.builder().childNodeId(infraStepNodeUuid).build())
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD_CHAIN).build())
                .build())
        .skipGraphType(SkipType.SKIP_NODE)
        .build();
  }
}
