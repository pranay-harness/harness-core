package io.harness.cdng.creator.plan.stage;

import io.harness.cdng.creator.plan.infrastructure.InfrastructurePmsPlanCreator;
import io.harness.cdng.creator.plan.service.ServicePMSPlanCreator;
import io.harness.cdng.pipeline.beans.DeploymentStageStepParameters;
import io.harness.cdng.pipeline.steps.DeploymentStageStep;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.facilitator.child.ChildFacilitator;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepOutcomeGroup;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeploymentStagePMSPlanCreator extends ChildrenPlanCreator<StageElementConfig> {
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, StageElementConfig field) {
    LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();
    Map<String, YamlField> dependenciesNodeMap = new HashMap<>();

    // Adding service child
    YamlField serviceField = ctx.getCurrentField().getNode().getField("spec").getNode().getField("service");

    PlanNode servicePlanNode = ServicePMSPlanCreator.createPlanForServiceNode(
        serviceField, ((DeploymentStageConfig) field.getStageType()).getService(), kryoSerializer);
    planCreationResponseMap.put(serviceField.getNode().getUuid(),
        PlanCreationResponse.builder().node(serviceField.getNode().getUuid(), servicePlanNode).build());

    // Adding infrastructure node
    String infraDefNodeUuid = ctx.getCurrentField()
                                  .getNode()
                                  .getField("spec")
                                  .getNode()
                                  .getField("infrastructure")
                                  .getNode()
                                  .getField("infrastructureDefinition")
                                  .getNode()
                                  .getUuid();
    YamlField infraField = ctx.getCurrentField().getNode().getField("spec").getNode().getField("infrastructure");
    YamlNode infraNode = infraField.getNode();

    PlanNode infraStepNode = InfrastructurePmsPlanCreator.getInfraStepPlanNode(
        infraDefNodeUuid, ((DeploymentStageConfig) field.getStageType()).getInfrastructure(), infraField);
    planCreationResponseMap.put(
        infraDefNodeUuid, PlanCreationResponse.builder().node(infraDefNodeUuid, infraStepNode).build());

    PlanNode infraSectionPlanNode =
        InfrastructurePmsPlanCreator.getInfraSectionPlanNode(infraNode, infraStepNode.getUuid(),
            ((DeploymentStageConfig) field.getStageType()).getInfrastructure(), kryoSerializer, infraField);
    planCreationResponseMap.put(
        infraNode.getUuid(), PlanCreationResponse.builder().node(infraNode.getUuid(), infraSectionPlanNode).build());

    // Add dependency for execution
    YamlField executionField = ctx.getCurrentField().getNode().getField("spec").getNode().getField("execution");
    dependenciesNodeMap.put(executionField.getNode().getUuid(), executionField);

    planCreationResponseMap.put(
        executionField.getNode().getUuid(), PlanCreationResponse.builder().dependencies(dependenciesNodeMap).build());
    return planCreationResponseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, StageElementConfig config, List<String> childrenNodeIds) {
    StepParameters stepParameters = DeploymentStageStepParameters.getStepParameters(config, childrenNodeIds.get(0));
    return PlanNode.builder()
        .uuid(config.getUuid())
        .name(config.getName())
        .identifier(config.getIdentifier())
        .group(StepOutcomeGroup.STAGE.name())
        .stepParameters(stepParameters)
        .stepType(DeploymentStageStep.STEP_TYPE)
        .facilitatorObtainment(FacilitatorObtainment.newBuilder().setType(ChildFacilitator.FACILITATOR_TYPE).build())
        .adviserObtainments(getAdviserObtainmentFromMetaData(ctx.getCurrentField()))
        .build();
  }

  private List<AdviserObtainment> getAdviserObtainmentFromMetaData(YamlField currentField) {
    List<AdviserObtainment> adviserObtainments = new ArrayList<>();
    if (currentField != null && currentField.getNode() != null) {
      if (checkIfParentIsParallel(currentField)) {
        return adviserObtainments;
      }
      YamlField siblingField =
          currentField.getNode().nextSiblingFromParentArray(currentField.getName(), Arrays.asList("stage", "parallel"));
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

  @Override
  public Class<StageElementConfig> getFieldClass() {
    return StageElementConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("stage", Collections.singleton("Deployment"));
  }

  private boolean checkIfParentIsParallel(YamlField currentField) {
    return YamlUtils.getGivenYamlNodeFromParentPath(currentField.getNode(), "parallel") != null;
  }
}
