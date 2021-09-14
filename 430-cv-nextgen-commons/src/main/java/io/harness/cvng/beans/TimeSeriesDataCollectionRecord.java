/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
public class TimeSeriesDataCollectionRecord {
  private String accountId;
  private String cvConfigId;
  private String verificationTaskId;
  private String host;
  private long timeStamp;
  private Set<TimeSeriesDataRecordMetricValue> metricValues;

  @Data
  @Builder
  @EqualsAndHashCode(of = "metricName")
  public static class TimeSeriesDataRecordMetricValue {
    private String metricName;
    private Set<TimeSeriesDataRecordGroupValue> timeSeriesValues;
  }

  @Data
  @Builder
  @EqualsAndHashCode(of = "groupName")
  public static class TimeSeriesDataRecordGroupValue {
    private String groupName;
    private double value;
    private Double percent;
  }
}
