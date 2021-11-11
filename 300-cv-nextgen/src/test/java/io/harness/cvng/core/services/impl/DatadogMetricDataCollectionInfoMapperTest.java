package io.harness.cvng.core.services.impl;


import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.beans.DatadogMetricsDataCollectionInfo;
import io.harness.cvng.beans.StackdriverDataCollectionInfo;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.beans.stackdriver.StackDriverMetricDefinition;
import io.harness.cvng.core.beans.DatadogMetricHealthDefinition;
import io.harness.cvng.core.entities.DatadogMetricCVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;

import static io.harness.rule.OwnerRule.PAVIC;
import static org.assertj.core.api.Assertions.assertThat;

public class DatadogMetricDataCollectionInfoMapperTest extends CvNextGenTestBase {
  private static final String MOCKED_DASHBOARD_NAME = "MockedDashboardName";
  private static final String MOCKED_METRIC_NAME = "testMetricName";
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

    DatadogMetricCVConfig datadogMetricCVConfig = builderFactory
            .datadogMetricCVConfigBuilder()
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
}
