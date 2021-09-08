package io.harness.pms.sdk.core.plan.creation.creators;

import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.MapStepParameters;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, YamlField config) {
    YamlNode yamlNode = config.getNode();
    LinkedHashMap<String, PlanCreationResponse> responseMap = new LinkedHashMap<>();
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
    PlanNode node = PlanNode.builder()
                        .uuid(uuid)
                        .identifier("stages-" + yamlNode.getIdentifier())
                        .stepType(StepType.newBuilder().setType("stages").setStepCategory(StepCategory.STAGES).build())
                        .name("stages")
                        .stepParameters(new MapStepParameters("childrenNodeIds",
                            stageYamlFields.stream().map(el -> el.getNode().getUuid()).collect(Collectors.toList())))
                        .facilitatorObtainment(FacilitatorObtainment.newBuilder()
                                                   .setType(FacilitatorType.newBuilder().setType("CHILDREN").build())
                                                   .build())
                        .skipExpressionChain(false)
                        .build();

    Map<String, YamlField> stageYamlFieldMap = new HashMap<>();
    stageYamlFields.forEach(stepField -> stageYamlFieldMap.put(stepField.getNode().getUuid(), stepField));
    responseMap.put(node.getUuid(),
        PlanCreationResponse.builder().node(node.getUuid(), node).dependencies(stageYamlFieldMap).build());
    return responseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(PlanCreationContext ctx, YamlField config, List<String> childrenNodeIds) {
    YamlNode yamlNode = config.getNode();
    return PlanNode.builder()
        .uuid(yamlNode.getUuid())
        .identifier(yamlNode.getIdentifier())
        .stepType(StepType.newBuilder().setType("pipeline").setStepCategory(StepCategory.PIPELINE).build())
        .name(yamlNode.getNameOrIdentifier())
        .stepParameters(new MapStepParameters("childrenNodeIds", childrenNodeIds))
        .facilitatorObtainment(FacilitatorObtainment.newBuilder()
                                   .setType(FacilitatorType.newBuilder().setType("CHILD_CHAIN").build())
                                   .build())
        .skipExpressionChain(false)
        .build();
  }
}
