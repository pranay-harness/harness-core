package software.wings.metrics;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.wings.metrics.MetricDefinition.Threshold;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

/**
 * Created by rsingh on 9/7/17.
 */
public class NewRelicMetricValueDefinitionTest {
  private NewRelicMetricDataRecord testRecord;
  private NewRelicMetricDataRecord controlRecord;

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
    for (Entry<String, List<Threshold>> valuesToAnalyze : NewRelicMetricValueDefinition.VALUES_TO_ANALYZE.entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("apdexScore")) {
        Assert.assertEquals(RiskLevel.HIGH, analysisValue.getRiskLevel());
        Assert.assertEquals(testRecord.getApdexScore(), analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(controlRecord.getApdexScore(), analysisValue.getControlValue(), 0.01);
      } else {
        Assert.assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        Assert.assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.setApdexScore(0.60);
    for (Entry<String, List<Threshold>> valuesToAnalyze : NewRelicMetricValueDefinition.VALUES_TO_ANALYZE.entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("apdexScore")) {
        Assert.assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        Assert.assertEquals(testRecord.getApdexScore(), analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(controlRecord.getApdexScore(), analysisValue.getControlValue(), 0.01);
      } else {
        Assert.assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        Assert.assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for low
    testRecord.setApdexScore(0.85);
    for (Entry<String, List<Threshold>> valuesToAnalyze : NewRelicMetricValueDefinition.VALUES_TO_ANALYZE.entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("apdexScore")) {
        Assert.assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        Assert.assertEquals(testRecord.getApdexScore(), analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(controlRecord.getApdexScore(), analysisValue.getControlValue(), 0.01);
      } else {
        Assert.assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        Assert.assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }
  }

  @Test
  public void testAlertWhenLowerThroughput() {
    testRecord.setThroughput(98);
    controlRecord.setThroughput(200);
    for (Entry<String, List<Threshold>> valuesToAnalyze : NewRelicMetricValueDefinition.VALUES_TO_ANALYZE.entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("throughput")) {
        Assert.assertEquals(RiskLevel.HIGH, analysisValue.getRiskLevel());
        Assert.assertEquals(testRecord.getThroughput(), analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(controlRecord.getThroughput(), analysisValue.getControlValue(), 0.01);
      } else {
        Assert.assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        Assert.assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.setThroughput(49);
    controlRecord.setThroughput(100);
    for (Entry<String, List<Threshold>> valuesToAnalyze : NewRelicMetricValueDefinition.VALUES_TO_ANALYZE.entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("throughput")) {
        Assert.assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        Assert.assertEquals(testRecord.getThroughput(), analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(controlRecord.getThroughput(), analysisValue.getControlValue(), 0.01);
      } else {
        Assert.assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        Assert.assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.setThroughput(140);
    controlRecord.setThroughput(200);
    for (Entry<String, List<Threshold>> valuesToAnalyze : NewRelicMetricValueDefinition.VALUES_TO_ANALYZE.entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("throughput")) {
        Assert.assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        Assert.assertEquals(testRecord.getThroughput(), analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(controlRecord.getThroughput(), analysisValue.getControlValue(), 0.01);
      } else {
        Assert.assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        Assert.assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for low
    testRecord.setThroughput(85);
    controlRecord.setThroughput(100);
    for (Entry<String, List<Threshold>> valuesToAnalyze : NewRelicMetricValueDefinition.VALUES_TO_ANALYZE.entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("throughput")) {
        Assert.assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        Assert.assertEquals(testRecord.getThroughput(), analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(controlRecord.getThroughput(), analysisValue.getControlValue(), 0.01);
      } else {
        Assert.assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        Assert.assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }
  }

  @Test
  public void testAlertWhenHigherResponseTime() {
    testRecord.setAverageResponseTime(100);
    controlRecord.setAverageResponseTime(50);
    for (Entry<String, List<Threshold>> valuesToAnalyze : NewRelicMetricValueDefinition.VALUES_TO_ANALYZE.entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("averageResponseTime")) {
        Assert.assertEquals(RiskLevel.HIGH, analysisValue.getRiskLevel());
        Assert.assertEquals(testRecord.getAverageResponseTime(), analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(controlRecord.getAverageResponseTime(), analysisValue.getControlValue(), 0.01);
      } else {
        Assert.assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        Assert.assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.setAverageResponseTime(130);
    controlRecord.setAverageResponseTime(100);
    for (Entry<String, List<Threshold>> valuesToAnalyze : NewRelicMetricValueDefinition.VALUES_TO_ANALYZE.entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("averageResponseTime")) {
        Assert.assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        Assert.assertEquals(testRecord.getAverageResponseTime(), analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(controlRecord.getAverageResponseTime(), analysisValue.getControlValue(), 0.01);
      } else {
        Assert.assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        Assert.assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.setAverageResponseTime(16);
    controlRecord.setAverageResponseTime(10);
    for (Entry<String, List<Threshold>> valuesToAnalyze : NewRelicMetricValueDefinition.VALUES_TO_ANALYZE.entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("averageResponseTime")) {
        Assert.assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        Assert.assertEquals(testRecord.getAverageResponseTime(), analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(controlRecord.getAverageResponseTime(), analysisValue.getControlValue(), 0.01);
      } else {
        Assert.assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        Assert.assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for low
    testRecord.setAverageResponseTime(85);
    controlRecord.setAverageResponseTime(100);
    for (Entry<String, List<Threshold>> valuesToAnalyze : NewRelicMetricValueDefinition.VALUES_TO_ANALYZE.entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("averageResponseTime")) {
        Assert.assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        Assert.assertEquals(testRecord.getAverageResponseTime(), analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(controlRecord.getAverageResponseTime(), analysisValue.getControlValue(), 0.01);
      } else {
        Assert.assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        Assert.assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }
  }

  @Test
  public void testAlertWhenHigherError() {
    testRecord.setError(100);
    controlRecord.setError(50);
    for (Entry<String, List<Threshold>> valuesToAnalyze : NewRelicMetricValueDefinition.VALUES_TO_ANALYZE.entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("error")) {
        Assert.assertEquals(RiskLevel.HIGH, analysisValue.getRiskLevel());
        Assert.assertEquals(testRecord.getError(), analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(controlRecord.getError(), analysisValue.getControlValue(), 0.01);
      } else {
        Assert.assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        Assert.assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.setError(130);
    controlRecord.setError(100);
    for (Entry<String, List<Threshold>> valuesToAnalyze : NewRelicMetricValueDefinition.VALUES_TO_ANALYZE.entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("error")) {
        Assert.assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        Assert.assertEquals(testRecord.getError(), analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(controlRecord.getError(), analysisValue.getControlValue(), 0.01);
      } else {
        Assert.assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        Assert.assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for medium
    testRecord.setError(16);
    controlRecord.setError(10);
    for (Entry<String, List<Threshold>> valuesToAnalyze : NewRelicMetricValueDefinition.VALUES_TO_ANALYZE.entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("error")) {
        Assert.assertEquals(RiskLevel.MEDIUM, analysisValue.getRiskLevel());
        Assert.assertEquals(testRecord.getError(), analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(controlRecord.getError(), analysisValue.getControlValue(), 0.01);
      } else {
        Assert.assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        Assert.assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }

    // test for low
    testRecord.setError(85);
    controlRecord.setError(100);
    for (Entry<String, List<Threshold>> valuesToAnalyze : NewRelicMetricValueDefinition.VALUES_TO_ANALYZE.entrySet()) {
      NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                .metricName("metric")
                                                                .metricValueName(valuesToAnalyze.getKey())
                                                                .thresholds(valuesToAnalyze.getValue())
                                                                .build();

      NewRelicMetricAnalysisValue analysisValue = metricValueDefinition.analyze(
          Collections.singletonList(testRecord), Collections.singletonList(controlRecord));

      if (analysisValue.getName().equals("error")) {
        Assert.assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        Assert.assertEquals(testRecord.getError(), analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(controlRecord.getError(), analysisValue.getControlValue(), 0.01);
      } else {
        Assert.assertEquals(RiskLevel.LOW, analysisValue.getRiskLevel());
        Assert.assertEquals(0.0, analysisValue.getTestValue(), 0.01);
        Assert.assertEquals(0.0, analysisValue.getControlValue(), 0.01);
      }
    }
  }
}
