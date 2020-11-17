package io.harness.pms.sample.cd.creator;

import com.google.common.base.Preconditions;

import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.plan.PlanNode;
import io.harness.pms.plan.common.creator.PlanCreationContext;
import io.harness.pms.plan.common.creator.PlanCreationResponse;
import io.harness.pms.plan.common.yaml.YamlField;
import io.harness.pms.plan.common.yaml.YamlNode;
import io.harness.pms.sdk.creator.ParallelChildrenPlanCreator;
import io.harness.pms.sdk.io.MapStepParameters;
import io.harness.pms.steps.StepType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DeploymentStagePlanCreator extends ParallelChildrenPlanCreator {
  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("stage", Collections.singleton("deployment"));
  }

  @Override
  public StepType getStepType() {
    return StepType.newBuilder().setType("stage").build();
  }

  @Override
  public String getGroup() {
    return "stage";
  }

  @Override
  public Map<String, PlanCreationResponse> createPlanForChildrenNodes(PlanCreationContext ctx, YamlField field) {
    YamlNode yamlNode = field.getNode();
    YamlNode specYamlNode = Preconditions.checkNotNull(yamlNode.getField("spec")).getNode();
    Map<String, PlanCreationResponse> responseMap = new HashMap<>();
    createPlanNodeForService(responseMap, ctx, specYamlNode);
    createPlanNodeForInfrastructure(responseMap, ctx, specYamlNode);
    createPlanNodeForSteps(responseMap, ctx, specYamlNode);
    return responseMap;
  }

  private void createPlanNodeForService(
      Map<String, PlanCreationResponse> responseMap, PlanCreationContext ctx, YamlNode yamlNode) {
    YamlNode serviceYamlNode = Preconditions.checkNotNull(yamlNode.getField("service")).getNode();
    if (serviceYamlNode == null) {
      return;
    }

    YamlNode infraSpecYamlNode = Preconditions.checkNotNull(serviceYamlNode.getField("serviceDefinition")).getNode();
    createSyncPlanNode(responseMap, ctx, "service", serviceYamlNode,
        new MapStepParameters("serviceDefinition", infraSpecYamlNode.toString()));
  }

  private void createPlanNodeForInfrastructure(
      Map<String, PlanCreationResponse> responseMap, PlanCreationContext ctx, YamlNode yamlNode) {
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
    createSyncPlanNode(responseMap, ctx, "infrastructure", infraDefYamlNode, stepParameters);
  }

  private void createSyncPlanNode(Map<String, PlanCreationResponse> responseMap, PlanCreationContext ctx, String type,
      YamlNode yamlNode, Object stepParameters) {
    PlanNode node = PlanNode.newBuilder()
                        .setUuid(yamlNode.getUuid())
                        .setIdentifier(yamlNode.getIdentifier() == null ? type : yamlNode.getIdentifier())
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
      Map<String, PlanCreationResponse> responseMap, PlanCreationContext ctx, YamlNode yamlNode) {
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
        PlanNode.newBuilder()
            .setUuid(uuid)
            .setIdentifier("steps-" + stepsYamlNode.getIdentifier())
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
