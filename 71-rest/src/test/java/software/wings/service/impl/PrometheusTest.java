package software.wings.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.metrics.MetricType;
import software.wings.resources.PrometheusResource;
import software.wings.service.impl.analysis.TimeSeries;

import java.util.Map;

public class PrometheusTest extends WingsBaseTest {
  @Test
  @Category(UnitTests.class)
  public void testEmptyTxns() {
    final Map<String, String> invalidFields = PrometheusResource.validateTransactions(null, false);
    assertEquals(1, invalidFields.size());
    assertEquals("No metrics given to analyze.", invalidFields.get("timeSeriesToAnalyze"));
  }

  @Test
  @Category(UnitTests.class)
  public void testInvalidUrl() {
    final TimeSeries timeSeries1 =
        TimeSeries.builder().txnName("t1").metricName("m1").metricType(MetricType.INFRA.name()).url("url").build();
    final TimeSeries timeSeries2 =
        TimeSeries.builder().txnName("t1").metricName("m2").metricType(MetricType.INFRA.name()).url("url").build();
    Map<String, String> invalidFields =
        PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2), false);
    assertEquals(2, invalidFields.size());
    assertEquals("[$hostName, $startTime, $endTime] are not present in the url.",
        invalidFields.get("Invalid url for txn: t1, metric : m1"));
    assertEquals("[$hostName, $startTime, $endTime] are not present in the url.",
        invalidFields.get("Invalid url for txn: t1, metric : m2"));

    // fix and validate
    timeSeries2.setUrl("$hostName$startTime$endTime");

    invalidFields = PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2), false);
    assertEquals(1, invalidFields.size());
    assertEquals("[$hostName, $startTime, $endTime] are not present in the url.",
        invalidFields.get("Invalid url for txn: t1, metric : m1"));

    // fix and validate
    timeSeries1.setUrl("$hostName$startTime$endTime");
    invalidFields = PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2), false);
    assertTrue(invalidFields.isEmpty());
  }

  @Test
  @Category(UnitTests.class)
  public void testSameMetricDifferentTypes() {
    final TimeSeries timeSeries1 =
        TimeSeries.builder().txnName("t1").metricName("m1").metricType(MetricType.RESP_TIME.name()).url("url").build();
    final TimeSeries timeSeries2 =
        TimeSeries.builder().txnName("t1").metricName("m1").metricType(MetricType.INFRA.name()).url("url").build();
    Map<String, String> invalidFields =
        PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2), false);
    assertEquals(3, invalidFields.size());
    assertEquals("[$hostName, $startTime, $endTime] are not present in the url.",
        invalidFields.get("Invalid url for txn: t1, metric : m1"));
    assertEquals(
        "m1 has been configured as RESP_TIME in previous transactions. Same metric name can not have different metric types.",
        invalidFields.get("Invalid metric type for txn: t1, metric : m1"));

    assertEquals("t1 has error metrics [] and/or response time metrics [m1] but no throughput metrics.",
        invalidFields.get("Invalid metrics for txn: t1"));

    // fix and validate
    timeSeries1.setUrl("$hostName$startTime$endTime");
    timeSeries2.setUrl("$hostName$startTime$endTime");

    invalidFields = PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2), false);
    assertEquals(2, invalidFields.size());
    assertEquals(
        "m1 has been configured as RESP_TIME in previous transactions. Same metric name can not have different metric types.",
        invalidFields.get("Invalid metric type for txn: t1, metric : m1"));
    assertEquals("t1 has error metrics [] and/or response time metrics [m1] but no throughput metrics.",
        invalidFields.get("Invalid metrics for txn: t1"));

    // fix and validate
    timeSeries1.setMetricType(MetricType.INFRA.name());
    invalidFields = PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2), false);
    assertTrue(invalidFields.isEmpty());
  }

  @Test
  @Category(UnitTests.class)
  public void testOnlyThroughput() {
    final TimeSeries timeSeries = TimeSeries.builder()
                                      .txnName("t1")
                                      .metricName("m1")
                                      .metricType(MetricType.THROUGHPUT.name())
                                      .url("$hostName$startTime$endTime")
                                      .build();
    Map<String, String> invalidFields = PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries), false);
    assertEquals(1, invalidFields.size());
    assertEquals(
        "t1 has only throughput metrics [m1]. Throughput metrics is used to analyze other metrics and is not analyzed.",
        invalidFields.get("Invalid metrics for txn: t1"));
  }

  @Test
  @Category(UnitTests.class)
  public void testOnlyError() {
    final TimeSeries timeSeries1 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m1")
                                       .metricType(MetricType.ERROR.name())
                                       .url("$hostName$startTime$endTime")
                                       .build();
    final TimeSeries timeSeries2 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m2")
                                       .metricType(MetricType.ERROR.name())
                                       .url("$hostName$startTime$endTime")
                                       .build();
    Map<String, String> invalidFields =
        PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2), false);
    assertEquals(1, invalidFields.size());
    assertEquals("t1 has error metrics [m1, m2] and/or response time metrics [] but no throughput metrics.",
        invalidFields.get("Invalid metrics for txn: t1"));
  }

  @Test
  @Category(UnitTests.class)
  public void testOnlyResponseTime() {
    final TimeSeries timeSeries1 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m1")
                                       .metricType(MetricType.RESP_TIME.name())
                                       .url("$hostName$startTime$endTime")
                                       .build();
    final TimeSeries timeSeries2 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m2")
                                       .metricType(MetricType.RESP_TIME.name())
                                       .url("$hostName$startTime$endTime")
                                       .build();
    Map<String, String> invalidFields =
        PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2), false);
    assertEquals(1, invalidFields.size());
    assertEquals("t1 has error metrics [] and/or response time metrics [m1, m2] but no throughput metrics.",
        invalidFields.get("Invalid metrics for txn: t1"));
  }

  @Test
  @Category(UnitTests.class)
  public void testMultipleThroughPuts() {
    final TimeSeries timeSeries1 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m1")
                                       .metricType(MetricType.ERROR.name())
                                       .url("$hostName$startTime$endTime")
                                       .build();
    final TimeSeries timeSeries2 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m2")
                                       .metricType(MetricType.THROUGHPUT.name())
                                       .url("$hostName$startTime$endTime")
                                       .build();
    final TimeSeries timeSeries3 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m3")
                                       .metricType(MetricType.THROUGHPUT.name())
                                       .url("$hostName$startTime$endTime")
                                       .build();
    Map<String, String> invalidFields =
        PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2, timeSeries3), false);
    assertEquals(1, invalidFields.size());
    assertEquals(
        "t1 has more than one throughput metrics [m2, m3] defined.", invalidFields.get("Invalid metrics for txn: t1"));
  }

  @Test
  @Category(UnitTests.class)
  public void testValidError() {
    final TimeSeries timeSeries1 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m1")
                                       .metricType(MetricType.ERROR.name())
                                       .url("$hostName$startTime$endTime")
                                       .build();
    final TimeSeries timeSeries2 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m2")
                                       .metricType(MetricType.ERROR.name())
                                       .url("$hostName$startTime$endTime")
                                       .build();
    final TimeSeries timeSeries3 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m3")
                                       .metricType(MetricType.THROUGHPUT.name())
                                       .url("$hostName$startTime$endTime")
                                       .build();
    Map<String, String> invalidFields =
        PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2, timeSeries3), false);
    assertTrue(invalidFields.isEmpty());
  }

  @Test
  @Category(UnitTests.class)
  public void testValidResponseTime() {
    final TimeSeries timeSeries1 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m1")
                                       .metricType(MetricType.RESP_TIME.name())
                                       .url("$hostName$startTime$endTime")
                                       .build();
    final TimeSeries timeSeries2 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m2")
                                       .metricType(MetricType.ERROR.name())
                                       .url("$hostName$startTime$endTime")
                                       .build();
    final TimeSeries timeSeries3 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m3")
                                       .metricType(MetricType.THROUGHPUT.name())
                                       .url("$hostName$startTime$endTime")
                                       .build();
    Map<String, String> invalidFields =
        PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2, timeSeries3), false);
    assertTrue(invalidFields.isEmpty());
  }

  @Test
  @Category(UnitTests.class)
  public void testValidNonError() {
    final TimeSeries timeSeries1 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m1")
                                       .metricType(MetricType.INFRA.name())
                                       .url("$hostName$startTime$endTime")
                                       .build();
    final TimeSeries timeSeries2 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m2")
                                       .metricType(MetricType.INFRA.name())
                                       .url("$hostName$startTime$endTime")
                                       .build();
    final TimeSeries timeSeries3 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m3")
                                       .metricType(MetricType.INFRA.name())
                                       .url("$hostName$startTime$endTime")
                                       .build();
    Map<String, String> invalidFields =
        PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2, timeSeries3), false);
    assertTrue(invalidFields.isEmpty());
  }
}
