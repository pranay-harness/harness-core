package io.harness.cvng.core.services.impl;

import static io.harness.rule.OwnerRule.DEEPAK_CHHIKARA;
import static io.harness.rule.OwnerRule.PAVIC;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.beans.DatadogMetricsDataCollectionInfo;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.core.entities.DatadogMetricCVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;
import io.harness.cvng.servicelevelobjective.entities.ThresholdServiceLevelIndicator;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DatadogMetricDataCollectionInfoMapperTest extends CvNextGenTestBase {
  private static final String MOCKED_DASHBOARD_NAME = "MockedDashboardName";
  private static final String MOCKED_METRIC_NAME = "testMetricName";
  private static final String MOCKED_METRIC_IDENTIFIER = "testMetricIdentifier";
  private static final String MOCKED_METRIC_QUERY = "system.user.cpu{*}";

  @Inject private DatadogMetricDataCollectionInfoMapper classUnderTest;
  private final BuilderFactory builderFactory = BuilderFactory.getDefault();

  @Test
  @Owner(developers = PAVIC)
  @Category(UnitTests.class)
  public void testToDataCollectionInfo() {
    MetricPack metricPack = MetricPack.builder().dataCollectionDsl("metric-pack-dsl").build();
    DatadogMetricCVConfig.MetricInfo metricInfo = DatadogMetricCVConfig.MetricInfo.builder()
                                                      .query(MOCKED_METRIC_QUERY)
                                                      .metricName(MOCKED_METRIC_NAME)
                                                      .metricType(TimeSeriesMetricType.INFRA)
                                                      .build();

    DatadogMetricCVConfig datadogMetricCVConfig = builderFactory.datadogMetricCVConfigBuilder()
                                                      .metricInfoList(Arrays.asList(metricInfo))
                                                      .dashboardName(MOCKED_DASHBOARD_NAME)
                                                      .build();
    datadogMetricCVConfig.setMetricPack(metricPack);

    DatadogMetricsDataCollectionInfo collectionInfoResult = classUnderTest.toDataCollectionInfo(datadogMetricCVConfig);

    assertThat(collectionInfoResult).isNotNull();
    collectionInfoResult.getMetricDefinitions().forEach(metricInfoToCheck -> {
      assertThat(metricInfoToCheck.getMetricName()).isEqualTo(MOCKED_METRIC_NAME);
      assertThat(metricInfoToCheck.getQuery()).isEqualTo(MOCKED_METRIC_QUERY);
    });
  }

  @Test
  @Owner(developers = DEEPAK_CHHIKARA)
  @Category(UnitTests.class)
  public void testToDataCollectionInfoForSLI() {
    MetricPack metricPack = MetricPack.builder().dataCollectionDsl("metric-pack-dsl").build();
    DatadogMetricCVConfig.MetricInfo metricInfo = DatadogMetricCVConfig.MetricInfo.builder()
                                                      .query(MOCKED_METRIC_QUERY)
                                                      .identifier(MOCKED_METRIC_IDENTIFIER)
                                                      .metricName(MOCKED_METRIC_NAME)
                                                      .metricType(TimeSeriesMetricType.INFRA)
                                                      .build();
    ServiceLevelIndicator serviceLevelIndicator =
        ThresholdServiceLevelIndicator.builder().metric1(MOCKED_METRIC_IDENTIFIER).build();

    DatadogMetricCVConfig datadogMetricCVConfig = builderFactory.datadogMetricCVConfigBuilder()
                                                      .metricInfoList(Arrays.asList(metricInfo))
                                                      .dashboardName(MOCKED_DASHBOARD_NAME)
                                                      .build();
    datadogMetricCVConfig.setMetricPack(metricPack);

    DatadogMetricsDataCollectionInfo collectionInfoResult =
        classUnderTest.toDataCollectionInfo(Collections.singletonList(datadogMetricCVConfig), serviceLevelIndicator);

    assertThat(collectionInfoResult).isNotNull();
    assertThat(collectionInfoResult.getMetricDefinitions().size()).isEqualTo(1);
    collectionInfoResult.getMetricDefinitions().forEach(metricInfoToCheck -> {
      assertThat(metricInfoToCheck.getMetricName()).isEqualTo(MOCKED_METRIC_IDENTIFIER);
      assertThat(metricInfoToCheck.getQuery()).isEqualTo(MOCKED_METRIC_QUERY);
    });
  }

  @Test
  @Owner(developers = DEEPAK_CHHIKARA)
  @Category(UnitTests.class)
  public void testToDataCollectionInfoForSLIWithDifferentMetricName() {
    MetricPack metricPack = MetricPack.builder().dataCollectionDsl("metric-pack-dsl").build();
    DatadogMetricCVConfig.MetricInfo metricInfo = DatadogMetricCVConfig.MetricInfo.builder()
                                                      .query(MOCKED_METRIC_QUERY)
                                                      .metricName(MOCKED_METRIC_NAME)
                                                      .identifier(MOCKED_METRIC_IDENTIFIER)
                                                      .metricType(TimeSeriesMetricType.INFRA)
                                                      .build();
    ServiceLevelIndicator serviceLevelIndicator = ThresholdServiceLevelIndicator.builder().metric1("metric1").build();

    DatadogMetricCVConfig datadogMetricCVConfig = builderFactory.datadogMetricCVConfigBuilder()
                                                      .metricInfoList(Arrays.asList(metricInfo))
                                                      .dashboardName(MOCKED_DASHBOARD_NAME)
                                                      .build();
    datadogMetricCVConfig.setMetricPack(metricPack);

    DatadogMetricsDataCollectionInfo collectionInfoResult =
        classUnderTest.toDataCollectionInfo(Collections.singletonList(datadogMetricCVConfig), serviceLevelIndicator);

    assertThat(collectionInfoResult).isNotNull();
    assertThat(collectionInfoResult.getMetricDefinitions().size()).isEqualTo(0);
  }
}
