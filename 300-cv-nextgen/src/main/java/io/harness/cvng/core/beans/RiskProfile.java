package io.harness.cvng.core.beans;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.beans.TimeSeriesThresholdType;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RiskProfile {
  private CVMonitoringCategory category;
  private TimeSeriesMetricType metricType;
  List<TimeSeriesThresholdType> thresholdTypes;
}
