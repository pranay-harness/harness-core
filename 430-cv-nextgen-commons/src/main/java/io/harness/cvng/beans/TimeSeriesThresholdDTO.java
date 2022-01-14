package io.harness.cvng.beans;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
public class TimeSeriesThresholdDTO {
  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  DataSourceType dataSourceType;
  String metricPackIdentifier;
  String metricName;
  TimeSeriesMetricType metricType;
  @Default String metricGroupName = "*";
  TimeSeriesThresholdActionType action;
  TimeSeriesThresholdCriteria criteria;
}
