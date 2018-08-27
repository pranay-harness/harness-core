package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.metrics.MetricType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.sm.states.NewRelicState.Metric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewRelicStateTest extends WingsBaseTest {
  private NewRelicState.Metric requestsPerMinuteMetric, averageResponseTimeMetric, errorMetric, apdexScoreMetric;
  private List<NewRelicState.Metric> expectedMetrics;

  @Inject @InjectMocks private NewRelicService newRelicService;

  @Before
  public void setUp() throws Exception {
    requestsPerMinuteMetric = NewRelicState.Metric.builder()
                                  .metricName(NewRelicMetricValueDefinition.REQUSET_PER_MINUTE)
                                  .mlMetricType(MetricType.THROUGHPUT)
                                  .displayName("Requests per Minute")
                                  .build();
    averageResponseTimeMetric = NewRelicState.Metric.builder()
                                    .metricName(NewRelicMetricValueDefinition.AVERAGE_RESPONSE_TIME)
                                    .mlMetricType(MetricType.RESP_TIME)
                                    .displayName("Response Time")
                                    .build();
    errorMetric = NewRelicState.Metric.builder()
                      .metricName(NewRelicMetricValueDefinition.ERROR)
                      .mlMetricType(MetricType.ERROR)
                      .displayName("ERROR")
                      .build();
    apdexScoreMetric = NewRelicState.Metric.builder()
                           .metricName(NewRelicMetricValueDefinition.APDEX_SCORE)
                           .mlMetricType(MetricType.VALUE)
                           .displayName("Apdex Score")
                           .build();

    expectedMetrics = Arrays.asList(requestsPerMinuteMetric, averageResponseTimeMetric, errorMetric, apdexScoreMetric);
  }

  @Test
  public void metrics() {
    /*
    Case 1: metricNames is an empty list
    Expected output: Metric Map should contain all metrics present in the YAML file
     */
    List<String> metricNames = new ArrayList<>();
    Map<String, Metric> metrics = newRelicService.getMetricsCorrespondingToMetricNames(metricNames);
    assertTrue(metrics.containsKey("requestsPerMinute"));
    assertTrue(metrics.containsKey("averageResponseTime"));
    assertTrue(metrics.containsKey("error"));
    assertTrue(metrics.containsKey("apdexScore"));

    /*
    Case 2: metricNames contains a non-empty subset of metrics
     */
    metricNames = Arrays.asList("apdexScore");
    metrics = newRelicService.getMetricsCorrespondingToMetricNames(metricNames);
    assertTrue(metrics.containsKey("apdexScore"));
    assertEquals(1, metrics.size());
    assertTrue(metrics.get("apdexScore").getTags().size() >= 1);
    assertEquals(Sets.newHashSet("WebTransactions"), metrics.get("apdexScore").getTags());

    /*
    Case 3: metricNames contains a list in which are metric names are incorrect
    Expected output: Empty map
     */
    metricNames = Arrays.asList("ApdexScore");
    metrics = newRelicService.getMetricsCorrespondingToMetricNames(metricNames);
    assertEquals(new HashMap<>(), metrics);
  }

  @Test
  public void metricNames() {
    List<NewRelicState.Metric> actualMetrics = newRelicService.getListOfMetrics();
    assertEquals(expectedMetrics, actualMetrics);
  }

  private TimeSeriesMetricDefinition buildTimeSeriesMetricDefinition(Metric metric) {
    return TimeSeriesMetricDefinition.builder()
        .metricName(metric.getMetricName())
        .metricType(metric.getMlMetricType())
        .tags(metric.getTags())
        .build();
  }

  @Test
  public void metricDefinitions() {
    Map<String, TimeSeriesMetricDefinition> expectedMetricDefinitions = new HashMap<>();
    expectedMetricDefinitions.put(
        requestsPerMinuteMetric.getMetricName(), buildTimeSeriesMetricDefinition(requestsPerMinuteMetric));
    expectedMetricDefinitions.put(
        averageResponseTimeMetric.getMetricName(), buildTimeSeriesMetricDefinition(averageResponseTimeMetric));
    expectedMetricDefinitions.put(errorMetric.getMetricName(), buildTimeSeriesMetricDefinition(errorMetric));
    expectedMetricDefinitions.put(apdexScoreMetric.getMetricName(), buildTimeSeriesMetricDefinition(apdexScoreMetric));

    List<String> metricNames = Arrays.asList("requestsPerMinute", "averageResponseTime", "error", "apdexScore");
    Map<String, Metric> metrics = newRelicService.getMetricsCorrespondingToMetricNames(metricNames);
    Map<String, TimeSeriesMetricDefinition> actualMetricDefinitions =
        newRelicService.metricDefinitions(metrics.values());

    assertEquals(expectedMetricDefinitions.get("requestsPerMinute"), actualMetricDefinitions.get("requestsPerMinute"));
    assertEquals(
        expectedMetricDefinitions.get("averageResponseTime"), actualMetricDefinitions.get("averageResponseTime"));
    assertEquals(expectedMetricDefinitions.get("error"), actualMetricDefinitions.get("error"));
    assertEquals(expectedMetricDefinitions.get("apdexScore"), actualMetricDefinitions.get("apdexScore"));
  }
}
