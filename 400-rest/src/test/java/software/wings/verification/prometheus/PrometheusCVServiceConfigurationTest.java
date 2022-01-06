/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.verification.prometheus;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.metrics.MetricType;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.sm.StateType;
import software.wings.verification.ServiceGuardThroughputToErrorsMap;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class PrometheusCVServiceConfigurationTest extends WingsBaseTest {
  private static final String configName = "configName";
  private static final String accountId = "accountId";
  private static final String connectorId = "connectorId";
  private static final String envId = "envId";
  private static final String serviceId = "serviceId";
  private static final StateType stateType = StateType.DYNA_TRACE;

  private List<TimeSeries> getTimeSeries() {
    return Lists.newArrayList(TimeSeries.builder().metricName("metric1").metricType("type1").build(),
        TimeSeries.builder().metricName("metric2").metricType("type2").build());
  }

  private PrometheusCVServiceConfiguration createPrometheusConfig() {
    PrometheusCVServiceConfiguration config = new PrometheusCVServiceConfiguration();
    config.setName(configName);
    config.setAccountId(accountId);
    config.setConnectorId(connectorId);
    config.setEnvId(envId);
    config.setServiceId(serviceId);
    config.setStateType(stateType);
    config.setEnabled24x7(true);

    config.setTimeSeriesToAnalyze(getTimeSeries());

    return config;
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testDeepCopy_prometheusConfig() {
    PrometheusCVServiceConfiguration config = createPrometheusConfig();

    PrometheusCVServiceConfiguration clonedConfig = (PrometheusCVServiceConfiguration) config.deepCopy();

    assertThat(clonedConfig.getName()).isEqualTo(configName);
    assertThat(clonedConfig.getAccountId()).isEqualTo(accountId);
    assertThat(clonedConfig.getConnectorId()).isEqualTo(connectorId);
    assertThat(clonedConfig.getEnvId()).isEqualTo(envId);
    assertThat(clonedConfig.getServiceId()).isEqualTo(serviceId);
    assertThat(clonedConfig.getStateType()).isEqualTo(stateType);
    assertThat(clonedConfig.isEnabled24x7()).isTrue();

    assertThat(new HashSet<>(clonedConfig.getTimeSeriesToAnalyze())).isEqualTo(new HashSet<>(getTimeSeries()));
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetThroughputToErrorsMap_emptyTimeSeries() {
    PrometheusCVServiceConfiguration prometheusCVServiceConfiguration =
        PrometheusCVServiceConfiguration.builder().timeSeriesToAnalyze(Lists.newArrayList()).build();
    assertThat(prometheusCVServiceConfiguration.getThroughputToErrors().isEmpty()).isTrue();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetThroughputToErrorsMap_whenNoThroughputAndError() {
    PrometheusCVServiceConfiguration prometheusCVServiceConfiguration =
        PrometheusCVServiceConfiguration.builder()
            .timeSeriesToAnalyze(Lists.newArrayList(TimeSeries.builder()
                                                        .url(generateUuid())
                                                        .metricType(MetricType.INFRA.name())
                                                        .metricName(generateUuid())
                                                        .txnName(generateUuid())
                                                        .build()))
            .build();
    assertThat(prometheusCVServiceConfiguration.getThroughputToErrors().isEmpty()).isTrue();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetThroughputToErrorsMap_whenThroughputAndError() {
    PrometheusCVServiceConfiguration prometheusCVServiceConfiguration =
        PrometheusCVServiceConfiguration.builder()
            .timeSeriesToAnalyze(Lists.newArrayList(TimeSeries.builder()
                                                        .url(generateUuid())
                                                        .metricType(MetricType.THROUGHPUT.name())
                                                        .txnName("txn1")
                                                        .metricName("metric1")
                                                        .build(),
                TimeSeries.builder()
                    .url(generateUuid())
                    .metricType(MetricType.ERROR.name())
                    .txnName("txn1")
                    .metricName("metric2")
                    .build(),
                TimeSeries.builder()
                    .url(generateUuid())
                    .metricType(MetricType.ERROR.name())
                    .txnName("txn1")
                    .metricName("metric3")
                    .build(),
                TimeSeries.builder()
                    .url(generateUuid())
                    .metricType(MetricType.RESP_TIME.name())
                    .txnName("txn1")
                    .metricName("metric4")
                    .build(),
                TimeSeries.builder()
                    .url(generateUuid())
                    .metricType(MetricType.THROUGHPUT.name())
                    .txnName("txn2")
                    .metricName("metric1")
                    .build(),
                TimeSeries.builder()
                    .url(generateUuid())
                    .metricType(MetricType.ERROR.name())
                    .txnName("txn2")
                    .metricName("metric2")
                    .build(),
                TimeSeries.builder()
                    .url(generateUuid())
                    .metricType(MetricType.ERROR.name())
                    .txnName("txn2")
                    .metricName("metric3")
                    .build()))
            .build();
    final List<ServiceGuardThroughputToErrorsMap> throughputToErrors =
        prometheusCVServiceConfiguration.getThroughputToErrors();
    assertThat(throughputToErrors.isEmpty()).isFalse();

    List<ServiceGuardThroughputToErrorsMap> expected = new ArrayList<>();
    expected.add(ServiceGuardThroughputToErrorsMap.builder()
                     .txnName("txn1")
                     .throughputMetric("metric1")
                     .errorMetrics(Lists.newArrayList("metric2", "metric3"))
                     .build());

    expected.add(ServiceGuardThroughputToErrorsMap.builder()
                     .txnName("txn2")
                     .throughputMetric("metric1")
                     .errorMetrics(Lists.newArrayList("metric2", "metric3"))
                     .build());

    assertThat(throughputToErrors).isEqualTo(expected);
  }
}
