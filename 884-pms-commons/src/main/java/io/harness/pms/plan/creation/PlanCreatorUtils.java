package io.harness.pms.plan.creation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.yaml.PmsYamlUtils;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
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
    YamlNode stages = PmsYamlUtils.getGivenYamlNodeFromParentPath(yamlField.getNode(), YAMLFieldNameConstants.STAGES);
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

  public boolean checkIfStageRollbackStepsPresent(YamlNode executionNode) {
    if (executionNode == null) {
      return false;
    }
    YamlField rollbackStepsField = executionNode.getField(YAMLFieldNameConstants.ROLLBACK_STEPS);
    return rollbackStepsField != null && rollbackStepsField.getNode().asArray().size() != 0;
  }

  /**
   * @param executionNode execution element.
   * @return boolean
   */
  public boolean checkIfAnyStepGroupRollback(YamlNode executionNode) {
    if (executionNode == null) {
      return false;
    }
    YamlField executionStepsField = executionNode.getField(YAMLFieldNameConstants.STEPS);
    List<YamlNode> stepsArrayFields = executionStepsField.getNode().asArray();
    for (int i = stepsArrayFields.size() - 1; i >= 0; i--) {
      List<YamlField> yamlFields = stepsArrayFields.get(i).fields();
      for (YamlField yamlField : yamlFields) {
        if (yamlField.getName().equals(YAMLFieldNameConstants.STEP_GROUP)) {
          YamlField rollbackStepsNode = yamlField.getNode().getField(YAMLFieldNameConstants.ROLLBACK_STEPS);
          if (rollbackStepsNode != null) {
            return true;
          }
        } else if (yamlField.getName().equals(YAMLFieldNameConstants.PARALLEL)) {
          List<YamlField> stepGroupFields = getStepGroupInParallelSectionHavingRollback(yamlField);
          if (EmptyPredicate.isEmpty(stepGroupFields)) {
            return false;
          }
          for (YamlField stepGroupField : stepGroupFields) {
            YamlField rollbackStepsNode = stepGroupField.getNode().getField(YAMLFieldNameConstants.ROLLBACK_STEPS);
            if (rollbackStepsNode != null) {
              return true;
            }
          }
        }
      }
    }

    return false;
  }

  public List<YamlField> getStepGroupInParallelSectionHavingRollback(YamlField parallelStepGroup) {
    List<YamlNode> yamlNodes =
        Optional.of(Preconditions.checkNotNull(parallelStepGroup).getNode().asArray()).orElse(Collections.emptyList());
    List<YamlField> stepGroupFields = new LinkedList<>();
    yamlNodes.forEach(yamlNode -> {
      YamlField stepGroupField = yamlNode.getField(YAMLFieldNameConstants.STEP_GROUP);
      if (stepGroupField != null && stepGroupField.getNode().getField(YAMLFieldNameConstants.ROLLBACK_STEPS) != null) {
        stepGroupFields.add(stepGroupField);
      }
    });
    return stepGroupFields;
  }

  public List<YamlField> getStepYamlFields(List<YamlNode> stepYamlNodes) {
    List<YamlField> stepFields = new LinkedList<>();

    stepYamlNodes.forEach(yamlNode -> {
      YamlField stepField = yamlNode.getField(YAMLFieldNameConstants.STEP);
      YamlField stepGroupField = yamlNode.getField(YAMLFieldNameConstants.STEP_GROUP);
      YamlField parallelStepField = yamlNode.getField(YAMLFieldNameConstants.PARALLEL);
      if (stepField != null) {
        stepFields.add(stepField);
      } else if (stepGroupField != null) {
        stepFields.add(stepGroupField);
      } else if (parallelStepField != null) {
        stepFields.add(parallelStepField);
      }
    });
    return stepFields;
  }

  public List<YamlField> getDependencyNodeIdsForParallelNode(YamlField parallelYamlField) {
    List<YamlField> childYamlFields = getStageChildFields(parallelYamlField);
    if (childYamlFields.isEmpty()) {
      List<YamlNode> yamlNodes = Optional.of(parallelYamlField.getNode().asArray()).orElse(Collections.emptyList());

      yamlNodes.forEach(yamlNode -> {
        YamlField stageField = yamlNode.getField(YAMLFieldNameConstants.STEP);
        YamlField stepGroupField = yamlNode.getField(YAMLFieldNameConstants.STEP_GROUP);
        if (stageField != null) {
          childYamlFields.add(stageField);
        } else if (stepGroupField != null) {
          childYamlFields.add(stepGroupField);
        }
      });
    }
    return childYamlFields;
  }

  public List<YamlField> getStageChildFields(YamlField parallelYamlField) {
    return Optional.of(parallelYamlField.getNode().asArray())
        .orElse(Collections.emptyList())
        .stream()
        .map(el -> el.getField(YAMLFieldNameConstants.STAGE))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }
}
