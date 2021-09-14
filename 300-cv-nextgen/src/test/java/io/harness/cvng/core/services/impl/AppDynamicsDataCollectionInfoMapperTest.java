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

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.AppDynamicsDataCollectionInfo;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AppDynamicsDataCollectionInfoMapperTest extends CvNextGenTestBase {
  @Inject private AppDynamicsDataCollectionInfoMapper mapper;
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testToDataConnectionInfo() {
    MetricPack metricPack = MetricPack.builder().dataCollectionDsl("metric-pack-dsl").build();
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setUuid(generateUuid());
    cvConfig.setAccountId(generateUuid());
    cvConfig.setApplicationName("cv-app");
    cvConfig.setMetricPack(metricPack);
    cvConfig.setTierName("docker-tier");
    AppDynamicsDataCollectionInfo appDynamicsDataCollectionInfo = mapper.toDataCollectionInfo(cvConfig);
    assertThat(appDynamicsDataCollectionInfo.getMetricPack()).isEqualTo(metricPack.toDTO());
    assertThat(appDynamicsDataCollectionInfo.getApplicationName()).isEqualTo("cv-app");
    assertThat(appDynamicsDataCollectionInfo.getTierName()).isEqualTo("docker-tier");
    assertThat(appDynamicsDataCollectionInfo.getDataCollectionDsl()).isEqualTo("metric-pack-dsl");
  }
}
