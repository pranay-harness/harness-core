package software.wings.service.impl.newrelic;

import com.google.common.math.Stats;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang.WordUtils;
import software.wings.metrics.MetricDefinition.Threshold;
import software.wings.metrics.MetricDefinition.ThresholdType;
import software.wings.metrics.RiskLevel;
import software.wings.metrics.ThresholdComparisonType;
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
  public static Map<String, List<Threshold>> VALUES_TO_ANALYZE = new HashMap<>();

  static {
    // throughput
    VALUES_TO_ANALYZE.put("throughput", new ArrayList<>());
    VALUES_TO_ANALYZE.get("throughput")
        .add(Threshold.builder()
                 .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
                 .comparisonType(ThresholdComparisonType.RATIO)
                 .high(0.5)
                 .medium(0.75)
                 .build());
    VALUES_TO_ANALYZE.get("throughput")
        .add(Threshold.builder()
                 .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                 .comparisonType(ThresholdComparisonType.DELTA)
                 .high(100)
                 .medium(50)
                 .build());

    // averageResponseTime
    VALUES_TO_ANALYZE.put("averageResponseTime", new ArrayList<>());
    VALUES_TO_ANALYZE.get("averageResponseTime")
        .add(Threshold.builder()
                 .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                 .comparisonType(ThresholdComparisonType.RATIO)
                 .high(1.5)
                 .medium(1.25)
                 .build());
    VALUES_TO_ANALYZE.get("averageResponseTime")
        .add(Threshold.builder()
                 .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                 .comparisonType(ThresholdComparisonType.DELTA)
                 .high(10)
                 .medium(5)
                 .build());

    // error
    VALUES_TO_ANALYZE.put("error", new ArrayList<>());
    VALUES_TO_ANALYZE.get("error").add(Threshold.builder()
                                           .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                                           .comparisonType(ThresholdComparisonType.RATIO)
                                           .high(1.5)
                                           .medium(1.25)
                                           .build());
    VALUES_TO_ANALYZE.get("error").add(Threshold.builder()
                                           .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                                           .comparisonType(ThresholdComparisonType.DELTA)
                                           .high(10)
                                           .medium(5)
                                           .build());

    // apdexScore
    VALUES_TO_ANALYZE.put("apdexScore", new ArrayList<>());
    VALUES_TO_ANALYZE.get("apdexScore")
        .add(Threshold.builder()
                 .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
                 .comparisonType(ThresholdComparisonType.ABSOLUTE)
                 .high(0.5)
                 .medium(0.75)
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
          .riskLevel(RiskLevel.LOW)
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
