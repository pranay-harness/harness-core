package io.harness.pms.plan.creation;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import java.util.*;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PlanCreatorUtils {
  public final String ANY_TYPE = "__any__";

  public boolean supportsField(Map<String, Set<String>> supportedTypes, YamlField field) {
    if (EmptyPredicate.isEmpty(supportedTypes)) {
      return false;
    }

    String fieldName = field.getName();
    Set<String> types = supportedTypes.get(fieldName);
    if (EmptyPredicate.isEmpty(types)) {
      return false;
    }

    String fieldType = field.getNode().getType();
    if (EmptyPredicate.isEmpty(fieldType)) {
      fieldType = ANY_TYPE;
    }
    return types.contains(fieldType);
  }

  public YamlField getStageConfig(YamlField yamlField, String stageIdentifier) {
    if (EmptyPredicate.isEmpty(stageIdentifier)) {
      return null;
    }
    if (yamlField.getName().equals(YAMLFieldNameConstants.PIPELINE)
        || yamlField.getName().equals(YAMLFieldNameConstants.STAGES)) {
      return null;
    }
    YamlNode stages = YamlUtils.getGivenYamlNodeFromParentPath(yamlField.getNode(), YAMLFieldNameConstants.STAGES);
    List<YamlField> stageYamlFields = getStageYamlFields(stages);
    for (YamlField stageYamlField : stageYamlFields) {
      if (stageYamlField.getNode().getIdentifier().equals(stageIdentifier)) {
        return stageYamlField;
      }
    }
    return null;
  }

  private List<YamlField> getStageYamlFields(YamlNode stagesYamlNode) {
    List<YamlNode> yamlNodes = Optional.of(stagesYamlNode.asArray()).orElse(Collections.emptyList());
    List<YamlField> stageFields = new LinkedList<>();

    yamlNodes.forEach(yamlNode -> {
      YamlField stageField = yamlNode.getField(YAMLFieldNameConstants.STAGE);
      YamlField parallelStageField = yamlNode.getField(YAMLFieldNameConstants.PARALLEL);
      if (stageField != null) {
        stageFields.add(stageField);
      } else if (parallelStageField != null) {
        stageFields.addAll(getStageYamlFields(parallelStageField.getNode()));
      }
    });
    return stageFields;
  }
}
