/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.cvng.api.CVConfigTransformerTestBase;
import io.harness.cvng.core.beans.AppDynamicsDSConfig;
import io.harness.cvng.core.beans.AppDynamicsDSConfig.ServiceMapping;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.rule.Owner;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AppDynamicsCVConfigTransformerTest extends CVConfigTransformerTestBase {
  private Set<MetricPack> metricPacks;
  private Set<ServiceMapping> serviceMappings;
  @Inject private AppDynamicsCVConfigTransformer appDynamicsCVConfigTransformer;
  @Before
  public void setup() {
    super.setUp();
    metricPacks = Sets.newHashSet(MetricPack.builder().identifier("metric pack1").build(),
        MetricPack.builder().identifier("metric pack2").build());
    serviceMappings = Sets.newHashSet(ServiceMapping.builder().serviceIdentifier("service1").tierName("tier1").build(),
        ServiceMapping.builder().serviceIdentifier("service2").tierName("tier2").build());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void transformToDSConfig_precondition() {
    assertThatThrownBy(() -> appDynamicsCVConfigTransformer.transform(Collections.emptyList()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("List of cvConfigs can not empty");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void transformToDSConfig_withAppDCVConfigForASingleApp() {
    AppDynamicsDSConfig appDynamicsDSConfig = appDynamicsCVConfigTransformer.transform(createCVConfigForAGroup("app1"));
    assertThat(appDynamicsDSConfig.getAccountId()).isEqualTo(accountId);
    assertThat(appDynamicsDSConfig.getAppConfigs().size()).isEqualTo(1);
    appDynamicsDSConfig.getAppConfigs().forEach(appdynamicsAppConfig -> {
      assertThat(appdynamicsAppConfig.getMetricPacks()).isEqualTo(metricPacks);
      assertThat(appdynamicsAppConfig.getServiceMappings()).isEqualTo(serviceMappings);
    });
  }

  private List<AppDynamicsCVConfig> createCVConfigForAGroup(String applicationName) {
    List<AppDynamicsCVConfig> appDynamicsCVConfigs = new ArrayList<>();
    metricPacks.forEach(metricPack -> serviceMappings.forEach(serviceMapping -> {
      AppDynamicsCVConfig appDynamicsCVConfig = new AppDynamicsCVConfig();
      fillCommonFields(appDynamicsCVConfig);
      appDynamicsCVConfig.setIdentifier(applicationName);
      appDynamicsCVConfig.setMonitoringSourceName(generateUuid());
      appDynamicsCVConfig.setTierName(serviceMapping.getTierName());
      appDynamicsCVConfig.setEnvIdentifier(envIdentifier);
      appDynamicsCVConfig.setServiceIdentifier(serviceMapping.getServiceIdentifier());
      appDynamicsCVConfig.setApplicationName(applicationName);
      appDynamicsCVConfig.setMetricPack(metricPack);
      appDynamicsCVConfigs.add(appDynamicsCVConfig);
    }));
    return appDynamicsCVConfigs;
  }
}
