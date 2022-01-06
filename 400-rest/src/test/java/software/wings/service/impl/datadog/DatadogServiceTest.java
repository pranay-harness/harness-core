/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.datadog;

import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.service.intfc.datadog.DatadogService;
import software.wings.sm.states.DatadogState;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DatadogServiceTest extends CategoryTest {
  private static final SecureRandom random = new SecureRandom();

  DatadogService datadogService = new DatadogServiceImpl();

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testValidateCustomFields_sameMetricName() {
    Map<String, Set<DatadogState.Metric>> customMetricMap = new HashMap<>();

    Set<DatadogState.Metric> metrics = getMetricsSet(Arrays.asList("INFRA"));
    metrics.iterator().next().setMetricName("docker.mem.rss");
    customMetricMap.put("pod_name", metrics);

    String metriList = "docker.mem.rss,kubernetes.cpu.usage.total";

    Map<String, String> invalidFields = DatadogServiceImpl.validateNameClashInCustomMetrics(customMetricMap, metriList);
    assertThat(invalidFields).hasSize(1);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testValidateCustomFields_differentMetricName() {
    Map<String, Set<DatadogState.Metric>> customMetricMap = new HashMap<>();

    Set<DatadogState.Metric> metrics = getMetricsSet(Arrays.asList("INFRA"));
    metrics.iterator().next().setMetricName("docker.cpu.res");
    customMetricMap.put("pod_name", metrics);

    String metriList = "docker.mem.rss,kubernetes.cpu.usage.total";

    Map<String, String> invalidFields = DatadogServiceImpl.validateNameClashInCustomMetrics(customMetricMap, metriList);
    assertThat(invalidFields).hasSize(0);
  }

  private Set<DatadogState.Metric> getMetricsSet(List<String> metricType) {
    Set<DatadogState.Metric> metrics = new HashSet<>();
    int i = 0;
    metricType.forEach(type -> {
      metrics.add(DatadogState.Metric.builder()
                      .txnName("transaction1")
                      .displayName("display23" + random.nextInt())
                      .mlMetricType(type)
                      .metricName("test.metric3" + random.nextInt())
                      .datadogMetricType("Custom")
                      .build());
    });

    return metrics;
  }
}
