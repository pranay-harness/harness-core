package software.wings.metrics;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * Possible types of metrics that can be tracked.
 * Created by mike@ on 4/7/17.
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum MetricType {
  /**
   * Metric that represents any infra statistics like cpu, memory etc
   */
  INFRA(Lists.newArrayList(Threshold.builder()
                               .thresholdType(ThresholdType.ALERT_HIGHER_OR_LOWER)
                               .comparisonType(ThresholdComparisonType.RATIO)
                               .ml(0.25)
                               .build(),
      Threshold.builder()
          .thresholdType(ThresholdType.ALERT_HIGHER_OR_LOWER)
          .comparisonType(ThresholdComparisonType.DELTA)
          .ml(25)
          .build())),

  /**
   * Metric that represents any observation between 0 and 1 with lower being bad
   * EXAMPLE : APDEX
   */
  VALUE(Lists.newArrayList(Threshold.builder()
                               .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
                               .comparisonType(ThresholdComparisonType.RATIO)
                               .ml(0.5)
                               .build(),
      Threshold.builder()
          .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
          .comparisonType(ThresholdComparisonType.DELTA)
          .ml(0.3)
          .build())),

  /**
   * Metric that measure time
   */
  RESP_TIME(Lists.newArrayList(Threshold.builder()
                                   .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                                   .comparisonType(ThresholdComparisonType.RATIO)
                                   .ml(0.5)
                                   .build(),
      Threshold.builder()
          .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
          .comparisonType(ThresholdComparisonType.DELTA)
          .ml(50)
          .build())),

  /**
   * Metric that count invocations
   */
  THROUGHPUT(Lists.newArrayList(Threshold.builder()
                                    .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
                                    .comparisonType(ThresholdComparisonType.RATIO)
                                    .ml(0.5)
                                    .build(),
      Threshold.builder()
          .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
          .comparisonType(ThresholdComparisonType.DELTA)
          .ml(20)
          .build())),

  /**
   * Metric that count error
   */
  ERROR(Lists.newArrayList(Threshold.builder()
                               .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                               .comparisonType(ThresholdComparisonType.RATIO)
                               .ml(0.01)
                               .build(),
      Threshold.builder()
          .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
          .comparisonType(ThresholdComparisonType.DELTA)
          .ml(0.01)
          .build()));

  private List<Threshold> thresholds;

  MetricType(List<Threshold> thresholds) {
    this.thresholds = thresholds;
  }

  @JsonIgnore
  public List<Threshold> getThresholds() {
    return thresholds;
  }
}
