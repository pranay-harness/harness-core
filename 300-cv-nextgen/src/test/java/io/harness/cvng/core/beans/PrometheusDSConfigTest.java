/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.beans.TimeSeriesThresholdType;
import io.harness.cvng.core.beans.DSConfig.CVConfigUpdateResult;
import io.harness.cvng.core.beans.PrometheusMetricDefinition.PrometheusFilter;
import io.harness.cvng.core.entities.PrometheusCVConfig;
import io.harness.cvng.core.entities.PrometheusCVConfig.MetricInfo;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PrometheusDSConfigTest extends DSConfigTestBase {
  private PrometheusDSConfig prometheusDSConfig;
  String serviceIdentifier;
  String envIdentifier;

  @Before
  public void setup() {
    prometheusDSConfig = new PrometheusDSConfig();
    fillCommonFields(prometheusDSConfig);
    envIdentifier = generateUuid();
    serviceIdentifier = generateUuid();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  @Ignore("We are moving away from DSConfig into HealthSource object")
  public void getCVConfigUpdateResult_whenNoConfigExists() {
    PrometheusMetricDefinition metricDefinition =
        PrometheusMetricDefinition.builder()
            .metricName("sampleMetric")
            .groupName("myMetricGroupName")
            .prometheusMetric("container.cpu.usage.total")
            .serviceFilter(
                Arrays.asList(PrometheusFilter.builder().labelName("namespace").labelValue("cv-demo").build()))
            .envFilter(Arrays.asList(PrometheusFilter.builder().labelName("container").labelValue("cv-demo").build()))
            .serviceIdentifier(serviceIdentifier)
            .envIdentifier(envIdentifier)
            .serviceInstanceFieldName("pod")
            .riskProfile(RiskProfile.builder()
                             .metricType(TimeSeriesMetricType.RESP_TIME)
                             .category(CVMonitoringCategory.PERFORMANCE)
                             .thresholdTypes(Arrays.asList(TimeSeriesThresholdType.ACT_WHEN_HIGHER))
                             .build())
            .build();
    prometheusDSConfig.setMetricDefinitions(Arrays.asList(metricDefinition));
    CVConfigUpdateResult cvConfigUpdateResult = prometheusDSConfig.getCVConfigUpdateResult(Collections.emptyList());

    assertThat(cvConfigUpdateResult).isNotNull();
    assertThat(cvConfigUpdateResult.getAdded()).isNotEmpty();
    assertThat(cvConfigUpdateResult.getUpdated()).isEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isEmpty();

    assertThat(cvConfigUpdateResult.getAdded().size()).isEqualTo(1);
    List<PrometheusCVConfig> cvConfigList = (List<PrometheusCVConfig>) (List<?>) cvConfigUpdateResult.getAdded();
    PrometheusCVConfig cvConfig = cvConfigList.get(0);
    assertThat(cvConfig.getGroupName()).isEqualTo("myMetricGroupName");
    assertThat(cvConfig.getServiceIdentifier()).isEqualTo(serviceIdentifier);
    assertThat(cvConfig.getEnvIdentifier()).isEqualTo(envIdentifier);
    assertThat(cvConfig.getMetricInfoList().size()).isEqualTo(1);
    MetricInfo metricInfo = cvConfig.getMetricInfoList().get(0);
    assertThat(metricInfo.getMetricName()).isEqualTo("sampleMetric");
    assertThat(metricInfo.getPrometheusMetricName()).isEqualTo(metricDefinition.getPrometheusMetric());
    assertThat(metricInfo.getEnvFilter()).isEqualTo(metricDefinition.getEnvFilter());
    assertThat(metricInfo.getServiceFilter()).isEqualTo(metricDefinition.getServiceFilter());
    assertThat(metricInfo.getServiceInstanceFieldName()).isEqualTo(metricDefinition.getServiceInstanceFieldName());
    assertThat(metricInfo.getAggregation()).isEqualTo(metricDefinition.getAggregation());
    assertThat(metricInfo.getAdditionalFilters()).isEqualTo(metricDefinition.getAdditionalFilters());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  @Ignore("We are moving away from DSConfig into HealthSource object")
  public void getCVConfigUpdateResult_whenNoConfigExists2ItemsSameGroup() {
    PrometheusMetricDefinition metricDefinition =
        PrometheusMetricDefinition.builder()
            .metricName("sampleMetric")
            .groupName("myMetricGroupName")
            .prometheusMetric("container.cpu.usage.total")
            .serviceFilter(
                Arrays.asList(PrometheusFilter.builder().labelName("namespace").labelValue("cv-demo").build()))
            .envFilter(Arrays.asList(PrometheusFilter.builder().labelName("container").labelValue("cv-demo").build()))
            .serviceIdentifier(serviceIdentifier)
            .envIdentifier(envIdentifier)
            .serviceInstanceFieldName("pod")
            .riskProfile(RiskProfile.builder()
                             .metricType(TimeSeriesMetricType.RESP_TIME)
                             .category(CVMonitoringCategory.PERFORMANCE)
                             .thresholdTypes(Arrays.asList(TimeSeriesThresholdType.ACT_WHEN_HIGHER))
                             .build())
            .build();

    PrometheusMetricDefinition metricDefinition2 =
        PrometheusMetricDefinition.builder()
            .metricName("sampleMetric2")
            .groupName("myMetricGroupName")
            .prometheusMetric("container.cpu.usage")
            .serviceFilter(
                Arrays.asList(PrometheusFilter.builder().labelName("namespace").labelValue("cv-demo").build()))
            .envFilter(Arrays.asList(PrometheusFilter.builder().labelName("container").labelValue("cv-demo").build()))
            .serviceIdentifier(serviceIdentifier)
            .envIdentifier(envIdentifier)
            .serviceInstanceFieldName("pod")
            .riskProfile(RiskProfile.builder()
                             .metricType(TimeSeriesMetricType.THROUGHPUT)
                             .category(CVMonitoringCategory.PERFORMANCE)
                             .thresholdTypes(Arrays.asList(TimeSeriesThresholdType.ACT_WHEN_HIGHER))
                             .build())
            .build();
    prometheusDSConfig.setMetricDefinitions(Arrays.asList(metricDefinition, metricDefinition2));
    CVConfigUpdateResult cvConfigUpdateResult = prometheusDSConfig.getCVConfigUpdateResult(Collections.emptyList());

    assertThat(cvConfigUpdateResult).isNotNull();
    assertThat(cvConfigUpdateResult.getAdded()).isNotEmpty();
    assertThat(cvConfigUpdateResult.getUpdated()).isEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isEmpty();

    assertThat(cvConfigUpdateResult.getAdded().size()).isEqualTo(1);
    List<PrometheusCVConfig> cvConfigList = (List<PrometheusCVConfig>) (List<?>) cvConfigUpdateResult.getAdded();
    PrometheusCVConfig cvConfig = cvConfigList.get(0);
    assertThat(cvConfig.getGroupName()).isEqualTo("myMetricGroupName");
    assertThat(cvConfig.getServiceIdentifier()).isEqualTo(serviceIdentifier);
    assertThat(cvConfig.getEnvIdentifier()).isEqualTo(envIdentifier);
    assertThat(cvConfig.getCategory().name()).isEqualTo(CVMonitoringCategory.PERFORMANCE.name());
    assertThat(cvConfig.getMetricInfoList().size()).isEqualTo(2);
    MetricInfo metricInfo = cvConfig.getMetricInfoList().get(0);
    assertThat(metricInfo.getMetricName()).isEqualTo("sampleMetric");
    assertThat(metricInfo.getPrometheusMetricName()).isEqualTo(metricDefinition.getPrometheusMetric());
    assertThat(metricInfo.getEnvFilter()).isEqualTo(metricDefinition.getEnvFilter());
    assertThat(metricInfo.getServiceFilter()).isEqualTo(metricDefinition.getServiceFilter());
    assertThat(metricInfo.getServiceInstanceFieldName()).isEqualTo(metricDefinition.getServiceInstanceFieldName());
    assertThat(metricInfo.getAggregation()).isEqualTo(metricDefinition.getAggregation());
    assertThat(metricInfo.getAdditionalFilters()).isEqualTo(metricDefinition.getAdditionalFilters());

    metricInfo = cvConfig.getMetricInfoList().get(1);
    assertThat(metricInfo.getMetricName()).isEqualTo(metricDefinition2.getMetricName());
    assertThat(metricInfo.getPrometheusMetricName()).isEqualTo(metricDefinition2.getPrometheusMetric());
    assertThat(metricInfo.getEnvFilter()).isEqualTo(metricDefinition2.getEnvFilter());
    assertThat(metricInfo.getServiceFilter()).isEqualTo(metricDefinition2.getServiceFilter());
    assertThat(metricInfo.getServiceInstanceFieldName()).isEqualTo(metricDefinition2.getServiceInstanceFieldName());
    assertThat(metricInfo.getAggregation()).isEqualTo(metricDefinition2.getAggregation());
    assertThat(metricInfo.getAdditionalFilters()).isEqualTo(metricDefinition2.getAdditionalFilters());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  @Ignore("We are moving away from DSConfig into HealthSource object")
  public void getCVConfigUpdateResult_whenUpdated() {
    PrometheusCVConfig cvConfig = PrometheusCVConfig.builder().groupName("groupName").build();
    cvConfig.setEnvIdentifier(envIdentifier);
    cvConfig.setServiceIdentifier(serviceIdentifier);
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);

    PrometheusMetricDefinition metricDefinition2 =
        PrometheusMetricDefinition.builder()
            .metricName("sampleMetric2")
            .groupName("groupName")
            .prometheusMetric("container.cpu.usage")
            .serviceFilter(
                Arrays.asList(PrometheusFilter.builder().labelName("namespace").labelValue("cv-demo").build()))
            .envFilter(Arrays.asList(PrometheusFilter.builder().labelName("container").labelValue("cv-demo").build()))
            .serviceIdentifier(serviceIdentifier)
            .envIdentifier(envIdentifier)
            .serviceInstanceFieldName("pod")
            .riskProfile(RiskProfile.builder()
                             .metricType(TimeSeriesMetricType.THROUGHPUT)
                             .category(CVMonitoringCategory.PERFORMANCE)
                             .thresholdTypes(Arrays.asList(TimeSeriesThresholdType.ACT_WHEN_HIGHER))
                             .build())
            .build();
    prometheusDSConfig.setMetricDefinitions(Arrays.asList(metricDefinition2));

    CVConfigUpdateResult cvConfigUpdateResult = prometheusDSConfig.getCVConfigUpdateResult(Arrays.asList(cvConfig));

    assertThat(cvConfigUpdateResult).isNotNull();
    assertThat(cvConfigUpdateResult.getAdded()).isEmpty();
    assertThat(cvConfigUpdateResult.getUpdated()).isNotEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isEmpty();

    assertThat(cvConfigUpdateResult.getUpdated().size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  @Ignore("We are moving away from DSConfig into HealthSource object")
  public void getCVConfigUpdateResult_whenDeleted() {
    PrometheusCVConfig cvConfig = PrometheusCVConfig.builder().groupName("groupName").build();
    cvConfig.setEnvIdentifier(envIdentifier);
    cvConfig.setServiceIdentifier(serviceIdentifier);
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);

    PrometheusMetricDefinition metricDefinition2 =
        PrometheusMetricDefinition.builder()
            .metricName("sampleMetric2")
            .groupName("groupNameNew")
            .prometheusMetric("container.cpu.usage")
            .serviceFilter(
                Arrays.asList(PrometheusFilter.builder().labelName("namespace").labelValue("cv-demo").build()))
            .envFilter(Arrays.asList(PrometheusFilter.builder().labelName("container").labelValue("cv-demo").build()))
            .serviceIdentifier(serviceIdentifier)
            .envIdentifier(envIdentifier)
            .serviceInstanceFieldName("pod")
            .riskProfile(RiskProfile.builder()
                             .metricType(TimeSeriesMetricType.THROUGHPUT)
                             .category(CVMonitoringCategory.PERFORMANCE)
                             .thresholdTypes(Arrays.asList(TimeSeriesThresholdType.ACT_WHEN_HIGHER))
                             .build())
            .build();
    prometheusDSConfig.setMetricDefinitions(Arrays.asList(metricDefinition2));

    CVConfigUpdateResult cvConfigUpdateResult = prometheusDSConfig.getCVConfigUpdateResult(Arrays.asList(cvConfig));

    assertThat(cvConfigUpdateResult).isNotNull();
    assertThat(cvConfigUpdateResult.getAdded()).isNotEmpty();
    assertThat(cvConfigUpdateResult.getUpdated()).isEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isNotEmpty();

    assertThat(cvConfigUpdateResult.getDeleted().size()).isEqualTo(1);
  }
}
