package software.wings.metrics;

import static org.junit.Assert.assertEquals;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.APDEX_SCORE;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.AVERAGE_RESPONSE_TIME;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.ERROR;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.REQUSET_PER_MINUTE;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.THROUGHPUT;

import org.junit.Before;
import org.junit.Test;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by rsingh on 9/7/17.
 */
public class NewRelicMetricValueDefinitionTest {
  private NewRelicMetricDataRecord testRecord;
  private NewRelicMetricDataRecord controlRecord;

  private Map<String, MetricType> getMetricTypeMap(Map<String, TimeSeriesMetricDefinition> stateValuesToAnalyze) {
    Map<String, MetricType> stateValuesToThresholds = new HashMap<>();
    for (Entry<String, TimeSeriesMetricDefinition> entry : stateValuesToAnalyze.entrySet()) {
      stateValuesToThresholds.put(entry.getKey(), entry.getValue().getMetricType());
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
                     .values(new HashMap<>())
                     .build();
    testRecord.getValues().put(THROUGHPUT, 0.0);
    testRecord.getValues().put(REQUSET_PER_MINUTE, 0.0);
    testRecord.getValues().put(AVERAGE_RESPONSE_TIME, 0.0);
    testRecord.getValues().put(ERROR, 0.0);
    testRecord.getValues().put(APDEX_SCORE, 0.0);

    controlRecord = NewRelicMetricDataRecord.builder()
                        .name("metric")
                        .workflowId("workflowId")
                        .workflowExecutionId("workflowExecutionId")
                        .serviceId("serviceId")
                        .stateExecutionId("stateExecutionId")
                        .timeStamp(System.currentTimeMillis())
                        .host("host")
                        .values(new HashMap<>())
                        .dataCollectionMinute(1)
                        .build();

    controlRecord.getValues().put(THROUGHPUT, 0.0);
    controlRecord.getValues().put(REQUSET_PER_MINUTE, 0.0);
    controlRecord.getValues().put(AVERAGE_RESPONSE_TIME, 0.0);
    controlRecord.getValues().put(ERROR, 0.0);
    controlRecord.getValues().put(APDEX_SCORE, 0.0);
  }

  @Test
  public void testAlertWhenLowerApdexWihRatio() {
    testRecord.getValues().put(APDEX_SCORE, 0.40);
    controlRecord.getValues().put(APDEX_SCORE, 1.0);
    for (Entry<String, MetricType> valuesToAnalyze :
        getMetricTypeMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .metricType(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals(APDEX_SCORE)) {
        assertEquals(RiskLevel.HIGH, analysisValue.getRiskLevel());
        assertEquals(testRecord.getValues().get(APDEX_SCORE), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getValues().get(APDEX_SCORE), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.getValues().put(APDEX_SCORE, 0.60);
    for (Entry<String, MetricType> valuesToAnalyze :
        getMetricTypeMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .metricType(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals(APDEX_SCORE)) {
        assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        assertEquals(testRecord.getValues().get(APDEX_SCORE), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getValues().get(APDEX_SCORE), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for low
    testRecord.getValues().put(APDEX_SCORE, 0.85);
    for (Entry<String, MetricType> valuesToAnalyze :
        getMetricTypeMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .metricType(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals(APDEX_SCORE)) {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(testRecord.getValues().get(APDEX_SCORE), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getValues().get(APDEX_SCORE), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }
  }

  @Test
  public void testAlertWhenLowerThroughput() {
    testRecord.getValues().put(THROUGHPUT, 98.0);
    controlRecord.getValues().put(THROUGHPUT, 200.0);
    for (Entry<String, MetricType> valuesToAnalyze :
        getMetricTypeMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .metricType(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals(THROUGHPUT)) {
        assertEquals(RiskLevel.HIGH, analysisValue.getRiskLevel());
        assertEquals(testRecord.getValues().get(THROUGHPUT), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getValues().get(THROUGHPUT), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.getValues().put(THROUGHPUT, 49.0);
    controlRecord.getValues().put(THROUGHPUT, 100.0);
    for (Entry<String, MetricType> valuesToAnalyze :
        getMetricTypeMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .metricType(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals(THROUGHPUT)) {
        assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        assertEquals(testRecord.getValues().get(THROUGHPUT), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getValues().get(THROUGHPUT), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.getValues().put(THROUGHPUT, 140.0);
    controlRecord.getValues().put(THROUGHPUT, 200.0);
    for (Entry<String, MetricType> valuesToAnalyze :
        getMetricTypeMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .metricType(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals(THROUGHPUT)) {
        assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        assertEquals(testRecord.getValues().get(THROUGHPUT), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getValues().get(THROUGHPUT), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for low
    testRecord.getValues().put(THROUGHPUT, 85.0);
    controlRecord.getValues().put(THROUGHPUT, 100.0);
    for (Entry<String, MetricType> valuesToAnalyze :
        getMetricTypeMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .metricType(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals(THROUGHPUT)) {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(testRecord.getValues().get(THROUGHPUT), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getValues().get(THROUGHPUT), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for zero throughput
    testRecord.getValues().put(THROUGHPUT, 0.0);
    controlRecord.getValues().put(THROUGHPUT, 150.0);
    for (Entry<String, MetricType> valuesToAnalyze :
        getMetricTypeMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .metricType(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals(THROUGHPUT)) {
        assertEquals(RiskLevel.HIGH, analysisValue.getRiskLevel());
        assertEquals(testRecord.getValues().get(THROUGHPUT), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getValues().get(THROUGHPUT), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }
  }

  @Test
  public void testAlertWhenHigherResponseTime() {
    testRecord.getValues().put(AVERAGE_RESPONSE_TIME, 100.0);
    controlRecord.getValues().put(AVERAGE_RESPONSE_TIME, 50.0);
    for (Entry<String, MetricType> valuesToAnalyze :
        getMetricTypeMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .metricType(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals(AVERAGE_RESPONSE_TIME)) {
        assertEquals(RiskLevel.HIGH, analysisValue.getRiskLevel());
        assertEquals(testRecord.getValues().get(AVERAGE_RESPONSE_TIME), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getValues().get(AVERAGE_RESPONSE_TIME), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.getValues().put(AVERAGE_RESPONSE_TIME, 130.0);
    controlRecord.getValues().put(AVERAGE_RESPONSE_TIME, 100.0);
    for (Entry<String, MetricType> valuesToAnalyze :
        getMetricTypeMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .metricType(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals(AVERAGE_RESPONSE_TIME)) {
        assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        assertEquals(testRecord.getValues().get(AVERAGE_RESPONSE_TIME), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getValues().get(AVERAGE_RESPONSE_TIME), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.getValues().put(AVERAGE_RESPONSE_TIME, 16.0);
    controlRecord.getValues().put(AVERAGE_RESPONSE_TIME, 10.0);
    for (Entry<String, MetricType> valuesToAnalyze :
        getMetricTypeMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .metricType(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals(AVERAGE_RESPONSE_TIME)) {
        assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        assertEquals(testRecord.getValues().get(AVERAGE_RESPONSE_TIME), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getValues().get(AVERAGE_RESPONSE_TIME), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for low
    testRecord.getValues().put(AVERAGE_RESPONSE_TIME, 85.0);
    controlRecord.getValues().put(AVERAGE_RESPONSE_TIME, 100.0);
    for (Entry<String, MetricType> valuesToAnalyze :
        getMetricTypeMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .metricType(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals(AVERAGE_RESPONSE_TIME)) {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(testRecord.getValues().get(AVERAGE_RESPONSE_TIME), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getValues().get(AVERAGE_RESPONSE_TIME), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }
  }

  @Test
  public void testAlertWhenHigherError() {
    testRecord.getValues().put(ERROR, 100.0);
    controlRecord.getValues().put(ERROR, 50.0);
    for (Entry<String, MetricType> valuesToAnalyze :
        getMetricTypeMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .metricType(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals(ERROR)) {
        assertEquals(RiskLevel.HIGH, analysisValue.getRiskLevel());
        assertEquals(testRecord.getValues().get(ERROR), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getValues().get(ERROR), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.getValues().put(ERROR, 106.0);
    controlRecord.getValues().put(ERROR, 100.0);
    for (Entry<String, MetricType> valuesToAnalyze :
        getMetricTypeMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .metricType(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals(ERROR)) {
        assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        assertEquals(testRecord.getValues().get(ERROR), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getValues().get(ERROR), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.getValues().put(ERROR, 2.0);
    controlRecord.getValues().put(ERROR, 0.50);
    for (Entry<String, MetricType> valuesToAnalyze :
        getMetricTypeMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .metricType(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals(ERROR)) {
        assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        assertEquals(testRecord.getValues().get(ERROR), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getValues().get(ERROR), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for low
    testRecord.getValues().put(ERROR, 85.0);
    controlRecord.getValues().put(ERROR, 100.0);
    for (Entry<String, MetricType> valuesToAnalyze :
        getMetricTypeMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE).entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .metricType(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals(ERROR)) {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(testRecord.getValues().get(ERROR), analysisValue.getTestValue(), 0.01);
        assertEquals(controlRecord.getValues().get(ERROR), analysisValue.getControlValue(), 0.01);
      } else {
        assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }
  }
}
