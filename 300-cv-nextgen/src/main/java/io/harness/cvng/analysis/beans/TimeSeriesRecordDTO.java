/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
  long epochMinute;
  double metricValue;
}
