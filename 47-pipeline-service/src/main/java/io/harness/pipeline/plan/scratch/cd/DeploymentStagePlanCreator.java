package io.harness.pipeline.plan.scratch.cd;

import com.google.common.base.Preconditions;

import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.pipeline.plan.scratch.lib.creator.ParallelChildrenPlanCreator;
import io.harness.pipeline.plan.scratch.common.creator.PlanCreationResponse;
import io.harness.pipeline.plan.scratch.lib.io.MapStepParameters;
import io.harness.pipeline.plan.scratch.common.yaml.YamlField;
import io.harness.pipeline.plan.scratch.common.yaml.YamlNode;
import io.harness.plan.PlanNode;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DeploymentStagePlanCreator extends ParallelChildrenPlanCreator {
  @Override
  public boolean supportsField(YamlField field) {
    return "deployment".equals(field.getNode().getType());
  }

  @Override
  public StepType getStepType() {
    return StepType.builder().type("stage").build();
  }

  @Override
  public String getGroup() {
    return "stage";
  }

  @Override
  public Map<String, PlanCreationResponse> createPlanForChildrenNodes(YamlField field) {
    YamlNode yamlNode = field.getNode();
    YamlNode specYamlNode = Preconditions.checkNotNull(yamlNode.getField("spec")).getNode();
    Map<String, PlanCreationResponse> responseMap = new HashMap<>();
    createPlanNodeForService(responseMap, specYamlNode);
    createPlanNodeForInfrastructure(responseMap, specYamlNode);
    createPlanNodeForSteps(responseMap, specYamlNode);
    return responseMap;
  }

  private void createPlanNodeForService(Map<String, PlanCreationResponse> responseMap, YamlNode yamlNode) {
    YamlNode serviceYamlNode = Preconditions.checkNotNull(yamlNode.getField("service")).getNode();
    if (serviceYamlNode == null) {
      return;
    }

    YamlNode infraSpecYamlNode = Preconditions.checkNotNull(serviceYamlNode.getField("serviceDefinition")).getNode();
    createSyncPlanNode(responseMap, "service", serviceYamlNode,
        new MapStepParameters("serviceDefinition", infraSpecYamlNode.toString()));
  }

  private void createPlanNodeForInfrastructure(Map<String, PlanCreationResponse> responseMap, YamlNode yamlNode) {
    YamlNode infraYamlNode = Preconditions.checkNotNull(yamlNode.getField("infrastructure")).getNode();
    if (infraYamlNode == null) {
      return;
    }

    MapStepParameters stepParameters = new MapStepParameters();
    String envBlob = Preconditions.checkNotNull(infraYamlNode.getField("environment")).getNode().toString();
    stepParameters.put("environment", envBlob);
    YamlNode infraDefYamlNode =
        Preconditions.checkNotNull(infraYamlNode.getField("infrastructureDefinition")).getNode();
    YamlNode infraDefSpecYamlNode = Preconditions.checkNotNull(infraDefYamlNode.getField("spec")).getNode();
    stepParameters.put("spec", infraDefSpecYamlNode.toString());
    createSyncPlanNode(responseMap, "infrastructure", infraDefYamlNode, stepParameters);
  }

  private void createSyncPlanNode(
      Map<String, PlanCreationResponse> responseMap, String type, YamlNode yamlNode, StepParameters stepParameters) {
    PlanNode node =
        PlanNode.builder()
            .uuid(yamlNode.getUuid())
            .identifier(yamlNode.getIdentifier())
            .stepType(StepType.builder().type(type).build())
            .name(type)
            .stepParameters(stepParameters)
            .facilitatorObtainment(
                FacilitatorObtainment.builder().type(FacilitatorType.builder().type("SYNC").build()).build())
            .skipExpressionChain(false)
            .build();
    Map<String, PlanNode> nodes = new HashMap<>();
    nodes.put(node.getUuid(), node);
    responseMap.put(node.getUuid(), PlanCreationResponse.builder().nodes(nodes).build());
  }

  private void createPlanNodeForSteps(Map<String, PlanCreationResponse> responseMap, YamlNode yamlNode) {
    YamlNode executionYamlNode = Preconditions.checkNotNull(yamlNode.getField("execution")).getNode();
    if (executionYamlNode == null) {
      return;
    }

    YamlNode stepsYamlNode = Preconditions.checkNotNull(executionYamlNode.getField("steps")).getNode();
    if (stepsYamlNode == null) {
      return;
    }

    List<YamlField> stepYamlFields = Optional.of(stepsYamlNode.asArray())
                                         .orElse(Collections.emptyList())
                                         .stream()
                                         .map(el -> el.getField("step"))
                                         .collect(Collectors.toList());
    String uuid = "steps-" + yamlNode.getUuid();
    PlanNode node =
        PlanNode.builder()
            .uuid(uuid)
            .identifier("steps-" + stepsYamlNode.getIdentifier())
            .stepType(StepType.builder().type("steps").build())
            .name("steps")
            .stepParameters(new MapStepParameters("childrenNodeIds",
                stepYamlFields.stream().map(el -> el.getNode().getUuid()).collect(Collectors.toList())))
            .facilitatorObtainment(
                FacilitatorObtainment.builder().type(FacilitatorType.builder().type("CHILDREN").build()).build())
            .skipExpressionChain(false)
            .build();

    Map<String, YamlField> stepYamlFieldsMap = new HashMap<>();
    stepYamlFields.forEach(stepField -> stepYamlFieldsMap.put(stepField.getNode().getUuid(), stepField));

    Map<String, PlanNode> nodes = new HashMap<>();
    nodes.put(node.getUuid(), node);
    responseMap.put(
        node.getUuid(), PlanCreationResponse.builder().nodes(nodes).dependencies(stepYamlFieldsMap).build());
  }
}
