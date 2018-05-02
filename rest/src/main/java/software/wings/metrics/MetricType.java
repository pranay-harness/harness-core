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
   * Metric that represents any observation
   */
  INFRA(Lists.newArrayList(Threshold.builder()
                               .thresholdType(ThresholdType.ALERT_HIGHER_OR_LOWER)
                               .comparisonType(ThresholdComparisonType.RATIO)
                               .high(0.5)
                               .medium(0.75)
                               .min(0.25)
                               .build(),
      Threshold.builder()
          .thresholdType(ThresholdType.ALERT_HIGHER_OR_LOWER)
          .comparisonType(ThresholdComparisonType.DELTA)
          .high(0.5)
          .medium(0.3)
          .min(0)
          .build())),

  /**
   * Metric that represents any observation
   */
  VALUE(Lists.newArrayList(Threshold.builder()
                               .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
                               .comparisonType(ThresholdComparisonType.RATIO)
                               .high(0.5)
                               .medium(0.75)
                               .min(0.5)
                               .build(),
      Threshold.builder()
          .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
          .comparisonType(ThresholdComparisonType.DELTA)
          .high(0.5)
          .medium(0.3)
          .min(0.3)
          .build())),

  /**
   * Metric that measure time
   */
  RESP_TIME(Lists.newArrayList(Threshold.builder()
                                   .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                                   .comparisonType(ThresholdComparisonType.RATIO)
                                   .high(1.5)
                                   .medium(1.25)
                                   .min(0.5)
                                   .build(),
      Threshold.builder()
          .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
          .comparisonType(ThresholdComparisonType.DELTA)
          .high(10)
          .medium(5)
          .min(50)
          .build())),

  /**
   * Metric that count invocations
   */
  THROUGHPUT(Lists.newArrayList(Threshold.builder()
                                    .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
                                    .comparisonType(ThresholdComparisonType.RATIO)
                                    .high(0.5)
                                    .medium(0.75)
                                    .min(0.5)
                                    .build(),
      Threshold.builder()
          .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
          .comparisonType(ThresholdComparisonType.DELTA)
          .high(100)
          .medium(50)
          .min(20)
          .build())),

  /**
   * Metric that count error
   */
  ERROR(Lists.newArrayList(Threshold.builder()
                               .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                               .comparisonType(ThresholdComparisonType.RATIO)
                               .high(1.10)
                               .medium(1.05)
                               .min(0.01)
                               .build(),
      Threshold.builder()
          .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
          .comparisonType(ThresholdComparisonType.DELTA)
          .high(2)
          .medium(1)
          .min(0.01)
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
