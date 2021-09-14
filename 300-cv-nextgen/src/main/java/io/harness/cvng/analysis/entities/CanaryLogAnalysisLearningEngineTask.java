/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.entities;

import java.util.Collections;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CanaryLogAnalysisLearningEngineTask extends LogAnalysisLearningEngineTask {
  private Set<String> controlHosts;

  public Set<String> getControlHosts() {
    if (controlHosts == null) {
      return Collections.emptySet();
    }
    return controlHosts;
  }
  @Override
  public LearningEngineTaskType getType() {
    return LearningEngineTaskType.CANARY_LOG_ANALYSIS;
  }
}
