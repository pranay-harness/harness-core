package io.harness.batch.processing.writer;

import static io.harness.batch.processing.ccm.UtilizationInstanceType.ECS_CLUSTER;
import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.batch.processing.billing.timeseries.data.InstanceUtilizationData;
import io.harness.batch.processing.billing.timeseries.service.impl.UtilizationDataServiceImpl;
import io.harness.batch.processing.ccm.UtilizationInstanceType;
import io.harness.batch.processing.integration.EcsEventGenerator;
import io.harness.category.element.UnitTests;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.EcsUtilization;
import io.harness.event.payloads.EcsUtilization.MetricValue;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class EcsUtilizationMetricsWriterTest extends CategoryTest implements EcsEventGenerator {
  @InjectMocks private EcsUtilizationMetricsWriter ecsUtilizationMetricsWriter;
  @Mock private UtilizationDataServiceImpl utilizationDataService;

  private final String TEST_ACCOUNT_ID = "ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String TEST_CLUSTER_NAME = "CLUSTER_NAME_" + this.getClass().getSimpleName();
  private final String TEST_CLUSTER_ARN = "CLUSTER_ARN_" + this.getClass().getSimpleName();
  private final String TEST_SERVICE_NAME = "SERVICE_NAME_" + this.getClass().getSimpleName();
  private final String TEST_SERVICE_ARN = "SERVICE_ARN_" + this.getClass().getSimpleName();
  private final String STATISTIC1 = "Maximum";
  private final String STATISTIC2 = "Average";
  private final String INVALID_STATISTIC = "invalid_statistic";
  private final String INVALID_METRIC = "invalid_metric";
  String SETTING_ID = "SETTING_ID";

  private final String INSTANCEID = TEST_SERVICE_ARN;
  private final String INSTANCETYPE = UtilizationInstanceType.ECS_SERVICE;

  @Captor private ArgumentCaptor<List<InstanceUtilizationData>> instanceUtilizationDataArgumentCaptor;

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shouldWriteTaskInfo() {
    PublishedMessage ecsUtilizationMetricsMessages = getEcsUtilizationMetricsMessage(
        TEST_ACCOUNT_ID, TEST_CLUSTER_NAME, TEST_CLUSTER_ARN, TEST_SERVICE_NAME, TEST_SERVICE_ARN, SETTING_ID);

    ecsUtilizationMetricsWriter.write(Collections.singletonList(ecsUtilizationMetricsMessages));
    verify(utilizationDataService).create(instanceUtilizationDataArgumentCaptor.capture());
    List<InstanceUtilizationData> instanceUtilizationDataList = instanceUtilizationDataArgumentCaptor.getValue();
    InstanceUtilizationData instanceUtilizationDataPoint0 = instanceUtilizationDataList.get(0);
    assertThat(instanceUtilizationDataPoint0.getInstanceId()).isEqualTo(INSTANCEID);
    assertThat(instanceUtilizationDataPoint0.getInstanceType()).isEqualTo(INSTANCETYPE);
    assertThat(instanceUtilizationDataPoint0.getSettingId()).isEqualTo(SETTING_ID);
    assertThat(instanceUtilizationDataPoint0.getCpuUtilizationAvg()).isEqualTo(0.5);
    assertThat(instanceUtilizationDataPoint0.getCpuUtilizationMax()).isEqualTo(0.5);
    assertThat(instanceUtilizationDataPoint0.getMemoryUtilizationAvg()).isEqualTo(10.24);
    assertThat(instanceUtilizationDataPoint0.getMemoryUtilizationMax()).isEqualTo(10.24);
    assertThat(instanceUtilizationDataPoint0.getEndTimestamp()).isEqualTo(12000000000L + 3600000L);
    assertThat(instanceUtilizationDataPoint0.getStartTimestamp()).isEqualTo(12000000000L);

    InstanceUtilizationData instanceUtilizationDataPoint1 = instanceUtilizationDataList.get(1);
    assertThat(instanceUtilizationDataPoint1.getInstanceId()).isEqualTo(INSTANCEID);
    assertThat(instanceUtilizationDataPoint1.getInstanceType()).isEqualTo(INSTANCETYPE);
    assertThat(instanceUtilizationDataPoint1.getSettingId()).isEqualTo(SETTING_ID);
    assertThat(instanceUtilizationDataPoint1.getCpuUtilizationAvg()).isEqualTo(0.6);
    assertThat(instanceUtilizationDataPoint1.getCpuUtilizationMax()).isEqualTo(0.6);
    assertThat(instanceUtilizationDataPoint1.getMemoryUtilizationAvg()).isEqualTo(20.48);
    assertThat(instanceUtilizationDataPoint1.getMemoryUtilizationMax()).isEqualTo(20.48);
    assertThat(instanceUtilizationDataPoint1.getEndTimestamp()).isEqualTo(14000000000L + 3600000L);
    assertThat(instanceUtilizationDataPoint1.getStartTimestamp()).isEqualTo(14000000000L);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void invalidMetricsAndStatisticsTest() {
    List<EcsUtilization> ecsUtilizationList = Arrays.asList(getInvalidStatisticEcsUtilization(),
        getInvalidMetricNameEcsUtilization(STATISTIC1), getInvalidMetricNameEcsUtilization(STATISTIC2));

    for (EcsUtilization ecsUtilization : ecsUtilizationList) {
      assertThatExceptionOfType(InvalidRequestException.class)
          .isThrownBy(()
                          -> ecsUtilizationMetricsWriter.write(
                              Collections.singletonList(getPublishedMessage(TEST_ACCOUNT_ID, ecsUtilization))));
    }
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void clusterInstanceTypeTest() {
    PublishedMessage ecsUtilizationMetricsMessages =
        getEcsUtilizationMetricsMessage(TEST_ACCOUNT_ID, TEST_CLUSTER_NAME, TEST_CLUSTER_ARN, "", "", SETTING_ID);
    ecsUtilizationMetricsWriter.write(Collections.singletonList(ecsUtilizationMetricsMessages));
    verify(utilizationDataService).create(instanceUtilizationDataArgumentCaptor.capture());
    InstanceUtilizationData instanceUtilizationData = instanceUtilizationDataArgumentCaptor.getValue().get(0);
    assertThat(instanceUtilizationData.getInstanceType()).isEqualTo(ECS_CLUSTER);
  }

  private EcsUtilization getInvalidStatisticEcsUtilization() {
    return EcsUtilization.newBuilder()
        .setClusterArn(TEST_CLUSTER_ARN)
        .setClusterName(TEST_CLUSTER_NAME)
        .setServiceArn(TEST_SERVICE_ARN)
        .setServiceName(TEST_SERVICE_NAME)
        .addMetricValues(MetricValue.newBuilder().setStatistic(INVALID_STATISTIC).build())
        .build();
  }

  private EcsUtilization getInvalidMetricNameEcsUtilization(String metricStatistic) {
    return EcsUtilization.newBuilder()
        .setClusterArn(TEST_CLUSTER_ARN)
        .setClusterName(TEST_CLUSTER_NAME)
        .setServiceArn(TEST_SERVICE_ARN)
        .setServiceName(TEST_SERVICE_NAME)
        .addMetricValues(MetricValue.newBuilder().setStatistic(metricStatistic).setMetricName(INVALID_METRIC).build())
        .build();
  }
}
