package io.harness.cvng.core.beans;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.cvng.beans.TimeSeriesCustomThresholdActions;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.beans.TimeSeriesThresholdActionType;
import io.harness.cvng.beans.TimeSeriesThresholdComparisonType;
import io.harness.cvng.beans.TimeSeriesThresholdType;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TimeSeriesMetricDefinition {
  String metricName;
  TimeSeriesMetricType metricType;
  @Default String metricGroupName = "*";
  TimeSeriesThresholdActionType actionType;
  TimeSeriesThresholdComparisonType comparisonType;
  TimeSeriesCustomThresholdActions action;
  Integer occurrenceCount;
  TimeSeriesThresholdType thresholdType;
  Double value;
}
