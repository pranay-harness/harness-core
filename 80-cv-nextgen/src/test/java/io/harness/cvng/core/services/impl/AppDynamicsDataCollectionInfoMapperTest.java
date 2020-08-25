package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.AppDynamicsDataCollectionInfo;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AppDynamicsDataCollectionInfoMapperTest extends CvNextGenTest {
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
    assertThat(appDynamicsDataCollectionInfo.getMetricPack()).isEqualTo(metricPack.getDTO());
    assertThat(appDynamicsDataCollectionInfo.getApplicationName()).isEqualTo("cv-app");
    assertThat(appDynamicsDataCollectionInfo.getTierName()).isEqualTo("docker-tier");
    assertThat(appDynamicsDataCollectionInfo.getDataCollectionDsl()).isEqualTo("metric-pack-dsl");
  }
}