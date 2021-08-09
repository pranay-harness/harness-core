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
