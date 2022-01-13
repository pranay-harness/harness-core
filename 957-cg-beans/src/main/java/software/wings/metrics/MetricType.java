/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.metrics;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import java.util.List;

/**
 * Possible types of metrics that can be tracked.
 * Created by mike@ on 4/7/17.
 */
public enum MetricType {
  /**
   * Metric that represents any infra statistics like cpu, memory etc
   */
  INFRA("INFRA",
      Lists.newArrayList(Threshold.builder()
                             .thresholdType(ThresholdType.ALERT_HIGHER_OR_LOWER)
                             .comparisonType(ThresholdComparisonType.RATIO)
                             .ml(0.2)
                             .build(),
          Threshold.builder()
              .thresholdType(ThresholdType.ALERT_HIGHER_OR_LOWER)
              .comparisonType(ThresholdComparisonType.DELTA)
              .ml(20)
              .build())),

  /**
   * Metric that represents any observation between 0 and 1 with lower being bad
   * EXAMPLE : APDEX
   */
  VALUE("VALUE",
      Lists.newArrayList(Threshold.builder()
                             .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
                             .comparisonType(ThresholdComparisonType.RATIO)
                             .ml(0.2)
                             .build(),
          Threshold.builder()
              .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
              .comparisonType(ThresholdComparisonType.DELTA)
              .ml(0.2)
              .build())),

  /**
   * Metric that measure time
   */
  RESP_TIME("RESP_TIME",
      Lists.newArrayList(Threshold.builder()
                             .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                             .comparisonType(ThresholdComparisonType.RATIO)
                             .ml(0.2)
                             .build(),
          Threshold.builder()
              .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
              .comparisonType(ThresholdComparisonType.DELTA)
              .ml(20)
              .build())),

  /**
   * Metric that count invocations
   */
  THROUGHPUT("THROUGHPUT",
      Lists.newArrayList(Threshold.builder()
                             .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
                             .comparisonType(ThresholdComparisonType.RATIO)
                             .ml(0.2)
                             .build(),
          Threshold.builder()
              .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
              .comparisonType(ThresholdComparisonType.DELTA)
              .ml(20)
              .build())),

  /**
   * Metric that count error
   */
  ERROR("ERROR",
      Lists.newArrayList(Threshold.builder()
                             .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                             .comparisonType(ThresholdComparisonType.RATIO)
                             .ml(0.01)
                             .build(),
          Threshold.builder()
              .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
              .comparisonType(ThresholdComparisonType.DELTA)
              .ml(0.01)
              .build())),
  /**
   * Metric that represents any observation between 0 and 1 with lower being bad
   * EXAMPLE : APDEX
   */
  APDEX("APDEX",
      Lists.newArrayList(Threshold.builder()
                             .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
                             .comparisonType(ThresholdComparisonType.RATIO)
                             .ml(0.2)
                             .build(),
          Threshold.builder()
              .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
              .comparisonType(ThresholdComparisonType.DELTA)
              .ml(0.01)
              .build())),

  /**
   * Metric that represents any observed value with lower being bad
   *
   */
  VALUE_LOWER("VALUE_LOWER",
      Lists.newArrayList(Threshold.builder()
                             .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
                             .comparisonType(ThresholdComparisonType.RATIO)
                             .ml(0.01)
                             .build(),
          Threshold.builder()
              .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
              .comparisonType(ThresholdComparisonType.DELTA)
              .ml(0.01)
              .build()));

  private String name;
  private List<Threshold> thresholds;

  MetricType(String name, List<Threshold> thresholds) {
    this.name = name;
    this.thresholds = thresholds;
  }

  @JsonIgnore
  public List<Threshold> getThresholds() {
    return thresholds;
  }
}
