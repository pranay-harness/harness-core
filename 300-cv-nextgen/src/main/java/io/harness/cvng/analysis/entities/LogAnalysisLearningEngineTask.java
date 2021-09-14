/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public abstract class LogAnalysisLearningEngineTask extends LearningEngineTask {
  private String controlDataUrl;
  private String testDataUrl;
  private String previousAnalysisUrl;
}
