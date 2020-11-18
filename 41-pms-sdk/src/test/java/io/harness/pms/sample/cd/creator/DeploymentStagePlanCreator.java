package io.harness.pms.sample.cd.creator;

import com.google.common.base.Preconditions;

import com.fasterxml.jackson.databind.JsonNode;
import io.harness.pms.creator.PlanCreationContext;
import io.harness.pms.creator.PlanCreationResponse;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.plan.PlanNode;
import io.harness.pms.sample.cd.beans.DeploymentStage;
import io.harness.pms.sample.cd.beans.DeploymentStageSpec;
import io.harness.pms.sample.cd.beans.Environment;
import io.harness.pms.sample.cd.beans.Execution;
import io.harness.pms.sample.cd.beans.Infrastructure;
import io.harness.pms.sample.cd.beans.InfrastructureDefinition;
import io.harness.pms.sample.cd.beans.Service;
import io.harness.pms.sample.cd.beans.ServiceDefinition;
import io.harness.pms.sdk.creator.ChildrenPlanCreator;
import io.harness.pms.sdk.io.MapStepParameters;
import io.harness.pms.steps.StepType;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DeploymentStagePlanCreator extends ChildrenPlanCreator<DeploymentStage> {
  @Override
  public Class<DeploymentStage> getFieldClass() {
    return DeploymentStage.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("stage", Collections.singleton("deployment"));
  }

  @Override
  public Map<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, DeploymentStage deploymentStage) {
    DeploymentStageSpec spec = Preconditions.checkNotNull(deploymentStage.getSpec());
    Map<String, PlanCreationResponse> responseMap = new HashMap<>();
    createPlanNodeForService(responseMap, ctx, spec.getService());
    createPlanNodeForInfrastructure(responseMap, ctx, spec.getInfrastructure());
    createPlanNodeForSteps(responseMap, ctx, spec.getExecution());
    return responseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, DeploymentStage deploymentStage, Set<String> childrenNodeIds) {
    return PlanNode.newBuilder()
        .setUuid(deploymentStage.getUuid())
        .setIdentifier(deploymentStage.getIdentifier())
        .setStepType(StepType.newBuilder().setType("stage").build())
        .setGroup("stage")
        .setName(deploymentStage.getIdentifier())
        .setStepParameters(ctx.toByteString(new MapStepParameters("childrenNodeIds", childrenNodeIds)))
        .addFacilitatorObtainments(FacilitatorObtainment.newBuilder()
                                       .setType(FacilitatorType.newBuilder().setType("CHILDREN").build())
                                       .build())
        .setSkipExpressionChain(false)
        .build();
  }

  private void createPlanNodeForService(
      Map<String, PlanCreationResponse> responseMap, PlanCreationContext ctx, Service service) {
    if (service == null) {
      return;
    }

    ServiceDefinition definition = Preconditions.checkNotNull(service.getServiceDefinition());
    createSyncPlanNode(responseMap, ctx, "service", service.getUuid(), service.getIdentifier(),
        new MapStepParameters("serviceDefinition", definition.getSpec()));
  }

  private void createPlanNodeForInfrastructure(
      Map<String, PlanCreationResponse> responseMap, PlanCreationContext ctx, Infrastructure infrastructure) {
    if (infrastructure == null) {
      return;
    }

    MapStepParameters stepParameters = new MapStepParameters();
    Environment environment = Preconditions.checkNotNull(infrastructure.getEnvironment());
    stepParameters.put("environmentName", environment.getName());
    InfrastructureDefinition infrastructureDefinition = infrastructure.getInfrastructureDefinition();
    stepParameters.put("infrastructureDefinition", infrastructureDefinition.getSpec());
    createSyncPlanNode(
        responseMap, ctx, "infrastructure", infrastructureDefinition.getUuid(), "infrastructure", stepParameters);
  }

  private void createSyncPlanNode(Map<String, PlanCreationResponse> responseMap, PlanCreationContext ctx, String type,
      String uuid, String identifier, Map<String, Object> stepParameters) {
    PlanNode node = PlanNode.newBuilder()
                        .setUuid(uuid)
                        .setIdentifier(identifier)
                        .setStepType(StepType.newBuilder().setType(type).build())
                        .setName(type)
                        .setStepParameters(ctx.toByteString(stepParameters))
                        .addFacilitatorObtainments(FacilitatorObtainment.newBuilder()
                                                       .setType(FacilitatorType.newBuilder().setType("SYNC").build())
                                                       .build())
                        .setSkipExpressionChain(false)
                        .build();
    responseMap.put(node.getUuid(), PlanCreationResponse.builder().node(node.getUuid(), node).build());
  }

  private void createPlanNodeForSteps(
      Map<String, PlanCreationResponse> responseMap, PlanCreationContext ctx, Execution execution) {
    if (execution == null) {
      return;
    }

    List<JsonNode> steps = Preconditions.checkNotNull(execution.getSteps());
    List<YamlField> stepYamlFields =
        steps.stream().map(el -> new YamlField("step", new YamlNode(el.get("step")))).collect(Collectors.toList());
    String uuid = "steps-" + execution.getUuid();
    PlanNode node =
        PlanNode.newBuilder()
            .setUuid(uuid)
            .setIdentifier("steps")
            .setStepType(StepType.newBuilder().setType("steps").build())
            .setName("steps")
            .setStepParameters(ctx.toByteString(new MapStepParameters("childrenNodeIds",
                stepYamlFields.stream().map(el -> el.getNode().getUuid()).collect(Collectors.toList()))))
            .addFacilitatorObtainments(FacilitatorObtainment.newBuilder()
                                           .setType(FacilitatorType.newBuilder().setType("CHILDREN").build())
                                           .build())
            .setSkipExpressionChain(false)
            .build();

    Map<String, YamlField> stepYamlFieldMap = new HashMap<>();
    stepYamlFields.forEach(stepField -> stepYamlFieldMap.put(stepField.getNode().getUuid(), stepField));
    responseMap.put(node.getUuid(),
        PlanCreationResponse.builder().node(node.getUuid(), node).dependencies(stepYamlFieldMap).build());
  }
}
