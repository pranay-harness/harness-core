package io.harness.pms.sdk.core.pipeline.filters;

import io.harness.pms.contracts.plan.EdgeLayoutList;
import io.harness.pms.contracts.plan.GraphLayoutNode;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;

import java.util.*;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StagesFilterJsonCreator {
  public Map<String, YamlField> getDependencies(YamlField stagesYamlNode) {
    Map<String, YamlField> dependencies = new HashMap<>();
    if (stagesYamlNode.getNode() == null) {
      return dependencies;
    }
    List<YamlField> stageYamlFields = getStageYamlFields(stagesYamlNode);
    for (YamlField stageYamlField : stageYamlFields) {
      dependencies.put(stageYamlField.getNode().getUuid(), stageYamlField);
    }
    return dependencies;
  }

  public Map<String, GraphLayoutNode> getStagesGraphLayoutNode(YamlField stagesYamlNode) {
    Map<String, GraphLayoutNode> stageYamlFieldMap = new HashMap<>();
    List<YamlField> stagesYamlField = getStageYamlFields(stagesYamlNode);
    List<EdgeLayoutList> edgeLayoutLists = new ArrayList<>();
    for (YamlField stageYamlField : stagesYamlField) {
      EdgeLayoutList.Builder stageEdgesBuilder = EdgeLayoutList.newBuilder();
      stageEdgesBuilder.addNextIds(stageYamlField.getNode().getUuid());
      edgeLayoutLists.add(stageEdgesBuilder.build());
    }
    stageYamlFieldMap.put(stagesYamlNode.getNode().getUuid(),
        GraphLayoutNode.newBuilder()
            .setNodeUUID(stagesYamlNode.getNode().getUuid())
            .setNodeType("stages")
            .setNodeIdentifier("stages")
            .setEdgeLayoutList(edgeLayoutLists.get(0))
            .build());

    for (int i = 0; i < edgeLayoutLists.size(); i++) {
      YamlField stageYamlField = stagesYamlField.get(i);
      stageYamlFieldMap.put(stageYamlField.getNode().getUuid(),
          GraphLayoutNode.newBuilder()
              .setNodeUUID(stageYamlField.getNode().getUuid())
              .setNodeType("stage")
              .setNodeIdentifier(stageYamlField.getNode().getUuid())
              .setEdgeLayoutList(
                  i + 1 < edgeLayoutLists.size() ? edgeLayoutLists.get(i + 1) : EdgeLayoutList.newBuilder().build())
              .build());
    }
    return stageYamlFieldMap;
  }

  private List<YamlField> getStageYamlFields(YamlField stagesYamlField) {
    List<YamlNode> yamlNodes = Optional.of(stagesYamlField.getNode().asArray()).orElse(Collections.emptyList());
    List<YamlField> stageFields = new LinkedList<>();

    yamlNodes.forEach(yamlNode -> {
      YamlField stageField = yamlNode.getField("stage");
      YamlField parallelStageField = yamlNode.getField("parallel");
      if (stageField != null) {
        stageFields.add(stageField);
      } else if (parallelStageField != null) {
        stageFields.add(parallelStageField);
      }
    });
    return stageFields;
  }
}
