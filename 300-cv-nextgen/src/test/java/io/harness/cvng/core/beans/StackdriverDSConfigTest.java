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
import io.harness.cvng.core.beans.StackdriverDSConfig.StackdriverConfiguration;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.StackdriverCVConfig;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class StackdriverDSConfigTest extends DSConfigTestBase {
  StackdriverDSConfig stackdriverDSConfig;
  private List<MetricPack> metricPacks;
  String serviceIdentifier;
  String envIdentifier;

  @Before
  public void setup() {
    stackdriverDSConfig = new StackdriverDSConfig();
    fillCommonFields(stackdriverDSConfig);
    envIdentifier = generateUuid();
    serviceIdentifier = generateUuid();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void getCVConfigUpdateResult_whenNoConfigExists() {
    StackdriverConfiguration configuration =
        StackdriverConfiguration.builder()
            .serviceIdentifier(serviceIdentifier)
            .envIdentifier(envIdentifier)
            .metricDefinition(
                StackdriverDefinition.builder()
                    .jsonMetricDefinition("{\"sampleJson\":[]}")
                    .metricName("metricName")
                    .dashboardName("dashboardName")
                    .metricTags(Arrays.asList("tag1"))
                    .riskProfile(RiskProfile.builder()
                                     .metricType(TimeSeriesMetricType.RESP_TIME)
                                     .category(CVMonitoringCategory.PERFORMANCE)
                                     .thresholdTypes(Arrays.asList(TimeSeriesThresholdType.ACT_WHEN_HIGHER))
                                     .build())
                    .build())
            .build();

    stackdriverDSConfig.setMetricConfigurations(Arrays.asList(configuration));

    CVConfigUpdateResult cvConfigUpdateResult = stackdriverDSConfig.getCVConfigUpdateResult(Collections.emptyList());

    assertThat(cvConfigUpdateResult).isNotNull();
    assertThat(cvConfigUpdateResult.getAdded()).isNotEmpty();
    assertThat(cvConfigUpdateResult.getUpdated()).isEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isEmpty();

    assertThat(cvConfigUpdateResult.getAdded().size()).isEqualTo(1);

    List<StackdriverCVConfig> cvConfigs = (List<StackdriverCVConfig>) (List<?>) cvConfigUpdateResult.getAdded();
    cvConfigs.forEach(cvConfig -> {
      assertThat(cvConfig.getDashboardName()).isEqualTo("dashboardName");
      assertThat(cvConfig.getServiceIdentifier()).isEqualTo(serviceIdentifier);
      assertThat(cvConfig.getCategory().name()).isEqualTo(CVMonitoringCategory.PERFORMANCE.name());
    });
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void getCVConfigUpdateResult_whenNoConfigExistsAddMultiple() {
    StackdriverConfiguration configuration =
        StackdriverConfiguration.builder()
            .serviceIdentifier(serviceIdentifier)
            .envIdentifier(envIdentifier)
            .metricDefinition(
                StackdriverDefinition.builder()
                    .jsonMetricDefinition("{\"sampleJson\":[]}")
                    .metricName("metricName")
                    .dashboardName("dashboardName")
                    .metricTags(Arrays.asList("tag1"))
                    .riskProfile(RiskProfile.builder()
                                     .metricType(TimeSeriesMetricType.RESP_TIME)
                                     .category(CVMonitoringCategory.PERFORMANCE)
                                     .thresholdTypes(Arrays.asList(TimeSeriesThresholdType.ACT_WHEN_HIGHER))
                                     .build())
                    .build())
            .build();
    StackdriverConfiguration configuration2 =
        StackdriverConfiguration.builder()
            .serviceIdentifier(serviceIdentifier + "2")
            .envIdentifier(envIdentifier)
            .metricDefinition(
                StackdriverDefinition.builder()
                    .jsonMetricDefinition("{\"sampleJson\":[]}")
                    .metricName("metricName")
                    .dashboardName("dashboardName")
                    .metricTags(Arrays.asList("tag1"))
                    .riskProfile(RiskProfile.builder()
                                     .metricType(TimeSeriesMetricType.RESP_TIME)
                                     .category(CVMonitoringCategory.PERFORMANCE)
                                     .thresholdTypes(Arrays.asList(TimeSeriesThresholdType.ACT_WHEN_HIGHER))
                                     .build())
                    .build())
            .build();

    stackdriverDSConfig.setMetricConfigurations(Arrays.asList(configuration, configuration2));

    CVConfigUpdateResult cvConfigUpdateResult = stackdriverDSConfig.getCVConfigUpdateResult(Collections.emptyList());

    assertThat(cvConfigUpdateResult).isNotNull();
    assertThat(cvConfigUpdateResult.getAdded()).isNotEmpty();
    assertThat(cvConfigUpdateResult.getUpdated()).isEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isEmpty();

    assertThat(cvConfigUpdateResult.getAdded().size()).isEqualTo(2);

    boolean service1Present = false;
    boolean service2Present = false;

    List<StackdriverCVConfig> cvConfigs = (List<StackdriverCVConfig>) (List<?>) cvConfigUpdateResult.getAdded();
    for (StackdriverCVConfig cvConfig : cvConfigs) {
      assertThat(cvConfig.getDashboardName()).isEqualTo("dashboardName");
      if (cvConfig.getServiceIdentifier().equals(serviceIdentifier)) {
        service1Present = true;
      } else if (cvConfig.getServiceIdentifier().equals(serviceIdentifier + "2")) {
        service2Present = true;
      }
      assertThat(cvConfig.getCategory().name()).isEqualTo(CVMonitoringCategory.PERFORMANCE.name());
    }
    assertThat(service1Present && service2Present).isTrue();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void getCVConfigUpdateResult_whenDeleted() {
    StackdriverConfiguration configuration =
        StackdriverConfiguration.builder()
            .serviceIdentifier(serviceIdentifier)
            .envIdentifier(envIdentifier)
            .metricDefinition(
                StackdriverDefinition.builder()
                    .jsonMetricDefinition("{\"sampleJson\":[]}")
                    .metricName("metricName")
                    .dashboardName("dashboardName")
                    .metricTags(Arrays.asList("tag1"))
                    .riskProfile(RiskProfile.builder()
                                     .metricType(TimeSeriesMetricType.RESP_TIME)
                                     .category(CVMonitoringCategory.PERFORMANCE)
                                     .thresholdTypes(Arrays.asList(TimeSeriesThresholdType.ACT_WHEN_HIGHER))
                                     .build())
                    .build())
            .build();
    stackdriverDSConfig.setMetricConfigurations(Arrays.asList(configuration));

    StackdriverCVConfig existingCVConfig = StackdriverCVConfig.builder().dashboardName("dashboard2").build();
    existingCVConfig.setEnvIdentifier(envIdentifier);
    existingCVConfig.setServiceIdentifier(serviceIdentifier);
    existingCVConfig.setMetricPack(MetricPack.builder().category(CVMonitoringCategory.PERFORMANCE).build());

    CVConfigUpdateResult cvConfigUpdateResult =
        stackdriverDSConfig.getCVConfigUpdateResult(Arrays.asList(existingCVConfig));

    assertThat(cvConfigUpdateResult).isNotNull();
    assertThat(cvConfigUpdateResult.getAdded()).isNotEmpty();
    assertThat(cvConfigUpdateResult.getUpdated()).isEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isNotEmpty();

    assertThat(cvConfigUpdateResult.getAdded().size()).isEqualTo(1);
    assertThat(cvConfigUpdateResult.getDeleted().size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void getCVConfigUpdateResult_whenUpdated() {
    StackdriverConfiguration configuration =
        StackdriverConfiguration.builder()
            .serviceIdentifier(serviceIdentifier)
            .envIdentifier(envIdentifier)
            .metricDefinition(
                StackdriverDefinition.builder()
                    .jsonMetricDefinition("{\"sampleJson\":[]}")
                    .metricName("metricName")
                    .dashboardName("dashboardName")
                    .metricTags(Arrays.asList("tag1"))
                    .riskProfile(RiskProfile.builder()
                                     .metricType(TimeSeriesMetricType.RESP_TIME)
                                     .category(CVMonitoringCategory.PERFORMANCE)
                                     .thresholdTypes(Arrays.asList(TimeSeriesThresholdType.ACT_WHEN_HIGHER))
                                     .build())
                    .build())
            .build();
    stackdriverDSConfig.setMetricConfigurations(Arrays.asList(configuration));

    StackdriverCVConfig existingCVConfig = StackdriverCVConfig.builder().dashboardName("dashboardName").build();
    existingCVConfig.setEnvIdentifier(envIdentifier);
    existingCVConfig.setServiceIdentifier(serviceIdentifier);
    existingCVConfig.setMetricPack(MetricPack.builder().category(CVMonitoringCategory.PERFORMANCE).build());

    CVConfigUpdateResult cvConfigUpdateResult =
        stackdriverDSConfig.getCVConfigUpdateResult(Arrays.asList(existingCVConfig));

    assertThat(cvConfigUpdateResult).isNotNull();
    assertThat(cvConfigUpdateResult.getAdded()).isEmpty();
    assertThat(cvConfigUpdateResult.getUpdated()).isNotEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isEmpty();

    assertThat(cvConfigUpdateResult.getUpdated().size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void getCVConfigUpdateResult_whenAddedWithExising() {
    StackdriverConfiguration configuration =
        StackdriverConfiguration.builder()
            .serviceIdentifier(serviceIdentifier)
            .envIdentifier(envIdentifier)
            .metricDefinition(
                StackdriverDefinition.builder()
                    .jsonMetricDefinition("{\"sampleJson\":[]}")
                    .metricName("metricName")
                    .dashboardName("dashboardName")
                    .metricTags(Arrays.asList("tag1"))
                    .riskProfile(RiskProfile.builder()
                                     .metricType(TimeSeriesMetricType.RESP_TIME)
                                     .category(CVMonitoringCategory.PERFORMANCE)
                                     .thresholdTypes(Arrays.asList(TimeSeriesThresholdType.ACT_WHEN_HIGHER))
                                     .build())
                    .build())
            .build();
    StackdriverConfiguration configuration2 =
        StackdriverConfiguration.builder()
            .serviceIdentifier(serviceIdentifier)
            .envIdentifier(envIdentifier)
            .metricDefinition(
                StackdriverDefinition.builder()
                    .jsonMetricDefinition("{\"sampleJson\":[]}")
                    .metricName("metricName")
                    .dashboardName("dashboardName2")
                    .metricTags(Arrays.asList("tag2"))
                    .riskProfile(RiskProfile.builder()
                                     .metricType(TimeSeriesMetricType.RESP_TIME)
                                     .category(CVMonitoringCategory.PERFORMANCE)
                                     .thresholdTypes(Arrays.asList(TimeSeriesThresholdType.ACT_WHEN_HIGHER))
                                     .build())
                    .build())
            .build();
    stackdriverDSConfig.setMetricConfigurations(Arrays.asList(configuration, configuration2));

    StackdriverCVConfig existingCVConfig = StackdriverCVConfig.builder().dashboardName("dashboardName2").build();
    existingCVConfig.setEnvIdentifier(envIdentifier);
    existingCVConfig.setServiceIdentifier(serviceIdentifier);
    existingCVConfig.setMetricPack(MetricPack.builder().category(CVMonitoringCategory.PERFORMANCE).build());

    CVConfigUpdateResult cvConfigUpdateResult =
        stackdriverDSConfig.getCVConfigUpdateResult(Arrays.asList(existingCVConfig));

    assertThat(cvConfigUpdateResult).isNotNull();
    assertThat(cvConfigUpdateResult.getAdded()).isNotEmpty();
    assertThat(cvConfigUpdateResult.getUpdated()).isNotEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isEmpty();

    assertThat(cvConfigUpdateResult.getUpdated().size()).isEqualTo(1);
    assertThat(cvConfigUpdateResult.getAdded().size()).isEqualTo(1);
  }
}
