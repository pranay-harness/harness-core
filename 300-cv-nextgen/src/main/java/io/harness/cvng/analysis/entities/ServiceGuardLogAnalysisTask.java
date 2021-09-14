/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.entities;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceGuardLogAnalysisTask extends LearningEngineTask {
  private String testDataUrl;
  private String frequencyPatternUrl;
  private boolean isBaselineWindow;

  @Override
  public LearningEngineTaskType getType() {
    return LearningEngineTaskType.SERVICE_GUARD_LOG_ANALYSIS;
  }
}
