package io.harness.cvng.analysis.entities;

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
public class TimeSeriesLoadTestLearningEngineTask extends LearningEngineTask {
  private String testDataUrl;
  private String controlDataUrl;
  private String metricTemplateUrl;

  private int dataLength;
  private int tolerance;
  private Long baselineStartTime;

  @Override
  public LearningEngineTaskType getType() {
    return LearningEngineTaskType.TIME_SERIES_LOAD_TEST;
  }
}
