package io.harness.pms.sdk.core.pipeline.filters;

import static io.harness.pms.yaml.YAMLFieldNameConstants.PARALLEL;

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

  public String getStartingNodeId(YamlField stagesYamlField) {
    return getStageYamlFields(stagesYamlField).get(0).getNode().getUuid();
  }

  public int getStagesCount(Collection<YamlField> stagesYamlField) {
    return (int) stagesYamlField.stream().filter(yamlField -> !yamlField.getName().equals(PARALLEL)).count();
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
