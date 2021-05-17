package io.harness.cdng.creator.plan.infrastructure;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.advisers.RollbackCustomAdviser;
import io.harness.cdng.creator.plan.PlanCreatorConstants;
import io.harness.cdng.creator.plan.stage.DeploymentStageConfig;
import io.harness.cdng.infra.steps.InfraSectionStepParameters;
import io.harness.cdng.infra.steps.InfraStepParameters;
import io.harness.cdng.infra.steps.InfrastructureSectionStep;
import io.harness.cdng.infra.steps.InfrastructureStep;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.utils.CommonPlanCreatorUtils;
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
import io.harness.pms.sdk.core.plan.PlanNode.PlanNodeBuilder;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.common.NGSectionStep;
import io.harness.steps.common.NGSectionStepParameters;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class InfrastructurePmsPlanCreator {
  public PlanNode getInfraStepPlanNode(PipelineInfrastructure pipelineInfrastructure, YamlField infraField) {
    PipelineInfrastructure actualInfraConfig = getActualInfraConfig(pipelineInfrastructure, infraField);
    return PlanNode.builder()
        .uuid(UUIDGenerator.generateUuid())
        .name(PlanCreatorConstants.INFRA_NODE_NAME)
        .identifier(PlanCreatorConstants.INFRA_DEFINITION_NODE_IDENTIFIER)
        .stepType(InfrastructureStep.STEP_TYPE)
        .skipExpressionChain(true)
        .stepParameters(InfraStepParameters.builder().pipelineInfrastructure(actualInfraConfig).build())
        .facilitatorObtainment(FacilitatorObtainment.newBuilder().setType(SyncFacilitator.FACILITATOR_TYPE).build())
        .build();
  }

  public PlanNode getInfraSectionPlanNode(YamlNode infraSectionNode, String infraStepNodeUuid,
      PipelineInfrastructure infrastructure, KryoSerializer kryoSerializer, YamlField infraField,
      YamlField resourceConstraintField) {
    PipelineInfrastructure actualInfraConfig = getActualInfraConfig(infrastructure, infraField);

    if (!ParameterField.isNull(infrastructure.getAllowSimultaneousDeployments())
        && infrastructure.getAllowSimultaneousDeployments().isExpression()) {
      throw new InvalidRequestException(
          "AllowedSimultaneous Deployment field is not a fixed value during execution of pipeline.");
    }
    boolean allowSimultaneousDeployments = false;
    if (!ParameterField.isNull(infrastructure.getAllowSimultaneousDeployments())) {
      allowSimultaneousDeployments = infrastructure.getAllowSimultaneousDeployments().getValue();
    }

    PlanNodeBuilder planNodeBuilder =
        PlanNode.builder()
            .uuid(infraSectionNode.getUuid())
            .name(PlanCreatorConstants.INFRA_SECTION_NODE_NAME)
            .identifier(PlanCreatorConstants.INFRA_SECTION_NODE_IDENTIFIER)
            .group(OutcomeExpressionConstants.INFRASTRUCTURE_GROUP)
            .stepType(InfrastructureSectionStep.STEP_TYPE)
            .stepParameters(InfraSectionStepParameters.getStepParameters(actualInfraConfig, infraStepNodeUuid))
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder().setType(ChildFacilitator.FACILITATOR_TYPE).build())
            .adviserObtainments(allowSimultaneousDeployments
                    ? getAdviserObtainmentFromMetaDataToResourceConstraint(resourceConstraintField, kryoSerializer)
                    : getAdviserObtainmentFromMetaDataToExecution(infraSectionNode, kryoSerializer));

    if (!isProvisionerConfigured(actualInfraConfig)) {
      planNodeBuilder.skipGraphType(SkipType.SKIP_NODE);
    }
    return planNodeBuilder.build();
  }

  private List<AdviserObtainment> getAdviserObtainmentFromMetaDataToExecution(
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
    adviserObtainments.add(AdviserObtainment.newBuilder().setType(RollbackCustomAdviser.ADVISER_TYPE).build());
    return adviserObtainments;
  }

  private List<AdviserObtainment> getAdviserObtainmentFromMetaDataToResourceConstraint(
      YamlField resourceConstraintField, KryoSerializer kryoSerializer) {
    List<AdviserObtainment> adviserObtainments = new ArrayList<>();
    if (resourceConstraintField != null && resourceConstraintField.getNode().getUuid() != null) {
      adviserObtainments.add(
          AdviserObtainment.newBuilder()
              .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
              .setParameters(ByteString.copyFrom(
                  kryoSerializer.asBytes(OnSuccessAdviserParameters.builder()
                                             .nextNodeId(resourceConstraintField.getNode().getUuid())
                                             .build())))
              .build());
    }
    adviserObtainments.add(AdviserObtainment.newBuilder().setType(RollbackCustomAdviser.ADVISER_TYPE).build());
    return adviserObtainments;
  }

  /**
   * Method returns actual InfraStructure object by resolving useFromStage if present.
   */
  public PipelineInfrastructure getActualInfraConfig(
      PipelineInfrastructure pipelineInfrastructure, YamlField infraField) {
    if (pipelineInfrastructure.getUseFromStage() != null) {
      if (pipelineInfrastructure.getInfrastructureDefinition() != null) {
        throw new InvalidArgumentsException("Infrastructure definition should not exist with UseFromStage.");
      }
      try {
        //  Add validation for not chaining of stages
        StageElementConfig stageElementConfig = YamlUtils.read(
            PlanCreatorUtils.getStageConfig(infraField, pipelineInfrastructure.getUseFromStage().getStage())
                .getNode()
                .toString(),
            StageElementConfig.class);
        DeploymentStageConfig deploymentStage = (DeploymentStageConfig) stageElementConfig.getStageType();
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

  public LinkedHashMap<String, PlanCreationResponse> createPlanForProvisioner(PipelineInfrastructure actualInfraConfig,
      YamlField infraField, String infraStepNodeId, KryoSerializer kryoSerializer) {
    if (!isProvisionerConfigured(actualInfraConfig)) {
      return new LinkedHashMap<>();
    }
    validateProvisionerConfig(actualInfraConfig.getInfrastructureDefinition().getProvisioner());

    YamlField infraDefField = infraField.getNode().getField(YamlTypes.INFRASTRUCTURE_DEF);
    YamlField provisionerYamlField = infraDefField.getNode().getField(YAMLFieldNameConstants.PROVISIONER);
    YamlField stepsYamlField = provisionerYamlField.getNode().getField(YAMLFieldNameConstants.STEPS);
    List<YamlNode> stepYamlNodes = stepsYamlField.getNode().asArray();

    // Add each step dependency
    LinkedHashMap<String, PlanCreationResponse> responseMap = new LinkedHashMap<>();
    List<YamlField> stepYamlFields = PlanCreatorUtils.getStepYamlFields(stepYamlNodes);
    for (YamlField stepYamlField : stepYamlFields) {
      Map<String, YamlField> stepYamlFieldMap = new HashMap<>();
      stepYamlFieldMap.put(stepYamlField.getNode().getUuid(), stepYamlField);
      responseMap.put(
          stepYamlField.getNode().getUuid(), PlanCreationResponse.builder().dependencies(stepYamlFieldMap).build());
    }

    // Add Steps Node
    PlanNode stepsNode = CommonPlanCreatorUtils.getStepsPlanNode(
        stepsYamlField.getNode().getUuid(), stepYamlFields.get(0).getNode().getUuid(), "Provisioner Steps Element");
    responseMap.put(stepsNode.getUuid(), PlanCreationResponse.builder().node(stepsNode.getUuid(), stepsNode).build());

    // Add provisioner Node
    PlanNode provisionerPlanNode =
        getProvisionerPlanNode(provisionerYamlField, stepsNode.getUuid(), infraStepNodeId, kryoSerializer);
    responseMap.put(provisionerPlanNode.getUuid(),
        PlanCreationResponse.builder().node(provisionerPlanNode.getUuid(), provisionerPlanNode).build());

    return responseMap;
  }

  private static void validateProvisionerConfig(@NotNull ExecutionElementConfig provisioner) {
    if (EmptyPredicate.isEmpty(provisioner.getSteps())) {
      throw new InvalidRequestException("Steps under Provisioner Section can't be empty");
    }
  }

  public boolean isProvisionerConfigured(PipelineInfrastructure actualInfraConfig) {
    return actualInfraConfig.getInfrastructureDefinition().getProvisioner() != null;
  }

  private PlanNode getProvisionerPlanNode(
      YamlField provisionerYamlField, String childNodeId, String infraStepNodeId, KryoSerializer kryoSerializer) {
    StepParameters stepParameters =
        NGSectionStepParameters.builder().childNodeId(childNodeId).logMessage("Provisioner Section").build();
    return PlanNode.builder()
        .uuid(provisionerYamlField.getNode().getUuid())
        .identifier(YAMLFieldNameConstants.PROVISIONER)
        .stepType(NGSectionStep.STEP_TYPE)
        .name(YAMLFieldNameConstants.PROVISIONER)
        .stepParameters(stepParameters)
        .facilitatorObtainment(FacilitatorObtainment.newBuilder().setType(ChildFacilitator.FACILITATOR_TYPE).build())
        .adviserObtainment(
            AdviserObtainment.newBuilder()
                .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                .setParameters(ByteString.copyFrom(
                    kryoSerializer.asBytes(OnSuccessAdviserParameters.builder().nextNodeId(infraStepNodeId).build())))
                .build())
        .skipGraphType(SkipType.SKIP_NODE)
        .build();
  }

  public String getProvisionerNodeId(YamlField infraField) {
    YamlField infraDefField = infraField.getNode().getField(YamlTypes.INFRASTRUCTURE_DEF);
    YamlField provisionerYamlField = infraDefField.getNode().getField(YAMLFieldNameConstants.PROVISIONER);
    return provisionerYamlField.getNode().getUuid();
  }

  public boolean areSimultaneousDeploymentsAllowed(YamlNode infraNode) {
    return infraNode.getCurrJsonNode().get("allowSimultaneousDeployments") != null;
  }
}
