package io.harness.cdng.creator.plan.infrastructure;

import io.harness.cdng.creator.plan.stage.DeploymentStageConfig;
import io.harness.cdng.infra.steps.InfraSectionStepParameters;
import io.harness.cdng.infra.steps.InfraStepParameters;
import io.harness.cdng.infra.steps.InfrastructureSectionStep;
import io.harness.cdng.infra.steps.InfrastructureStep;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.facilitator.child.ChildFacilitator;
import io.harness.pms.sdk.core.facilitator.sync.SyncFacilitator;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class InfrastructurePmsPlanCreator {
  public PlanNode getInfraStepPlanNode(
      String uuid, PipelineInfrastructure pipelineInfrastructure, YamlField infraField) {
    PipelineInfrastructure actualInfraConfig = getActualInfraConfig(pipelineInfrastructure, infraField);

    return PlanNode.builder()
        .uuid(uuid)
        .name(PlanCreatorConstants.INFRA_NODE_NAME)
        .identifier(PlanCreatorConstants.INFRA_DEFINITION_NODE_IDENTIFIER)
        .stepType(InfrastructureStep.STEP_TYPE)
        .skipExpressionChain(true)
        .stepParameters(InfraStepParameters.builder().pipelineInfrastructure(actualInfraConfig).build())
        .facilitatorObtainment(FacilitatorObtainment.newBuilder().setType(SyncFacilitator.FACILITATOR_TYPE).build())
        .build();
  }

  public PlanNode getInfraSectionPlanNode(YamlNode infraSectionNode, String infraStepNodeUuid,
      PipelineInfrastructure infrastructure, KryoSerializer kryoSerializer, YamlField infraField) {
    PipelineInfrastructure actualInfraConfig = getActualInfraConfig(infrastructure, infraField);

    return PlanNode.builder()
        .uuid(infraSectionNode.getUuid())
        .name(PlanCreatorConstants.INFRA_SECTION_NODE_IDENTIFIER)
        .identifier(PlanCreatorConstants.INFRA_SECTION_NODE_IDENTIFIER)
        .stepType(InfrastructureSectionStep.STEP_TYPE)
        .skipGraphType(SkipType.SKIP_NODE)
        .stepParameters(InfraSectionStepParameters.getStepParameters(actualInfraConfig, infraStepNodeUuid))
        .facilitatorObtainment(FacilitatorObtainment.newBuilder().setType(ChildFacilitator.FACILITATOR_TYPE).build())
        .adviserObtainments(getAdviserObtainmentFromMetaData(infraSectionNode, kryoSerializer))
        .build();
  }

  private List<AdviserObtainment> getAdviserObtainmentFromMetaData(
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

  /** Method returns actual InfraStructure object by resolving useFromStage if present. */
  private PipelineInfrastructure getActualInfraConfig(
      PipelineInfrastructure pipelineInfrastructure, YamlField infraField) {
    if (pipelineInfrastructure.getUseFromStage() != null) {
      if (pipelineInfrastructure.getInfrastructureDefinition() != null) {
        throw new InvalidArgumentsException("Infrastructure definition should not exist with UseFromStage.");
      }
      try {
        //  Add validation for not chaining of stages
        DeploymentStageConfig deploymentStage = YamlUtils.read(
            PlanCreatorUtils.getStageConfig(infraField, pipelineInfrastructure.getUseFromStage().getStage())
                .getNode()
                .toString(),
            DeploymentStageConfig.class);
        if (deploymentStage != null) {
          return pipelineInfrastructure.applyUseFromStage(deploymentStage.getInfrastructure());
        } else {
          throw new InvalidArgumentsException("Stage identifier given in useFromStage doesn't exist.");
        }
      } catch (IOException ex) {
        throw new InvalidRequestException("cannot convert stage YamlField to Stage Object");
      }
    }
    return pipelineInfrastructure;
  }
}
