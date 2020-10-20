package io.harness.pipeline.plan.scratch.lib.creator;

import com.google.common.base.Preconditions;

import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.pipeline.plan.scratch.common.creator.PlanCreationResponse;
import io.harness.pipeline.plan.scratch.lib.io.MapStepParameters;
import io.harness.pipeline.plan.scratch.common.yaml.YamlField;
import io.harness.pipeline.plan.scratch.common.yaml.YamlNode;
import io.harness.plan.PlanNode;
import io.harness.state.StepType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PipelinePlanCreator extends ParallelChildrenPlanCreator {
  @Override
  public boolean supportsField(YamlField field) {
    return "pipeline".equals(field.getName());
  }

  @Override
  public StepType getStepType() {
    return StepType.builder().type("pipeline").build();
  }

  @Override
  public boolean isStartingNode() {
    return true;
  }

  @Override
  public Map<String, PlanCreationResponse> createPlanForChildrenNodes(YamlField field) {
    YamlNode yamlNode = field.getNode();
    Map<String, PlanCreationResponse> responseMap = new HashMap<>();
    YamlNode stagesYanlNode = Preconditions.checkNotNull(yamlNode.getField("stages")).getNode();
    if (stagesYanlNode == null) {
      return responseMap;
    }

    List<YamlField> stageYamlFields = Optional.of(stagesYanlNode.asArray())
                                          .orElse(Collections.emptyList())
                                          .stream()
                                          .map(el -> el.getField("stage"))
                                          .collect(Collectors.toList());
    String uuid = "stages-" + yamlNode.getUuid();
    PlanNode node =
        PlanNode.builder()
            .uuid(uuid)
            .identifier("stages-" + yamlNode.getIdentifier())
            .stepType(StepType.builder().type("steps").build())
            .name("stages")
            .stepParameters(new MapStepParameters("childrenNodeIds",
                stageYamlFields.stream().map(el -> el.getNode().getUuid()).collect(Collectors.toList())))
            .facilitatorObtainment(
                FacilitatorObtainment.builder().type(FacilitatorType.builder().type("CHILDREN").build()).build())
            .skipExpressionChain(false)
            .build();

    Map<String, YamlField> stageYamlFieldsMap = new HashMap<>();
    stageYamlFields.forEach(stepField -> stageYamlFieldsMap.put(stepField.getNode().getUuid(), stepField));

    Map<String, PlanNode> nodes = new HashMap<>();
    nodes.put(node.getUuid(), node);
    responseMap.put(
        node.getUuid(), PlanCreationResponse.builder().nodes(nodes).dependencies(stageYamlFieldsMap).build());
    return responseMap;
  }
}
