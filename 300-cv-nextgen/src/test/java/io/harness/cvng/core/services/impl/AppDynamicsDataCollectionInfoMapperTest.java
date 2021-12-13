package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ABHIJITH;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.beans.AppDynamicsDataCollectionInfo;
import io.harness.cvng.beans.AppDynamicsDataCollectionInfo.AppMetricInfoDTO;
import io.harness.cvng.core.entities.AnalysisInfo.SLI;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.AppDynamicsCVConfig.MetricInfo;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.services.CVNextGenConstants;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AppDynamicsDataCollectionInfoMapperTest extends CvNextGenTestBase {
  @Inject private AppDynamicsDataCollectionInfoMapper mapper;
  BuilderFactory builderFactory;

  @Before
  public void before() {
    builderFactory = BuilderFactory.getDefault();
  }

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

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testToDataConnectionInfo_forSLI() {
    List<AppDynamicsCVConfig> cvConfigs = Arrays.<AppDynamicsCVConfig>asList(
        (AppDynamicsCVConfig) builderFactory.appDynamicsCVConfigBuilder()
            .metricPack(MetricPack.builder().identifier(CVNextGenConstants.ERRORS_PACK_IDENTIFIER).build())
            .build(),
        builderFactory.appDynamicsCVConfigBuilder()
            .metricInfos(Arrays.asList(getMetricInfo("1"), getMetricInfo("2")))
            .build(),
        builderFactory.appDynamicsCVConfigBuilder().metricInfos(Arrays.asList(getMetricInfo("3"))).build(),
        builderFactory.appDynamicsCVConfigBuilder()
            .metricInfos(Arrays.asList(getMetricInfo("4"), getMetricInfo("5")))
            .build());
    AppDynamicsDataCollectionInfo appDynamicsDataCollectionInfo = mapper.toDataCollectionInfo(
        cvConfigs, builderFactory.ratioServiceLevelIndicatorBuilder().metric1("metric4").metric2("metric2").build());
    assertThat(appDynamicsDataCollectionInfo.getMetricPack().getIdentifier())
        .isEqualTo(CVNextGenConstants.CUSTOM_PACK_IDENTIFIER);
    assertThat(appDynamicsDataCollectionInfo.getCustomMetrics()).hasSize(2);
    assertThat(appDynamicsDataCollectionInfo.getCustomMetrics()
                   .stream()
                   .map(AppMetricInfoDTO::getMetricPath)
                   .collect(Collectors.toSet()))
        .isEqualTo(new HashSet<>(Arrays.asList("metricPath4", "metricPath2")));
    assertThat(appDynamicsDataCollectionInfo.getCustomMetrics()
                   .stream()
                   .map(AppMetricInfoDTO::getMetricIdentifier)
                   .collect(Collectors.toSet()))
        .isEqualTo(new HashSet<>(Arrays.asList("metric4", "metric2")));
    assertThat(appDynamicsDataCollectionInfo.getCustomMetrics()
                   .stream()
                   .map(AppMetricInfoDTO::getMetricName)
                   .collect(Collectors.toSet()))
        .isEqualTo(new HashSet<>(Arrays.asList("metricName4", "metricName2")));
    assertThat(appDynamicsDataCollectionInfo.getCustomMetrics()
                   .stream()
                   .map(AppMetricInfoDTO::getBaseFolder)
                   .collect(Collectors.toSet()))
        .isEqualTo(new HashSet<>(Arrays.asList("baseFolder2", "baseFolder4")));
    assertThat(appDynamicsDataCollectionInfo.getDataCollectionDsl()).isEqualTo("dsl");
  }

  private MetricInfo getMetricInfo(String suffix) {
    return MetricInfo.builder()
        .metricName("metricName" + suffix)
        .identifier("metric" + suffix)
        .metricPath("metricPath" + suffix)
        .baseFolder("baseFolder" + suffix)
        .sli(SLI.builder().enabled(true).build())
        .build();
  }
}
