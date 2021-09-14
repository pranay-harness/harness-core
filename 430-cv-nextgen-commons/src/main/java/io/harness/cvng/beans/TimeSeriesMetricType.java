/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans;

import com.google.common.collect.Lists;
import java.util.List;

public enum TimeSeriesMetricType {
  INFRA("INFRA",
      Lists.newArrayList(
          TimeSeriesThresholdCriteria.builder().criteria("> 0.2").type(TimeSeriesThresholdComparisonType.RATIO).build(),
          TimeSeriesThresholdCriteria.builder().criteria("< 0.2").type(TimeSeriesThresholdComparisonType.RATIO).build(),
          TimeSeriesThresholdCriteria.builder().criteria("> 20").type(TimeSeriesThresholdComparisonType.DELTA).build(),
          TimeSeriesThresholdCriteria.builder()
              .criteria("< 20")
              .type(TimeSeriesThresholdComparisonType.DELTA)
              .build())),

  RESP_TIME("RESP_TIME",
      Lists.newArrayList(
          TimeSeriesThresholdCriteria.builder().criteria("> 0.2").type(TimeSeriesThresholdComparisonType.RATIO).build(),
          TimeSeriesThresholdCriteria.builder()
              .criteria("> 20")
              .type(TimeSeriesThresholdComparisonType.DELTA)
              .build())),

  THROUGHPUT("THROUGHPUT",
      Lists.newArrayList(
          TimeSeriesThresholdCriteria.builder().criteria("< 0.2").type(TimeSeriesThresholdComparisonType.RATIO).build(),
          TimeSeriesThresholdCriteria.builder()
              .criteria("< 20")
              .type(TimeSeriesThresholdComparisonType.DELTA)
              .build())),

  ERROR("ERROR",
      Lists.newArrayList(TimeSeriesThresholdCriteria.builder()
                             .criteria("> 0.01")
                             .type(TimeSeriesThresholdComparisonType.RATIO)
                             .build(),
          TimeSeriesThresholdCriteria.builder()
              .criteria("> 0.01")
              .type(TimeSeriesThresholdComparisonType.DELTA)
              .build())),

  APDEX("APDEX",
      Lists.newArrayList(
          TimeSeriesThresholdCriteria.builder().criteria("< 0.2").type(TimeSeriesThresholdComparisonType.RATIO).build(),
          TimeSeriesThresholdCriteria.builder()
              .criteria("< 0.01")
              .type(TimeSeriesThresholdComparisonType.DELTA)
              .build())),

  OTHER("OTHER",
      Lists.newArrayList(
          TimeSeriesThresholdCriteria.builder().criteria("> 0.2").type(TimeSeriesThresholdComparisonType.RATIO).build(),
          TimeSeriesThresholdCriteria.builder()
              .criteria("> 0.01")
              .type(TimeSeriesThresholdComparisonType.DELTA)
              .build(),
          TimeSeriesThresholdCriteria.builder().criteria("> 20").type(TimeSeriesThresholdComparisonType.DELTA).build(),
          TimeSeriesThresholdCriteria.builder()
              .criteria("< 20")
              .type(TimeSeriesThresholdComparisonType.DELTA)
              .build()));

  private String name;
  private List<TimeSeriesThresholdCriteria> thresholds;

  TimeSeriesMetricType(String name, List<TimeSeriesThresholdCriteria> thresholds) {
    this.name = name;
    this.thresholds = thresholds;
  }

  public List<TimeSeriesThresholdCriteria> getThresholds() {
    return thresholds;
  }
}
