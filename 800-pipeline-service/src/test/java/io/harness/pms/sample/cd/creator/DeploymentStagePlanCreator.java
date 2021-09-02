package io.harness.pms.sample.cd.creator;

import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sample.cd.beans.DeploymentStage;
import io.harness.pms.sample.cd.beans.DeploymentStageSpec;
import io.harness.pms.sample.cd.beans.Environment;
import io.harness.pms.sample.cd.beans.Execution;
import io.harness.pms.sample.cd.beans.Infrastructure;
import io.harness.pms.sample.cd.beans.InfrastructureDefinition;
import io.harness.pms.sample.cd.beans.Service;
import io.harness.pms.sample.cd.beans.ServiceDefinition;
import io.harness.pms.sample.steps.InfrastructureStepParameters;
import io.harness.pms.sdk.core.plan.MapStepParameters;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, DeploymentStage config) {
    DeploymentStageSpec spec = Preconditions.checkNotNull(config.getSpec());
    LinkedHashMap<String, PlanCreationResponse> responseMap = new LinkedHashMap<>();
    createPlanNodeForService(responseMap, spec.getService());
    createPlanNodeForInfrastructure(responseMap, spec.getInfrastructure());
    createPlanNodeForSteps(responseMap, spec.getExecution());
    return responseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, DeploymentStage config, List<String> childrenNodeIds) {
    return PlanNode.builder()
        .uuid(config.getUuid())
        .identifier(config.getIdentifier())
        .stepType(StepType.newBuilder().setType("stage").build())
        .group("stage")
        .name(config.getIdentifier())
        .stepParameters(new MapStepParameters("childrenNodeIds", childrenNodeIds))
        .facilitatorObtainment(FacilitatorObtainment.newBuilder()
                                   .setType(FacilitatorType.newBuilder().setType("CHILDREN").build())
                                   .build())
        .skipExpressionChain(false)
        .build();
  }

  private void createPlanNodeForService(Map<String, PlanCreationResponse> responseMap, Service service) {
    if (service == null) {
      return;
    }

    ServiceDefinition definition = Preconditions.checkNotNull(service.getServiceDefinition());
    createSyncPlanNode(responseMap, "service", service.getUuid(), service.getIdentifier(),
        new MapStepParameters("serviceDefinition", definition.getSpec()));
  }

  private void createPlanNodeForInfrastructure(
      Map<String, PlanCreationResponse> responseMap, Infrastructure infrastructure) {
    if (infrastructure == null) {
      return;
    }

    InfrastructureStepParameters stepParameters = InfrastructureStepParameters.builder().build();
    Environment environment = Preconditions.checkNotNull(infrastructure.getEnvironment());
    stepParameters.setEnvironmentName(environment.getName());
    InfrastructureDefinition infrastructureDefinition = infrastructure.getInfrastructureDefinition();
    stepParameters.setTmpBool(infrastructureDefinition.getTmpBool());
    stepParameters.setInfrastructureDefinition(infrastructureDefinition.getSpec());
    createSyncPlanNode(
        responseMap, "infrastructure", infrastructureDefinition.getUuid(), "infrastructure", stepParameters);
  }

  private void createSyncPlanNode(Map<String, PlanCreationResponse> responseMap, String type, String uuid,
      String identifier, StepParameters stepParameters) {
    PlanNode node = PlanNode.builder()
                        .uuid(uuid)
                        .identifier(identifier)
                        .stepType(StepType.newBuilder().setType(type).build())
                        .name(type)
                        .stepParameters(stepParameters)
                        .facilitatorObtainment(FacilitatorObtainment.newBuilder()
                                                   .setType(FacilitatorType.newBuilder().setType("SYNC").build())
                                                   .build())
                        .skipExpressionChain(false)
                        .build();
    Map<String, PlanCreationContextValue> contextValueMap = new HashMap<>();
    contextValueMap.put("dummy", PlanCreationContextValue.newBuilder().setStringValue("dummyValue").build());
    responseMap.put(
        node.getUuid(), PlanCreationResponse.builder().node(node.getUuid(), node).contextMap(contextValueMap).build());
  }

  private void createPlanNodeForSteps(Map<String, PlanCreationResponse> responseMap, Execution execution) {
    if (execution == null) {
      return;
    }

    List<JsonNode> steps = Preconditions.checkNotNull(execution.getSteps());
    List<YamlField> stepYamlFields =
        steps.stream().map(el -> new YamlField("step", new YamlNode(el.get("step")))).collect(Collectors.toList());
    String uuid = "steps-" + execution.getUuid();
    PlanNode node = PlanNode.builder()
                        .uuid(uuid)
                        .identifier("steps")
                        .stepType(StepType.newBuilder().setType("steps").build())
                        .name("steps")
                        .stepParameters(new MapStepParameters("childrenNodeIds",
                            stepYamlFields.stream().map(el -> el.getNode().getUuid()).collect(Collectors.toList())))
                        .facilitatorObtainment(FacilitatorObtainment.newBuilder()
                                                   .setType(FacilitatorType.newBuilder().setType("CHILDREN").build())
                                                   .build())
                        .skipExpressionChain(false)
                        .build();

    Map<String, YamlField> stepYamlFieldMap = new HashMap<>();
    stepYamlFields.forEach(stepField -> stepYamlFieldMap.put(stepField.getNode().getUuid(), stepField));
    responseMap.put(node.getUuid(),
        PlanCreationResponse.builder()
            .node(node.getUuid(), node)
            .dependencies(DependenciesUtils.toDependenciesProto(stepYamlFieldMap))
            .build());
  }
}
