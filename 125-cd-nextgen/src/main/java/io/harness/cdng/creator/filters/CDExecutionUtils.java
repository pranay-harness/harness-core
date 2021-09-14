/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.creator.filters;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class CDExecutionUtils {
  public Map<String, YamlField> getDependencies(YamlField stageField) {
    // Add dependency for execution
    YamlField executionField =
        stageField.getNode().getField(YamlTypes.SPEC).getNode().getField(YAMLFieldNameConstants.EXECUTION);
    Map<String, YamlField> dependencies = new HashMap<>();
    dependencies.put(executionField.getNode().getUuid(), executionField);
    YamlField rollbackSteps = executionField.getNode().getField(YAMLFieldNameConstants.ROLLBACK_STEPS);
    if (rollbackSteps != null && rollbackSteps.getNode().asArray().size() != 0) {
      dependencies.putAll(RollbackFilterJsonCreator.createPlanForRollback(executionField));
    }
    return dependencies;
  }
}
