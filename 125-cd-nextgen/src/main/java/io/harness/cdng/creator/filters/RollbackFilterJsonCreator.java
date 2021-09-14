/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.creator.filters;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;

import java.util.LinkedHashMap;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class RollbackFilterJsonCreator {
  public LinkedHashMap<String, YamlField> createPlanForRollback(YamlField executionField) {
    YamlField rollbackStepsField = executionField.getNode().getField(YAMLFieldNameConstants.ROLLBACK_STEPS);

    if (rollbackStepsField == null || rollbackStepsField.getNode().asArray().size() == 0) {
      return new LinkedHashMap<>();
    }
    LinkedHashMap<String, YamlField> responseMap = new LinkedHashMap<>();
    List<YamlField> stepYamlFields = PlanCreatorUtils.getStepYamlFields(rollbackStepsField.getNode().asArray());
    for (YamlField stepYamlField : stepYamlFields) {
      responseMap.put(stepYamlField.getNode().getUuid(), stepYamlField);
    }
    return responseMap;
  }
}
