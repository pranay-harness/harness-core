package software.wings.metrics;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by rsingh on 9/7/17.
 */
public class NewRelicMetricValueDefinitionTest {
  private NewRelicMetricDataRecord testRecord;
  private NewRelicMetricDataRecord controlRecord;

  private Map<String, List<Threshold>> getThresholdsMap(Map<String, TimeSeriesMetricDefinition> stateValuesToAnalyze) {
    Map<String, List<Threshold>> stateValuesToThresholds = new HashMap<>();
    for (Entry<String, TimeSeriesMetricDefinition> entry : stateValuesToAnalyze.entrySet()) {
      stateValuesToThresholds.put(entry.getKey(), entry.getValue().getThresholds());
    }

    return stateValuesToThresholds;
  }

  @Before
  public void setUp() throws Exception {
    testRecord = NewRelicMetricDataRecord.builder()
                     .name("metric")
                     .workflowId("workflowId")
                     .workflowExecutionId("workflowExecutionId")
                     .serviceId("serviceId")
                     .stateExecutionId("stateExecutionId")
                     .timeStamp(System.currentTimeMillis())
                     .host("host")
                     .dataCollectionMinute(1)
                     .throughput(0.0)
                     .averageResponseTime(0.0)
                     .error(0.0)
                     .apdexScore(0.0)
                     .build();

    controlRecord = NewRelicMetricDataRecord.builder()
                        .name("metric")
                        .workflowId("workflowId")
                        .workflowExecutionId("workflowExecutionId")
                        .serviceId("serviceId")
                        .stateExecutionId("stateExecutionId")
                        .timeStamp(System.currentTimeMillis())
                        .host("host")
                        .dataCollectionMinute(1)
                        .throughput(0.0)
                        .averageResponseTime(0.0)
                        .error(0.0)
                        .apdexScore(0.0)
                        .build();
  }

  @Test
  public void testAlertWhenLowerApdexWihRatio() {
    testRecord.setApdexScore(0.40);
    controlRecord.setApdexScore(1.0);
    for (Entry<String, List<Threshold>> valuesToAnalyze :
        getThresholdsMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("apdexScore")) {
        assertEquals(RiskLevel.HIGH, analysisValue.getRiskLevel());
        assertEquals(testRecord.getApdexScore(), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getApdexScore(), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.setApdexScore(0.60);
    for (Entry<String, List<Threshold>> valuesToAnalyze :
        getThresholdsMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("apdexScore")) {
        assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        assertEquals(testRecord.getApdexScore(), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getApdexScore(), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for low
    testRecord.setApdexScore(0.85);
    for (Entry<String, List<Threshold>> valuesToAnalyze :
        getThresholdsMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("apdexScore")) {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(testRecord.getApdexScore(), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getApdexScore(), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }
  }

  @Test
  public void testAlertWhenLowerThroughput() {
    testRecord.setThroughput(98);
    controlRecord.setThroughput(200);
    for (Entry<String, List<Threshold>> valuesToAnalyze :
        getThresholdsMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("throughput")) {
        assertEquals(RiskLevel.HIGH, analysisValue.getRiskLevel());
        assertEquals(testRecord.getThroughput(), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getThroughput(), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.setThroughput(49);
    controlRecord.setThroughput(100);
    for (Entry<String, List<Threshold>> valuesToAnalyze :
        getThresholdsMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("throughput")) {
        assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        assertEquals(testRecord.getThroughput(), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getThroughput(), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.setThroughput(140);
    controlRecord.setThroughput(200);
    for (Entry<String, List<Threshold>> valuesToAnalyze :
        getThresholdsMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("throughput")) {
        assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        assertEquals(testRecord.getThroughput(), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getThroughput(), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for low
    testRecord.setThroughput(85);
    controlRecord.setThroughput(100);
    for (Entry<String, List<Threshold>> valuesToAnalyze :
        getThresholdsMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("throughput")) {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(testRecord.getThroughput(), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getThroughput(), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for zero throughput
    testRecord.setThroughput(0);
    controlRecord.setThroughput(150);
    for (Entry<String, List<Threshold>> valuesToAnalyze :
        getThresholdsMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("throughput")) {
        assertEquals(RiskLevel.HIGH, analysisValue.getRiskLevel());
        assertEquals(testRecord.getThroughput(), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getThroughput(), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }
  }

  @Test
  public void testAlertWhenHigherResponseTime() {
    testRecord.setAverageResponseTime(100);
    controlRecord.setAverageResponseTime(50);
    for (Entry<String, List<Threshold>> valuesToAnalyze :
        getThresholdsMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("averageResponseTime")) {
        assertEquals(RiskLevel.HIGH, analysisValue.getRiskLevel());
        assertEquals(testRecord.getAverageResponseTime(), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getAverageResponseTime(), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.setAverageResponseTime(130);
    controlRecord.setAverageResponseTime(100);
    for (Entry<String, List<Threshold>> valuesToAnalyze :
        getThresholdsMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("averageResponseTime")) {
        assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        assertEquals(testRecord.getAverageResponseTime(), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getAverageResponseTime(), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.setAverageResponseTime(16);
    controlRecord.setAverageResponseTime(10);
    for (Entry<String, List<Threshold>> valuesToAnalyze :
        getThresholdsMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("averageResponseTime")) {
        assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        assertEquals(testRecord.getAverageResponseTime(), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getAverageResponseTime(), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for low
    testRecord.setAverageResponseTime(85);
    controlRecord.setAverageResponseTime(100);
    for (Entry<String, List<Threshold>> valuesToAnalyze :
        getThresholdsMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("averageResponseTime")) {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(testRecord.getAverageResponseTime(), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getAverageResponseTime(), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }
  }

  @Test
  public void testAlertWhenHigherError() {
    testRecord.setError(100);
    controlRecord.setError(50);
    for (Entry<String, List<Threshold>> valuesToAnalyze :
        getThresholdsMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("error")) {
        assertEquals(RiskLevel.HIGH, analysisValue.getRiskLevel());
        assertEquals(testRecord.getError(), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getError(), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.setError(106);
    controlRecord.setError(100);
    for (Entry<String, List<Threshold>> valuesToAnalyze :
        getThresholdsMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("error")) {
        assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        assertEquals(testRecord.getError(), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getError(), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.setError(2);
    controlRecord.setError(0.5);
    for (Entry<String, List<Threshold>> valuesToAnalyze :
        getThresholdsMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("error")) {
        assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        assertEquals(testRecord.getError(), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getError(), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for low
    testRecord.setError(85);
    controlRecord.setError(100);
    for (Entry<String, List<Threshold>> valuesToAnalyze :
        getThresholdsMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("error")) {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(testRecord.getError(), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getError(), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }
  }
}
