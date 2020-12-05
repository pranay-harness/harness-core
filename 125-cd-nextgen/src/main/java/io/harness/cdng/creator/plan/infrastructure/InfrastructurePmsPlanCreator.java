package io.harness.cdng.creator.plan.infrastructure;

import io.harness.adviser.OrchestrationAdviserTypes;
import io.harness.advisers.success.OnSuccessAdviserParameters;
import io.harness.cdng.infra.steps.InfraSectionStepParameters;
import io.harness.cdng.infra.steps.InfraStepParameters;
import io.harness.cdng.infra.steps.InfrastructureSectionStep;
import io.harness.cdng.infra.steps.InfrastructureStep;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.pms.advisers.AdviserObtainment;
import io.harness.pms.advisers.AdviserType;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.sdk.core.facilitator.child.ChildFacilitator;
import io.harness.pms.sdk.core.facilitator.sync.SyncFacilitator;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.steps.SkipType;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.serializer.KryoSerializer;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;

public class InfrastructurePmsPlanCreator {
  public static PlanNode getInfraStepPlanNode(String uuid, PipelineInfrastructure pipelineInfrastructure) {
    return PlanNode.builder()
        .uuid(uuid)
        .name(PlanCreatorConstants.INFRA_NODE_NAME)
        .identifier(PlanCreatorConstants.INFRA_DEFINITION_NODE_IDENTIFIER)
        .stepType(InfrastructureStep.STEP_TYPE)
        .skipExpressionChain(true)
        .stepParameters(InfraStepParameters.builder().pipelineInfrastructure(pipelineInfrastructure).build())
        .facilitatorObtainment(FacilitatorObtainment.newBuilder().setType(SyncFacilitator.FACILITATOR_TYPE).build())
        .build();
  }

  public static PlanNode getInfraSectionPlanNode(YamlNode infraSectionNode, String infraStepNodeUuid,
      PipelineInfrastructure infrastructure, KryoSerializer kryoSerializer) {
    return PlanNode.builder()
        .uuid(infraSectionNode.getUuid())
        .name(PlanCreatorConstants.INFRA_SECTION_NODE_IDENTIFIER)
        .identifier(PlanCreatorConstants.INFRA_SECTION_NODE_IDENTIFIER)
        .stepType(InfrastructureSectionStep.STEP_TYPE)
        .skipGraphType(SkipType.SKIP_NODE)
        .stepParameters(InfraSectionStepParameters.getStepParameters(infrastructure, infraStepNodeUuid))
        .facilitatorObtainment(FacilitatorObtainment.newBuilder().setType(ChildFacilitator.FACILITATOR_TYPE).build())
        .adviserObtainments(getAdviserObtainmentFromMetaData(infraSectionNode, kryoSerializer))
        .build();
  }

  private static List<AdviserObtainment> getAdviserObtainmentFromMetaData(
      YamlNode currentNode, KryoSerializer kryoSerializer) {
    List<AdviserObtainment> adviserObtainments = new ArrayList<>();
    if (currentNode != null) {
      YamlField siblingField = currentNode.nextSiblingNodeFromParentObject("execution");
      if (siblingField != null && siblingField.getNode().getUuid() != null) {
        adviserObtainments.add(
            AdviserObtainment.newBuilder()
                .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                    OnSuccessAdviserParameters.builder().nextNodeId(siblingField.getNode().getUuid()).build())))
                .build());
      }
    }
    return adviserObtainments;
  }
}
