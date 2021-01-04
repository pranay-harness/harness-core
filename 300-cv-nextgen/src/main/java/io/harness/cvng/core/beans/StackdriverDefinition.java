package io.harness.cvng.core.beans;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.beans.TimeSeriesThresholdType;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Value
@Builder
public class StackdriverDefinition {
  private String dashboardName;
  private String dashboardPath;
  private String metricName;
  private Object jsonMetricDefinition;
  private List<String> metricTags;
  private RiskProfile riskProfile;
  private boolean isManualQuery;

  @Data
  @Builder
  public static class RiskProfile {
    private CVMonitoringCategory category;
    private TimeSeriesMetricType metricType;
    List<TimeSeriesThresholdType> thresholdTypes;
  }
}
