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
