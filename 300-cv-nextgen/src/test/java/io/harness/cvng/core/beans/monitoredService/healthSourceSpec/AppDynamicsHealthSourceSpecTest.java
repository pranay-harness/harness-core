/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans.monitoredService.healthSourceSpec;

import static io.harness.rule.OwnerRule.KANHAIYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.monitoredService.HealthSource.CVConfigUpdateResult;
import io.harness.cvng.core.beans.monitoredService.MetricPackDTO;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.AppDynamicsHealthSourceSpec;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppDynamicsHealthSourceSpecTest extends CvNextGenTestBase {
  AppDynamicsHealthSourceSpec appDynamicsHealthSourceSpec;
  @Inject MetricPackService metricPackService;
  String orgIdentifier;
  String projectIdentifier;
  String accountId;
  String applicationName;
  String tierName;
  String feature;
  String connectorIdentifier;
  String serviceIdentifier;
  String envIdentifier;
  String identifier;
  String name;
  List<MetricPackDTO> metricPackDTOS;
  BuilderFactory builderFactory;

  @Before
  public void setup() {
    builderFactory = BuilderFactory.getDefault();
    accountId = builderFactory.getContext().getAccountId();
    orgIdentifier = builderFactory.getContext().getOrgIdentifier();
    projectIdentifier = builderFactory.getContext().getProjectIdentifier();
    serviceIdentifier = builderFactory.getContext().getServiceIdentifier();
    envIdentifier = builderFactory.getContext().getEnvIdentifier();
    applicationName = "appName";
    tierName = "tierName";
    feature = "Application Monitoring";
    connectorIdentifier = "connectorRef";

    identifier = "identifier";
    name = "some-name";
    metricPackDTOS = Arrays.asList(MetricPackDTO.builder().identifier(CVMonitoringCategory.ERRORS).build());
    appDynamicsHealthSourceSpec = AppDynamicsHealthSourceSpec.builder()
                                      .applicationName(applicationName)
                                      .tierName(tierName)
                                      .connectorRef(connectorIdentifier)
                                      .feature(feature)
                                      .metricPacks(metricPackDTOS.stream().collect(Collectors.toSet()))
                                      .build();
    metricPackService.createDefaultMetricPackAndThresholds(accountId, orgIdentifier, projectIdentifier);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void getCVConfigUpdateResult_whenNoConfigExist() {
    CVConfigUpdateResult cvConfigUpdateResult =
        appDynamicsHealthSourceSpec.getCVConfigUpdateResult(accountId, orgIdentifier, projectIdentifier, envIdentifier,
            serviceIdentifier, identifier, name, Collections.emptyList(), metricPackService);
    assertThat(cvConfigUpdateResult.getUpdated()).isEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isEmpty();
    List<CVConfig> added = cvConfigUpdateResult.getAdded();

    List<AppDynamicsCVConfig> appDynamicsCVConfigs = (List<AppDynamicsCVConfig>) (List<?>) added;
    assertThat(appDynamicsCVConfigs).hasSize(1);
    AppDynamicsCVConfig appDynamicsCVConfig = appDynamicsCVConfigs.get(0);
    assertCommon(appDynamicsCVConfig);
    assertThat(appDynamicsCVConfig.getMetricPack().getCategory()).isEqualTo(CVMonitoringCategory.ERRORS);
    assertThat(appDynamicsCVConfig.getMetricPack().getMetrics().size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void getCVConfigUpdateResult_checkDeleted() {
    List<CVConfig> cvConfigs = new ArrayList<>();
    cvConfigs.add(
        createCVConfig(MetricPack.builder().accountId(accountId).category(CVMonitoringCategory.PERFORMANCE).build()));
    CVConfigUpdateResult result = appDynamicsHealthSourceSpec.getCVConfigUpdateResult(accountId, orgIdentifier,
        projectIdentifier, envIdentifier, serviceIdentifier, identifier, name, cvConfigs, metricPackService);
    assertThat(result.getDeleted()).hasSize(1);
    AppDynamicsCVConfig appDynamicsCVConfig = (AppDynamicsCVConfig) result.getDeleted().get(0);
    assertThat(appDynamicsCVConfig.getMetricPack().getCategory()).isEqualTo(CVMonitoringCategory.PERFORMANCE);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void getCVConfigUpdateResult_checkAdded() {
    List<CVConfig> cvConfigs = new ArrayList<>();
    cvConfigs.add(
        createCVConfig(MetricPack.builder().accountId(accountId).category(CVMonitoringCategory.PERFORMANCE).build()));
    CVConfigUpdateResult result = appDynamicsHealthSourceSpec.getCVConfigUpdateResult(accountId, orgIdentifier,
        projectIdentifier, envIdentifier, serviceIdentifier, identifier, name, cvConfigs, metricPackService);
    assertThat(result.getAdded()).hasSize(1);
    AppDynamicsCVConfig appDynamicsCVConfig = (AppDynamicsCVConfig) result.getAdded().get(0);
    assertCommon(appDynamicsCVConfig);
    assertThat(appDynamicsCVConfig.getMetricPack().getCategory()).isEqualTo(CVMonitoringCategory.ERRORS);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void getCVConfigUpdateResult_checkUpdated() {
    List<CVConfig> cvConfigs = new ArrayList<>();
    cvConfigs.add(createCVConfig(metricPackService.getMetricPack(
        accountId, orgIdentifier, projectIdentifier, DataSourceType.APP_DYNAMICS, CVMonitoringCategory.ERRORS)));
    CVConfigUpdateResult result = appDynamicsHealthSourceSpec.getCVConfigUpdateResult(accountId, orgIdentifier,
        projectIdentifier, envIdentifier, serviceIdentifier, identifier, name, cvConfigs, metricPackService);
    assertThat(result.getUpdated()).hasSize(1);
    AppDynamicsCVConfig appDynamicsCVConfig = (AppDynamicsCVConfig) result.getUpdated().get(0);
    assertCommon(appDynamicsCVConfig);
    assertThat(appDynamicsCVConfig.getMetricPack().getCategory()).isEqualTo(CVMonitoringCategory.ERRORS);
  }

  private void assertCommon(AppDynamicsCVConfig cvConfig) {
    assertThat(cvConfig.getAccountId()).isEqualTo(accountId);
    assertThat(cvConfig.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(cvConfig.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(cvConfig.getApplicationName()).isEqualTo(applicationName);
    assertThat(cvConfig.getTierName()).isEqualTo(tierName);
    assertThat(cvConfig.getConnectorIdentifier()).isEqualTo(connectorIdentifier);
    assertThat(cvConfig.getEnvIdentifier()).isEqualTo(envIdentifier);
    assertThat(cvConfig.getServiceIdentifier()).isEqualTo(serviceIdentifier);
    assertThat(cvConfig.getIdentifier()).isEqualTo(identifier);
    assertThat(cvConfig.getProductName()).isEqualTo(feature);
    assertThat(cvConfig.getMonitoringSourceName()).isEqualTo(name);
    assertThat(cvConfig.getMetricPack().getAccountId()).isEqualTo(accountId);
    assertThat(cvConfig.getMetricPack().getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(cvConfig.getMetricPack().getDataSourceType()).isEqualTo(DataSourceType.APP_DYNAMICS);
  }

  private CVConfig createCVConfig(MetricPack metricPack) {
    return builderFactory.appDynamicsCVConfigBuilder()
        .tierName(tierName)
        .applicationName(applicationName)
        .metricPack(metricPack)
        .connectorIdentifier(connectorIdentifier)
        .monitoringSourceName(name)
        .productName(feature)
        .identifier(identifier)
        .build();
  }
}
