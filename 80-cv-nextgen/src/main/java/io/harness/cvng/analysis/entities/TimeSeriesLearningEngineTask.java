package io.harness.cvng.analysis.entities;

import static io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType.SERVICE_GUARD_TIME_SERIES;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesLearningEngineTask extends LearningEngineTask {
  private String testDataUrl;
  private String previousAnalysisUrl;
  private String metricTemplateUrl;
  private String previousAnomaliesUrl;
  private String cumulativeSumsUrl;
  private Set<String> keyTransactions = new HashSet<>();
  private int tolerance;
  @Builder.Default private int dataLength = 5;
  @Builder.Default private int windowSize = 5;

  @Override
  public LearningEngineTaskType getType() {
    return SERVICE_GUARD_TIME_SERIES;
  }
}
