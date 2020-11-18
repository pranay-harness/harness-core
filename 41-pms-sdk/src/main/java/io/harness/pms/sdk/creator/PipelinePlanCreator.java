package io.harness.pms.sdk.creator;

import com.google.common.base.Preconditions;

import io.harness.pms.creator.PlanCreationContext;
import io.harness.pms.creator.PlanCreationResponse;
import io.harness.pms.creator.PlanCreatorUtils;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.plan.PlanNode;
import io.harness.pms.sdk.io.MapStepParameters;
import io.harness.pms.steps.StepType;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PipelinePlanCreator extends ChildrenPlanCreator<YamlField> {
  @Override
  public Class<YamlField> getFieldClass() {
    return YamlField.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("pipeline", Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }

  @Override
  public String getStartingNodeId(YamlField field) {
    return field.getNode().getUuid();
  }

  @Override
  public Map<String, PlanCreationResponse> createPlanForChildrenNodes(PlanCreationContext ctx, YamlField field) {
    YamlNode yamlNode = field.getNode();
    Map<String, PlanCreationResponse> responseMap = new HashMap<>();
    YamlNode stagesYamlNode = Preconditions.checkNotNull(yamlNode.getField("stages")).getNode();
    if (stagesYamlNode == null) {
      return responseMap;
    }

    List<YamlField> stageYamlFields = Optional.of(stagesYamlNode.asArray())
                                          .orElse(Collections.emptyList())
                                          .stream()
                                          .map(el -> el.getField("stage"))
                                          .collect(Collectors.toList());
    String uuid = "stages-" + yamlNode.getUuid();
    PlanNode node =
        PlanNode.newBuilder()
            .setUuid(uuid)
            .setIdentifier("stages-" + yamlNode.getIdentifier())
            .setStepType(StepType.newBuilder().setType("stages").build())
            .setName("stages")
            .setStepParameters(ctx.toByteString(new MapStepParameters("childrenNodeIds",
                stageYamlFields.stream().map(el -> el.getNode().getUuid()).collect(Collectors.toList()))))
            .addFacilitatorObtainments(FacilitatorObtainment.newBuilder()
                                           .setType(FacilitatorType.newBuilder().setType("CHILDREN").build())
                                           .build())
            .setSkipExpressionChain(false)
            .build();

    Map<String, YamlField> stageYamlFieldMap = new HashMap<>();
    stageYamlFields.forEach(stepField -> stageYamlFieldMap.put(stepField.getNode().getUuid(), stepField));
    responseMap.put(node.getUuid(),
        PlanCreationResponse.builder().node(node.getUuid(), node).dependencies(stageYamlFieldMap).build());
    return responseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(PlanCreationContext ctx, YamlField field, Set<String> childrenNodeIds) {
    YamlNode yamlNode = field.getNode();
    return PlanNode.newBuilder()
        .setUuid(yamlNode.getUuid())
        .setIdentifier(yamlNode.getIdentifier())
        .setStepType(StepType.newBuilder().setType("pipeline").build())
        .setName(yamlNode.getNameOrIdentifier())
        .setStepParameters(ctx.toByteString(new MapStepParameters("childrenNodeIds", childrenNodeIds)))
        .addFacilitatorObtainments(FacilitatorObtainment.newBuilder()
                                       .setType(FacilitatorType.newBuilder().setType("CHILD_CHAIN").build())
                                       .build())
        .setSkipExpressionChain(false)
        .build();
  }
}
