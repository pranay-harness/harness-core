/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ABHIJITH;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.NewRelicDataCollectionInfo;
import io.harness.cvng.beans.NewRelicDataCollectionInfo.NewRelicMetricInfoDTO;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.MetricResponseMapping;
import io.harness.cvng.core.entities.AnalysisInfo;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.NewRelicCVConfig;
import io.harness.cvng.core.entities.NewRelicCVConfig.NewRelicMetricInfo;
import io.harness.cvng.core.entities.VerificationTask.TaskType;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NewRelicDataCollectionInfoMapperTest extends CvNextGenTestBase {
  @Inject private NewRelicDataCollectionInfoMapper mapper;
  BuilderFactory builderFactory;

  @Before
  public void setup() {
    builderFactory = BuilderFactory.getDefault();
  }

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
    NewRelicDataCollectionInfo dataCollectionInfo = mapper.toDataCollectionInfo(cvConfig, TaskType.DEPLOYMENT);
    assertThat(dataCollectionInfo.getMetricPack()).isEqualTo(metricPack.toDTO());
    assertThat(dataCollectionInfo.getApplicationName()).isEqualTo("cv-app");
    assertThat(dataCollectionInfo.getApplicationId()).isEqualTo(cvConfig.getApplicationId());
    assertThat(dataCollectionInfo.getDataCollectionDsl()).isEqualTo("metric-pack-dsl");
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testToDataConnectionInfo_withCustomMetrics() {
    NewRelicCVConfig cvConfig = createCVConfigWithCustomMetric();
    NewRelicDataCollectionInfo dataCollectionInfo = mapper.toDataCollectionInfo(cvConfig, TaskType.LIVE_MONITORING);
    assertThat(dataCollectionInfo.getMetricPack()).isEqualTo(cvConfig.getMetricPack().toDTO());
    assertThat(dataCollectionInfo.getApplicationName()).isNull();
    assertThat(dataCollectionInfo.getApplicationId()).isEqualTo(0);
    assertThat(dataCollectionInfo.getDataCollectionDsl()).isEqualTo("metric-pack-dsl");

    assertThat(dataCollectionInfo.getGroupName()).isEqualTo("groupName");
    assertThat(dataCollectionInfo.getMetricInfoList().size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testToDataConnectionInfo_withCustomMetricsTaskTypeFilter() {
    NewRelicCVConfig cvConfig = createCVConfigWithCustomMetric();
    NewRelicDataCollectionInfo dataCollectionInfo = mapper.toDataCollectionInfo(cvConfig, TaskType.DEPLOYMENT);
    assertThat(dataCollectionInfo.getMetricPack()).isEqualTo(cvConfig.getMetricPack().toDTO());
    assertThat(dataCollectionInfo.getApplicationName()).isNull();
    assertThat(dataCollectionInfo.getApplicationId()).isEqualTo(0);
    assertThat(dataCollectionInfo.getDataCollectionDsl()).isEqualTo("metric-pack-dsl");

    assertThat(dataCollectionInfo.getGroupName()).isEqualTo("groupName");
    assertThat(dataCollectionInfo.getMetricInfoList().size()).isEqualTo(1);
    NewRelicMetricInfoDTO metricInfoDTO = dataCollectionInfo.getMetricInfoList().get(0);
    NewRelicMetricInfo metricInfo = cvConfig.getMetricInfos().get(0);
    assertThat(metricInfoDTO.getMetricName()).isEqualTo(metricInfo.getMetricName());
    assertThat(metricInfoDTO.getMetricIdentifier()).isEqualTo(metricInfo.getIdentifier());
    assertThat(metricInfoDTO.getNrql()).isEqualTo(metricInfo.getNrql());
    assertThat(metricInfoDTO.getResponseMapping()).isEqualTo(metricInfo.getResponseMapping().toDto());
  }

  private NewRelicCVConfig createCVConfigWithCustomMetric() {
    NewRelicCVConfig cvConfig = (NewRelicCVConfig) builderFactory.newRelicCVConfigBuilder()
                                    .groupName("groupName")

                                    .connectorIdentifier("connector")
                                    .productName("apm")
                                    .identifier("monService")
                                    .monitoringSourceName("monService")
                                    .build();
    cvConfig.setMetricPack(
        MetricPack.builder().dataCollectionDsl("metric-pack-dsl").category(CVMonitoringCategory.PERFORMANCE).build());
    cvConfig.setMetricInfos(Arrays.asList(
        NewRelicCVConfig.NewRelicMetricInfo.builder()
            .metricName("metric1")
            .identifier("metricIdentifier1")
            .nrql("Select * from transactions")
            .metricType(TimeSeriesMetricType.RESP_TIME)
            .responseMapping(MetricResponseMapping.builder()
                                 .metricValueJsonPath("$.metricValue")
                                 .timestampJsonPath("$.timestamp")
                                 .build())
            .deploymentVerification(AnalysisInfo.DeploymentVerification.builder().enabled(Boolean.TRUE).build())
            .liveMonitoring(AnalysisInfo.LiveMonitoring.builder().enabled(Boolean.FALSE).build())
            .sli(AnalysisInfo.SLI.builder().enabled(Boolean.TRUE).build())
            .build()));
    return cvConfig;
  }
}
