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
