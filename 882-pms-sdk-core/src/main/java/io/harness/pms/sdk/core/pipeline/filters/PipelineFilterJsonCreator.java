package io.harness.pms.sdk.core.pipeline.filters;

import io.harness.pms.pipeline.filter.FilterCreationResponse;
import io.harness.pms.plan.EdgeLayoutList;
import io.harness.pms.plan.GraphLayoutNode;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.yaml.YamlField;

import java.util.*;
import java.util.stream.Collectors;

public class PipelineFilterJsonCreator implements FilterJsonCreator<YamlField> {
  @Override
  public Class<YamlField> getFieldClass() {
    return YamlField.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("pipeline", Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }

  @Override
  public FilterCreationResponse handleNode(YamlField yamlField) {
    YamlField stages = yamlField.getNode().getField("stages");

    FilterCreationResponse creationResponse = FilterCreationResponse.builder().build();
    if (stages == null) {
      return creationResponse;
    }

    // ToDo Add support for parallel stages
    List<YamlField> stageYamlFields = Optional.of(stages.getNode().asArray())
                                          .orElse(Collections.emptyList())
                                          .stream()
                                          .map(el -> el.getField("stage"))
                                          .filter(Objects::nonNull)
                                          .collect(Collectors.toList());

    Map<String, YamlField> stageYamlFieldMap = new HashMap<>();
    EdgeLayoutList.Builder stagesEdgesBuilder = EdgeLayoutList.newBuilder();
    stageYamlFields.forEach(stageField -> {
      stageYamlFieldMap.put(stageField.getNode().getUuid(), stageField);
      stagesEdgesBuilder.addNextIds(stageField.getNode().getUuid());
    });
    if (!stageYamlFieldMap.isEmpty()) {
      creationResponse.addDependencies(stageYamlFieldMap);
    }

    Map<String, GraphLayoutNode> layoutNodeMap = new HashMap<>();
    GraphLayoutNode pipelineGraph =
        GraphLayoutNode.newBuilder()
            .setNodeType("pipeline")
            .setNodeIdentifier("pipeline")
            .setNodeUUID(yamlField.getNode().getUuid())
            .setEdgeLayoutList(EdgeLayoutList.newBuilder().addNextIds(stages.getNode().getUuid()).build())
            .build();

    GraphLayoutNode stagesGraph = GraphLayoutNode.newBuilder()
                                      .setNodeUUID(stages.getNode().getUuid())
                                      .setNodeType("stages")
                                      .setNodeIdentifier("stages")
                                      .setEdgeLayoutList(stagesEdgesBuilder.build())
                                      .build();
    creationResponse.setStartingNodeId(yamlField.getNode().getUuid());
    layoutNodeMap.put(yamlField.getNode().getUuid(), pipelineGraph);
    layoutNodeMap.put(stages.getNode().getUuid(), stagesGraph);
    creationResponse.addLayoutNodes(layoutNodeMap);
    return creationResponse;
  }
}
