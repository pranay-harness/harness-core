package io.harness.cvng.core.services.impl;

import static io.harness.rule.OwnerRule.DEEPAK_CHHIKARA;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.beans.PrometheusDataCollectionInfo;
import io.harness.cvng.beans.PrometheusDataCollectionInfo.MetricCollectionInfo;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.core.beans.PrometheusMetricDefinition.PrometheusFilter;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.PrometheusCVConfig;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;
import io.harness.cvng.servicelevelobjective.entities.ThresholdServiceLevelIndicator;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PrometheusDataCollectionInfoMapperTest extends CvNextGenTestBase {
  @Inject private PrometheusDataCollectionInfoMapper mapper;
  BuilderFactory builderFactory = BuilderFactory.getDefault();

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testToDataConnectionInfo() {
    MetricPack metricPack = MetricPack.builder().dataCollectionDsl("metric-pack-dsl").build();
    PrometheusCVConfig cvConfig = builderFactory.prometheusCVConfigBuilder().groupName("mygroupName").build();

    cvConfig.setMetricPack(metricPack);

    PrometheusCVConfig.MetricInfo metricInfo =
        PrometheusCVConfig.MetricInfo.builder()
            .metricName("myMetric")
            .metricType(TimeSeriesMetricType.RESP_TIME)
            .prometheusMetricName("cpu_usage_total")
            .envFilter(Arrays.asList(PrometheusFilter.builder().labelName("namespace").labelValue("cv-demo").build()))
            .serviceFilter(Arrays.asList(PrometheusFilter.builder().labelName("app").labelValue("cv-demo-app").build()))
            .additionalFilters(Arrays.asList(PrometheusFilter.builder().labelName("filter2").labelValue("cv-2").build(),
                PrometheusFilter.builder().labelName("filter3").labelValue("cv-3").build()))
            .build();

    cvConfig.setMetricInfoList(Arrays.asList(metricInfo));
    PrometheusDataCollectionInfo dataCollectionInfo = mapper.toDataCollectionInfo(cvConfig);
    assertThat(dataCollectionInfo.getGroupName()).isEqualTo("mygroupName");
    assertThat(dataCollectionInfo.getMetricCollectionInfoList()).isNotEmpty();
    assertThat(dataCollectionInfo.getDataCollectionDsl()).isEqualTo("metric-pack-dsl");

    List<MetricCollectionInfo> metricCollectionInfoList = dataCollectionInfo.getMetricCollectionInfoList();
    metricCollectionInfoList.forEach(metricCollectionInfo -> {
      assertThat(metricCollectionInfo.getMetricName()).isEqualTo("myMetric");
      assertThat(metricCollectionInfo.getQuery())
          .isEqualTo("cpu_usage_total{app=\"cv-demo-app\",namespace=\"cv-demo\",filter2=\"cv-2\",filter3=\"cv-3\"}");
      assertThat(metricCollectionInfo.getFilters())
          .isEqualTo("app=\"cv-demo-app\",namespace=\"cv-demo\",filter2=\"cv-2\",filter3=\"cv-3\"");
    });
  }

  @Test
  @Owner(developers = DEEPAK_CHHIKARA)
  @Category(UnitTests.class)
  public void testToDataCollectionInfoForSLI() {
    MetricPack metricPack = MetricPack.builder().dataCollectionDsl("metric-pack-dsl").build();
    PrometheusCVConfig cvConfig = builderFactory.prometheusCVConfigBuilder().groupName("mygroupName").build();
    ServiceLevelIndicator serviceLevelIndicator = ThresholdServiceLevelIndicator.builder().metric1("myMetric").build();

    cvConfig.setMetricPack(metricPack);

    PrometheusCVConfig.MetricInfo metricInfo = PrometheusCVConfig.MetricInfo.builder()
                                                   .metricName("myMetric")
                                                   .metricType(TimeSeriesMetricType.RESP_TIME)
                                                   .prometheusMetricName("cpu_usage_total")
                                                   .build();

    cvConfig.setMetricInfoList(Arrays.asList(metricInfo));
    PrometheusDataCollectionInfo dataCollectionInfo =
        mapper.toDataCollectionInfo(Collections.singletonList(cvConfig), serviceLevelIndicator);
    assertThat(dataCollectionInfo.getMetricCollectionInfoList().size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = DEEPAK_CHHIKARA)
  @Category(UnitTests.class)
  public void testToDataCollectionInfoForSLIWithDifferentMetricName() {
    MetricPack metricPack = MetricPack.builder().dataCollectionDsl("metric-pack-dsl").build();
    PrometheusCVConfig cvConfig = builderFactory.prometheusCVConfigBuilder().groupName("mygroupName").build();
    ServiceLevelIndicator serviceLevelIndicator = ThresholdServiceLevelIndicator.builder().metric1("metric1").build();

    cvConfig.setMetricPack(metricPack);

    PrometheusCVConfig.MetricInfo metricInfo = PrometheusCVConfig.MetricInfo.builder()
                                                   .metricName("myMetric")
                                                   .metricType(TimeSeriesMetricType.RESP_TIME)
                                                   .prometheusMetricName("cpu_usage_total")
                                                   .build();

    cvConfig.setMetricInfoList(Arrays.asList(metricInfo));
    PrometheusDataCollectionInfo dataCollectionInfo =
        mapper.toDataCollectionInfo(Collections.singletonList(cvConfig), serviceLevelIndicator);
    assertThat(dataCollectionInfo.getMetricCollectionInfoList().size()).isEqualTo(0);
  }
}
