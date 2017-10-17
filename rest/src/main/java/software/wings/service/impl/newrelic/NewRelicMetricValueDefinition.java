package software.wings.service.impl.newrelic;

import com.google.common.math.Stats;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang.WordUtils;
import software.wings.metrics.RiskLevel;
import software.wings.metrics.Threshold;
import software.wings.metrics.ThresholdComparisonType;
import software.wings.metrics.ThresholdType;
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
  public static Map<String, List<Threshold>> NEW_RELIC_VALUES_TO_ANALYZE = new HashMap<>();

  static {
    // throughput
    NEW_RELIC_VALUES_TO_ANALYZE.put("throughput", new ArrayList<>());
    NEW_RELIC_VALUES_TO_ANALYZE.get("throughput")
        .add(Threshold.builder()
                 .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
                 .comparisonType(ThresholdComparisonType.RATIO)
                 .high(0.5)
                 .medium(0.75)
                 .build());
    NEW_RELIC_VALUES_TO_ANALYZE.get("throughput")
        .add(Threshold.builder()
                 .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                 .comparisonType(ThresholdComparisonType.DELTA)
                 .high(100)
                 .medium(50)
                 .build());

    // averageResponseTime
    NEW_RELIC_VALUES_TO_ANALYZE.put("averageResponseTime", new ArrayList<>());
    NEW_RELIC_VALUES_TO_ANALYZE.get("averageResponseTime")
        .add(Threshold.builder()
                 .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                 .comparisonType(ThresholdComparisonType.RATIO)
                 .high(1.5)
                 .medium(1.25)
                 .build());
    NEW_RELIC_VALUES_TO_ANALYZE.get("averageResponseTime")
        .add(Threshold.builder()
                 .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                 .comparisonType(ThresholdComparisonType.DELTA)
                 .high(10)
                 .medium(5)
                 .build());

    // error
    NEW_RELIC_VALUES_TO_ANALYZE.put("error", new ArrayList<>());
    NEW_RELIC_VALUES_TO_ANALYZE.get("error").add(Threshold.builder()
                                                     .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                                                     .comparisonType(ThresholdComparisonType.RATIO)
                                                     .high(1.5)
                                                     .medium(1.25)
                                                     .build());
    NEW_RELIC_VALUES_TO_ANALYZE.get("error").add(Threshold.builder()
                                                     .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                                                     .comparisonType(ThresholdComparisonType.DELTA)
                                                     .high(10)
                                                     .medium(5)
                                                     .build());

    // apdexScore
    NEW_RELIC_VALUES_TO_ANALYZE.put("apdexScore", new ArrayList<>());
    NEW_RELIC_VALUES_TO_ANALYZE.get("apdexScore")
        .add(Threshold.builder()
                 .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
                 .comparisonType(ThresholdComparisonType.ABSOLUTE)
                 .high(0.5)
                 .medium(0.75)
                 .build());
  }

  public static Map<String, List<Threshold>> APP_DYNAMICS_VALUES_TO_ANALYZE = new HashMap<>();
  static {
    // 95th percentile response time
    String metricName = AppdynamicsConstants.METRIC_NAMES_TO_VARIABLES.get(AppdynamicsConstants.RESPONSE_TIME_95);
    APP_DYNAMICS_VALUES_TO_ANALYZE.put(metricName, new ArrayList<>());
    APP_DYNAMICS_VALUES_TO_ANALYZE.get(metricName)
        .add(Threshold.builder()
                 .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                 .comparisonType(ThresholdComparisonType.RATIO)
                 .high(1.5)
                 .medium(1.25)
                 .build());
    APP_DYNAMICS_VALUES_TO_ANALYZE.get(metricName)
        .add(Threshold.builder()
                 .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                 .comparisonType(ThresholdComparisonType.DELTA)
                 .high(10)
                 .medium(5)
                 .build());

    // slow calls
    metricName = AppdynamicsConstants.METRIC_NAMES_TO_VARIABLES.get(AppdynamicsConstants.NUMBER_OF_SLOW_CALLS);
    APP_DYNAMICS_VALUES_TO_ANALYZE.put(metricName, new ArrayList<>());
    APP_DYNAMICS_VALUES_TO_ANALYZE.get(metricName)
        .add(Threshold.builder()
                 .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                 .comparisonType(ThresholdComparisonType.DELTA)
                 .high(100)
                 .medium(50)
                 .build());
    APP_DYNAMICS_VALUES_TO_ANALYZE.get(metricName)
        .add(Threshold.builder()
                 .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                 .comparisonType(ThresholdComparisonType.RATIO)
                 .high(1.5)
                 .medium(1.25)
                 .build());

    // error
    metricName = AppdynamicsConstants.METRIC_NAMES_TO_VARIABLES.get(AppdynamicsConstants.ERRORS_PER_MINUTE);
    APP_DYNAMICS_VALUES_TO_ANALYZE.put(metricName, new ArrayList<>());
    APP_DYNAMICS_VALUES_TO_ANALYZE.get(metricName)
        .add(Threshold.builder()
                 .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                 .comparisonType(ThresholdComparisonType.RATIO)
                 .high(1.5)
                 .medium(1.25)
                 .build());
    APP_DYNAMICS_VALUES_TO_ANALYZE.get(metricName)
        .add(Threshold.builder()
                 .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                 .comparisonType(ThresholdComparisonType.DELTA)
                 .high(10)
                 .medium(5)
                 .build());

    // stalls
    metricName = AppdynamicsConstants.METRIC_NAMES_TO_VARIABLES.get(AppdynamicsConstants.STALL_COUNT);
    APP_DYNAMICS_VALUES_TO_ANALYZE.put(metricName, new ArrayList<>());
    APP_DYNAMICS_VALUES_TO_ANALYZE.get(metricName)
        .add(Threshold.builder()
                 .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                 .comparisonType(ThresholdComparisonType.DELTA)
                 .high(10)
                 .medium(5)
                 .build());
    APP_DYNAMICS_VALUES_TO_ANALYZE.get(metricName)
        .add(Threshold.builder()
                 .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                 .comparisonType(ThresholdComparisonType.RATIO)
                 .high(1.5)
                 .medium(1.25)
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
    if (records == null || records.isEmpty()) {
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
