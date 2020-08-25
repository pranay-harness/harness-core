package io.harness.cvng.analysis.beans;

import lombok.Builder;
import lombok.Value;
@Value
@Builder
public class TimeSeriesRecordDTO {
  String verificationTaskId;
  String host;
  String metricName;
  String groupName;
  long timestamp;
  double metricValue;
}
