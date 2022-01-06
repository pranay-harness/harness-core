/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.rule.OwnerRule.RAGHU;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.metrics.MetricType;
import software.wings.resources.PrometheusResource;
import software.wings.service.impl.analysis.TimeSeries;

import com.google.common.collect.Lists;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PrometheusTest extends WingsBaseTest {
  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testEmptyTxns() {
    final Map<String, String> invalidFields = PrometheusResource.validateTransactions(null, false);
    assertThat(invalidFields).hasSize(1);
    assertThat(invalidFields.get("timeSeriesToAnalyze")).isEqualTo("No metrics given to analyze.");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testInvalidUrl() {
    final TimeSeries timeSeries1 =
        TimeSeries.builder().txnName("t1").metricName("m1").metricType(MetricType.INFRA.name()).url("url").build();
    final TimeSeries timeSeries2 =
        TimeSeries.builder().txnName("t1").metricName("m2").metricType(MetricType.INFRA.name()).url("url").build();
    Map<String, String> invalidFields =
        PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2), false);
    assertThat(invalidFields).hasSize(4);
    assertThat(invalidFields.get("Invalid query for group: t1, metric : m1"))
        .isEqualTo("$hostName is not present in the url.");
    assertThat(invalidFields.get("Invalid query for group: t1, metric : m2"))
        .isEqualTo("$hostName is not present in the url.");
    assertThat(invalidFields.get("Invalid query format for group: t1, metric: m1"))
        .isEqualTo("Expected format example jvm_memory_max_bytes{pod_name=\"$hostName\"}");
    assertThat(invalidFields.get("Invalid query format for group: t1, metric: m2"))
        .isEqualTo("Expected format example jvm_memory_max_bytes{pod_name=\"$hostName\"}");

    // fix and validate
    timeSeries2.setUrl("$hostName");

    invalidFields = PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2), false);
    assertThat(invalidFields).hasSize(3);
    assertThat(invalidFields.get("Invalid query for group: t1, metric : m1"))
        .isEqualTo("$hostName is not present in the url.");
    assertThat(invalidFields.get("Invalid query format for group: t1, metric: m1"))
        .isEqualTo("Expected format example jvm_memory_max_bytes{pod_name=\"$hostName\"}");
    assertThat(invalidFields.get("Invalid query format for group: t1, metric: m2"))
        .isEqualTo("Expected format example jvm_memory_max_bytes{pod_name=\"$hostName\"}");

    // fix and validate
    timeSeries1.setUrl("jvm_memory_max_bytes{pod_name=\"$hostName\"}");
    timeSeries2.setUrl("jvm_memory_max_bytes{pod_name=\"$hostName\"}");
    invalidFields = PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2), false);
    assertThat(invalidFields.isEmpty()).isTrue();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testSameMetricDifferentTypes() {
    final TimeSeries timeSeries1 =
        TimeSeries.builder().txnName("t1").metricName("m1").metricType(MetricType.RESP_TIME.name()).url("url").build();
    final TimeSeries timeSeries2 =
        TimeSeries.builder().txnName("t1").metricName("m1").metricType(MetricType.INFRA.name()).url("url").build();
    Map<String, String> invalidFields =
        PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2), false);
    assertThat(invalidFields).hasSize(4);
    assertThat(invalidFields.get("Invalid query for group: t1, metric : m1"))
        .isEqualTo("$hostName is not present in the url.");
    assertThat(invalidFields.get("Invalid metric type for group: t1, metric : m1"))
        .isEqualTo("m1 has been configured as RESP_TIME in previous transactions. "
            + "Same metric name can not have different metric types.");
    assertThat(invalidFields.get("Invalid metrics for group: t1"))
        .isEqualTo("t1 has error metrics [] and/or response time metrics [m1] but no throughput metrics.");
    assertThat(invalidFields.get("Invalid query format for group: t1, metric: m1"))
        .isEqualTo("Expected format example jvm_memory_max_bytes{pod_name=\"$hostName\"}");

    // fix and validate
    timeSeries1.setUrl("jvm_memory_max_bytes{pod_name=\"$hostName\"}");
    timeSeries2.setUrl("jvm_memory_max_bytes{pod_name=\"$hostName\"}");

    invalidFields = PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2), false);
    assertThat(invalidFields).hasSize(2);
    assertThat(invalidFields.get("Invalid metric type for group: t1, metric : m1"))
        .isEqualTo("m1 has been configured as RESP_TIME in previous transactions. "
            + "Same metric name can not have different metric types.");
    assertThat(invalidFields.get("Invalid metrics for group: t1"))
        .isEqualTo("t1 has error metrics [] and/or response time metrics [m1] but no throughput metrics.");

    // fix and validate
    timeSeries1.setMetricType(MetricType.INFRA.name());
    invalidFields = PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2), false);
    assertThat(invalidFields.isEmpty()).isTrue();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testOnlyThroughput() {
    final TimeSeries timeSeries = TimeSeries.builder()
                                      .txnName("t1")
                                      .metricName("m1")
                                      .metricType(MetricType.THROUGHPUT.name())
                                      .url("jvm_memory_max_bytes{pod_name=\"$hostName\"}")
                                      .build();
    Map<String, String> invalidFields = PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries), false);
    assertThat(invalidFields).hasSize(1);
    assertThat(invalidFields.get("Invalid metrics for group: t1"))
        .isEqualTo(
            "t1 has only throughput metrics [m1]. Throughput metrics is used to analyze other metrics and is not analyzed.");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testOnlyError() {
    final TimeSeries timeSeries1 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m1")
                                       .metricType(MetricType.ERROR.name())
                                       .url("jvm_memory_max_bytes{pod_name=\"$hostName\"}")
                                       .build();
    final TimeSeries timeSeries2 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m2")
                                       .metricType(MetricType.ERROR.name())
                                       .url("jvm_memory_max_bytes{pod_name=\"$hostName\"}")
                                       .build();
    Map<String, String> invalidFields =
        PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2), false);
    assertThat(invalidFields).hasSize(1);
    assertThat(invalidFields.get("Invalid metrics for group: t1"))
        .isEqualTo("t1 has error metrics [m1, m2] and/or response time metrics [] but no throughput metrics.");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testOnlyResponseTime() {
    final TimeSeries timeSeries1 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m1")
                                       .metricType(MetricType.RESP_TIME.name())
                                       .url("jvm_memory_max_bytes{pod_name=\"$hostName\"}")
                                       .build();
    final TimeSeries timeSeries2 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m2")
                                       .metricType(MetricType.RESP_TIME.name())
                                       .url("jvm_memory_max_bytes{pod_name=\"$hostName\"}")
                                       .build();
    Map<String, String> invalidFields =
        PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2), false);
    assertThat(invalidFields).hasSize(1);
    assertThat(invalidFields.get("Invalid metrics for group: t1"))
        .isEqualTo("t1 has error metrics [] and/or response time metrics [m1, m2] but no throughput metrics.");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testMultipleThroughPuts() {
    final TimeSeries timeSeries1 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m1")
                                       .metricType(MetricType.ERROR.name())
                                       .url("jvm_memory_max_bytes{pod_name=\"$hostName\"}")
                                       .build();
    final TimeSeries timeSeries2 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m2")
                                       .metricType(MetricType.THROUGHPUT.name())
                                       .url("jvm_memory_max_bytes{pod_name=\"$hostName\"}")
                                       .build();
    final TimeSeries timeSeries3 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m3")
                                       .metricType(MetricType.THROUGHPUT.name())
                                       .url("jvm_memory_max_bytes{pod_name=\"$hostName\"}")
                                       .build();
    Map<String, String> invalidFields =
        PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2, timeSeries3), false);
    assertThat(invalidFields).hasSize(1);
    assertThat(invalidFields.get("Invalid metrics for group: t1"))
        .isEqualTo("t1 has more than one throughput metrics [m2, m3] defined.");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidError() {
    final TimeSeries timeSeries1 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m1")
                                       .metricType(MetricType.ERROR.name())
                                       .url("jvm_memory_max_bytes{pod_name=\"$hostName\"}")
                                       .build();
    final TimeSeries timeSeries2 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m2")
                                       .metricType(MetricType.ERROR.name())
                                       .url("jvm_memory_max_bytes{pod_name=\"$hostName\"}")
                                       .build();
    final TimeSeries timeSeries3 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m3")
                                       .metricType(MetricType.THROUGHPUT.name())
                                       .url("jvm_memory_max_bytes{pod_name=\"$hostName\"}")
                                       .build();
    Map<String, String> invalidFields =
        PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2, timeSeries3), false);
    assertThat(invalidFields.isEmpty()).isTrue();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidResponseTime() {
    final TimeSeries timeSeries1 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m1")
                                       .metricType(MetricType.RESP_TIME.name())
                                       .url("jvm_memory_max_bytes{pod_name=\"$hostName\"}")
                                       .build();
    final TimeSeries timeSeries2 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m2")
                                       .metricType(MetricType.ERROR.name())
                                       .url("jvm_memory_max_bytes{pod_name=\"$hostName\"}")
                                       .build();
    final TimeSeries timeSeries3 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m3")
                                       .metricType(MetricType.THROUGHPUT.name())
                                       .url("jvm_memory_max_bytes{pod_name=\"$hostName\"}")
                                       .build();
    Map<String, String> invalidFields =
        PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2, timeSeries3), false);
    assertThat(invalidFields.isEmpty()).isTrue();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidNonError() {
    final TimeSeries timeSeries1 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m1")
                                       .metricType(MetricType.INFRA.name())
                                       .url("jvm_memory_max_bytes{pod_name=\"$hostName\"}")
                                       .build();
    final TimeSeries timeSeries2 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m2")
                                       .metricType(MetricType.INFRA.name())
                                       .url("jvm_memory_max_bytes{pod_name=\"$hostName\"}")
                                       .build();
    final TimeSeries timeSeries3 = TimeSeries.builder()
                                       .txnName("t1")
                                       .metricName("m3")
                                       .metricType(MetricType.INFRA.name())
                                       .url("jvm_memory_max_bytes{pod_name=\"$hostName\"}")
                                       .build();
    Map<String, String> invalidFields =
        PrometheusResource.validateTransactions(Lists.newArrayList(timeSeries1, timeSeries2, timeSeries3), false);
    assertThat(invalidFields.isEmpty()).isTrue();
  }
}
