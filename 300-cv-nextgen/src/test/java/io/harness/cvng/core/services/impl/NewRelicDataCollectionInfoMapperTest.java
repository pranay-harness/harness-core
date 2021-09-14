/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.NewRelicDataCollectionInfo;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.NewRelicCVConfig;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NewRelicDataCollectionInfoMapperTest extends CvNextGenTestBase {
  @Inject private NewRelicDataCollectionInfoMapper mapper;
  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testToDataConnectionInfo() {
    MetricPack metricPack = MetricPack.builder().dataCollectionDsl("metric-pack-dsl").build();
    NewRelicCVConfig cvConfig = new NewRelicCVConfig();
    cvConfig.setUuid(generateUuid());
    cvConfig.setAccountId(generateUuid());
    cvConfig.setApplicationName("cv-app");
    cvConfig.setMetricPack(metricPack);
    cvConfig.setApplicationId(12345l);
    NewRelicDataCollectionInfo dataCollectionInfo = mapper.toDataCollectionInfo(cvConfig);
    assertThat(dataCollectionInfo.getMetricPack()).isEqualTo(metricPack.toDTO());
    assertThat(dataCollectionInfo.getApplicationName()).isEqualTo("cv-app");
    assertThat(dataCollectionInfo.getApplicationId()).isEqualTo(cvConfig.getApplicationId());
    assertThat(dataCollectionInfo.getDataCollectionDsl()).isEqualTo("metric-pack-dsl");
  }
}
