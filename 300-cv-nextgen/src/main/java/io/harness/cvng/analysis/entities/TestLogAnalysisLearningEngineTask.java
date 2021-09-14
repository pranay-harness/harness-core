/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.entities;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TestLogAnalysisLearningEngineTask extends LogAnalysisLearningEngineTask {
  @Override
  public LearningEngineTaskType getType() {
    return LearningEngineTaskType.TEST_LOG_ANALYSIS;
  }
}
