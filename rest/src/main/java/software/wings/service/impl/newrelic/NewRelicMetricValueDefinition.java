package software.wings.service.impl.newrelic;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.math.Stats;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.text.WordUtils;
import software.wings.metrics.MetricType;
import software.wings.metrics.RiskLevel;
import software.wings.metrics.Threshold;
import software.wings.metrics.ThresholdComparisonType;
import software.wings.metrics.ThresholdType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.metrics.appdynamics.AppdynamicsConstants;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysisValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Created by rsingh on 9/6/17.
 */
@Data
@Builder
public class NewRelicMetricValueDefinition {
  public static Map<String, TimeSeriesMetricDefinition> NEW_RELIC_VALUES_TO_ANALYZE = new HashMap<>();

  static {
    List<Threshold> thresholds;

    // requestsPerMinute
    thresholds = new ArrayList<>();
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
                       .comparisonType(ThresholdComparisonType.RATIO)
                       .high(0.5)
                       .medium(0.75)
                       .min(0.5)
                       .build());
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                       .comparisonType(ThresholdComparisonType.DELTA)
                       .high(100)
                       .medium(50)
                       .min(20)
                       .build());
    NEW_RELIC_VALUES_TO_ANALYZE.put("requestsPerMinute",
        TimeSeriesMetricDefinition.builder()
            .metricName("requestsPerMinute")
            .metricType(MetricType.THROUGHPUT)
            .thresholds(thresholds)
            .build());

    // averageResponseTime
    thresholds = new ArrayList<>();
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                       .comparisonType(ThresholdComparisonType.RATIO)
                       .high(1.5)
                       .medium(1.25)
                       .min(0.5)
                       .build());
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                       .comparisonType(ThresholdComparisonType.DELTA)
                       .high(10)
                       .medium(5)
                       .min(50)
                       .build());
    NEW_RELIC_VALUES_TO_ANALYZE.put("averageResponseTime",
        TimeSeriesMetricDefinition.builder()
            .metricName("averageResponseTime")
            .metricType(MetricType.RESP_TIME)
            .thresholds(thresholds)
            .build());

    // error
    thresholds = new ArrayList<>();
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                       .comparisonType(ThresholdComparisonType.RATIO)
                       .high(1.5)
                       .medium(1.25)
                       .min(0.5)
                       .build());
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                       .comparisonType(ThresholdComparisonType.DELTA)
                       .high(10)
                       .medium(5)
                       .min(0)
                       .build());
    NEW_RELIC_VALUES_TO_ANALYZE.put("error",
        TimeSeriesMetricDefinition.builder()
            .metricName("error")
            .metricType(MetricType.ERROR)
            .thresholds(thresholds)
            .build());

    // apdexScore
    thresholds = new ArrayList<>();
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
                       .comparisonType(ThresholdComparisonType.RATIO)
                       .high(0.5)
                       .medium(0.75)
                       .min(0.5)
                       .build());
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                       .comparisonType(ThresholdComparisonType.DELTA)
                       .high(0.5)
                       .medium(0.3)
                       .min(0.3)
                       .build());
    NEW_RELIC_VALUES_TO_ANALYZE.put("apdexScore",
        TimeSeriesMetricDefinition.builder()
            .metricName("apdexScore")
            .metricType(MetricType.VALUE)
            .thresholds(thresholds)
            .build());
  }

  public static Map<String, TimeSeriesMetricDefinition> APP_DYNAMICS_VALUES_TO_ANALYZE = new HashMap<>();
  static {
    List<Threshold> thresholds;

    // 95th percentile response time
    String metricName = AppdynamicsConstants.METRIC_NAMES_TO_VARIABLES.get(AppdynamicsConstants.RESPONSE_TIME_95);
    thresholds = new ArrayList<>();
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                       .comparisonType(ThresholdComparisonType.RATIO)
                       .high(1.5)
                       .medium(1.25)
                       .min(0.5)
                       .build());
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                       .comparisonType(ThresholdComparisonType.DELTA)
                       .high(10)
                       .medium(5)
                       .min(50)
                       .build());
    APP_DYNAMICS_VALUES_TO_ANALYZE.put(metricName,
        TimeSeriesMetricDefinition.builder()
            .metricName(metricName)
            .metricType(MetricType.RESP_TIME)
            .thresholds(thresholds)
            .build());

    // slow calls
    metricName = AppdynamicsConstants.METRIC_NAMES_TO_VARIABLES.get(AppdynamicsConstants.NUMBER_OF_SLOW_CALLS);
    thresholds = new ArrayList<>();
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                       .comparisonType(ThresholdComparisonType.DELTA)
                       .high(100)
                       .medium(50)
                       .min(10)
                       .build());
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                       .comparisonType(ThresholdComparisonType.RATIO)
                       .high(1.5)
                       .medium(1.25)
                       .min(0.5)
                       .build());
    APP_DYNAMICS_VALUES_TO_ANALYZE.put(metricName,
        TimeSeriesMetricDefinition.builder()
            .metricName(metricName)
            .metricType(MetricType.COUNT)
            .thresholds(thresholds)
            .build());

    // error
    metricName = AppdynamicsConstants.METRIC_NAMES_TO_VARIABLES.get(AppdynamicsConstants.ERRORS_PER_MINUTE);
    thresholds = new ArrayList<>();
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                       .comparisonType(ThresholdComparisonType.RATIO)
                       .high(1.5)
                       .medium(1.25)
                       .min(0.5)
                       .build());
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                       .comparisonType(ThresholdComparisonType.DELTA)
                       .high(10)
                       .medium(5)
                       .min(0)
                       .build());
    APP_DYNAMICS_VALUES_TO_ANALYZE.put(metricName,
        TimeSeriesMetricDefinition.builder()
            .metricName(metricName)
            .metricType(MetricType.ERROR)
            .thresholds(thresholds)
            .build());

    // stalls
    metricName = AppdynamicsConstants.METRIC_NAMES_TO_VARIABLES.get(AppdynamicsConstants.STALL_COUNT);
    thresholds = new ArrayList<>();
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                       .comparisonType(ThresholdComparisonType.DELTA)
                       .high(10)
                       .medium(5)
                       .min(10)
                       .build());
    thresholds.add(Threshold.builder()
                       .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                       .comparisonType(ThresholdComparisonType.RATIO)
                       .high(1.5)
                       .medium(1.25)
                       .min(0.5)
                       .build());
    APP_DYNAMICS_VALUES_TO_ANALYZE.put(metricName,
        TimeSeriesMetricDefinition.builder()
            .metricName(metricName)
            .metricType(MetricType.COUNT)
            .thresholds(thresholds)
            .build());
  }

  private String metricName;
  private String metricValueName;
  private List<Threshold> thresholds;

  public NewRelicMetricAnalysisValue analyze(
      List<NewRelicMetricDataRecord> testRecords, List<NewRelicMetricDataRecord> controlRecords) {
    double testValue = getValueForComparison(testRecords);
    double controlValue = getValueForComparison(controlRecords);

    if (controlValue < 0.0 || testValue < 0.0) {
      return NewRelicMetricAnalysisValue.builder()
          .name(metricValueName)
          .riskLevel(RiskLevel.NA)
          .controlValue(controlValue)
          .testValue(testValue)
          .build();
    }

    RiskLevel riskLevel = RiskLevel.HIGH;

    for (Threshold threshold : thresholds) {
      RiskLevel currentRiskLevel = threshold.getRiskLevel(testValue, controlValue);
      if (currentRiskLevel.compareTo(riskLevel) > 0) {
        riskLevel = currentRiskLevel;
      }
    }

    return NewRelicMetricAnalysisValue.builder()
        .riskLevel(riskLevel)
        .name(metricValueName)
        .testValue(testValue)
        .controlValue(controlValue)
        .build();
  }

  private double getValueForComparison(List<NewRelicMetricDataRecord> records) {
    double value;
    if (isEmpty(records)) {
      value = -1;
    } else {
      List<Double> testValues;
      try {
        testValues = parseValuesForAnalysis(records);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      if (testValues.isEmpty()) {
        value = -1;
      } else {
        value = Stats.of(testValues).sum() / testValues.size();
      }
    }
    return value;
  }

  private List<Double> parseValuesForAnalysis(List<NewRelicMetricDataRecord> records)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    List<Double> values = new ArrayList<>();
    Method getValueMethod = NewRelicMetricDataRecord.class.getMethod("get" + WordUtils.capitalize(metricValueName));
    for (NewRelicMetricDataRecord metricDataRecord : records) {
      if (!metricDataRecord.getName().equals(metricName)) {
        continue;
      }
      Double value = (Double) getValueMethod.invoke(metricDataRecord);
      if (value.doubleValue() >= 0.0) {
        values.add(value);
      }
    }

    return values;
  }
}
