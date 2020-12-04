package io.harness.cdng.creator.plan.stage;

import io.harness.cdng.creator.plan.infrastructure.InfrastructurePmsPlanCreator;
import io.harness.cdng.creator.plan.service.ServicePMSPlanCreator;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.pipeline.beans.DeploymentStageStepParameters;
import io.harness.cdng.pipeline.steps.DeploymentStageStep;
import io.harness.facilitator.OrchestrationFacilitatorType;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.YamlField;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DeploymentStagePMSPlanCreator extends ChildrenPlanCreator<StageElementConfig> {
  @Override
  public Map<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, StageElementConfig field) {
    Map<String, PlanCreationResponse> planCreationResponseMap = new HashMap<>();
    Map<String, YamlField> dependenciesNodeMap = new HashMap<>();

    // Adding service child
    String serviceNodeUuid =
        ctx.getCurrentField().getNode().getField("spec").getNode().getField("service").getNode().getUuid();

    PlanNode servicePlanNode = ServicePMSPlanCreator.createPlanForServiceNode(
        serviceNodeUuid, ((DeploymentStageConfig) field.getStageType()).getService());
    planCreationResponseMap.put(
        serviceNodeUuid, PlanCreationResponse.builder().node(serviceNodeUuid, servicePlanNode).build());

    // Adding infrastructure node
    String infraDefNodeUuid = ctx.getCurrentField()
                                  .getNode()
                                  .getField("infrastructure")
                                  .getNode()
                                  .getField("infrastructureDefinition")
                                  .getNode()
                                  .getUuid();
    String infraNodeUuid = ctx.getCurrentField().getNode().getField("infrastructure").getNode().getUuid();

    PlanNode infraStepNode = InfrastructurePmsPlanCreator.getInfraStepPlanNode(
        infraDefNodeUuid, ((DeploymentStageConfig) field.getStageType()).getInfrastructure());
    planCreationResponseMap.put(
        infraDefNodeUuid, PlanCreationResponse.builder().node(infraDefNodeUuid, infraStepNode).build());

    PlanNode infraSectionPlanNode =
        InfrastructurePmsPlanCreator.getInfraSectionPlanNode(infraNodeUuid, infraStepNode.getUuid());
    planCreationResponseMap.put(
        infraNodeUuid, PlanCreationResponse.builder().node(infraNodeUuid, infraSectionPlanNode).build());

    // Add dependency for execution
    YamlField executionField = ctx.getCurrentField().getNode().getField("execution");
    dependenciesNodeMap.put(infraNodeUuid, executionField);

    planCreationResponseMap.put(
        executionField.getNode().getUuid(), PlanCreationResponse.builder().dependencies(dependenciesNodeMap).build());
    return planCreationResponseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, StageElementConfig field, Set<String> childrenNodeIds) {
    StepParameters stepParameters =
        DeploymentStageStepParameters.builder().deploymentStage((DeploymentStage) field.getStageType()).build();
    return PlanNode.builder()
        .uuid(field.getUuid())
        .name(field.getName())
        .identifier(field.getIdentifier())
        .stepParameters(stepParameters)
        .stepType(DeploymentStageStep.STEP_TYPE)
        .facilitatorObtainment(FacilitatorObtainment.newBuilder()
                                   .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD))
                                   .build())
        .build();
  }

  @Override
  public Class<StageElementConfig> getFieldClass() {
    return StageElementConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("stage", Collections.singleton("Deployment"));
  }
}
