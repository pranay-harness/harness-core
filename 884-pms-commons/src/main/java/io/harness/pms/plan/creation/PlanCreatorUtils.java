/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.plan.creation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.YamlException;
import io.harness.pms.contracts.plan.YamlUpdates;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import com.google.common.base.Preconditions;
import java.io.IOException;
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
  public final String TEMPLATE_TYPE = "__template__";

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
      if (field.getNode().getTemplate() == null) {
        fieldType = ANY_TYPE;
      } else {
        fieldType = TEMPLATE_TYPE;
      }
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

  public static YamlUpdates.Builder setYamlUpdate(YamlField yamlField, YamlUpdates.Builder yamlUpdates) {
    try {
      return yamlUpdates.putFqnToYaml(yamlField.getYamlPath(), YamlUtils.writeYamlString(yamlField));
    } catch (IOException e) {
      throw new YamlException(
          "Yaml created for yamlField at " + yamlField.getYamlPath() + " could not be converted into a yaml string");
    }
  }
}
