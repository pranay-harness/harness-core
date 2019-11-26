package io.harness.batch.processing.writer;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.batch.processing.billing.timeseries.data.InstanceUtilizationData;
import io.harness.batch.processing.billing.timeseries.service.impl.UtilizationDataServiceImpl;
import io.harness.batch.processing.ccm.UtilizationJobType;
import io.harness.batch.processing.integration.EcsEventGenerator;
import io.harness.category.element.UnitTests;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.EcsUtilization;
import io.harness.event.payloads.EcsUtilization.MetricValue;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
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
  private final String METRIC = "MemoryUtilization";
  private final String INSTANCEID = TEST_SERVICE_ARN;
  private final String INSTANCETYPE = UtilizationJobType.ECS_SERVICE;

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shouldWriteTaskInfo() {
    String settingId = "SETTING_ID";
    PublishedMessage ecsUtilizationMetricsMessages = getEcsUtilizationMetricsMessage(
        TEST_ACCOUNT_ID, TEST_CLUSTER_NAME, TEST_CLUSTER_ARN, TEST_SERVICE_NAME, TEST_SERVICE_ARN, settingId);

    ecsUtilizationMetricsWriter.write(Arrays.asList(ecsUtilizationMetricsMessages));
    ArgumentCaptor<InstanceUtilizationData> instanceUtilizationDataArgumentCaptor =
        ArgumentCaptor.forClass(InstanceUtilizationData.class);
    verify(utilizationDataService).create(instanceUtilizationDataArgumentCaptor.capture());
    InstanceUtilizationData instanceUtilizationData = instanceUtilizationDataArgumentCaptor.getValue();
    assertThat(instanceUtilizationData.getClusterArn()).isEqualTo(TEST_CLUSTER_ARN);
    assertThat(instanceUtilizationData.getClusterName()).isEqualTo(TEST_CLUSTER_NAME);
    assertThat(instanceUtilizationData.getServiceArn()).isEqualTo(TEST_SERVICE_ARN);
    assertThat(instanceUtilizationData.getServiceName()).isEqualTo(TEST_SERVICE_NAME);
    assertThat(instanceUtilizationData.getInstanceId()).isEqualTo(INSTANCEID);
    assertThat(instanceUtilizationData.getInstanceType()).isEqualTo(INSTANCETYPE);
    assertThat(instanceUtilizationData.getSettingId()).isEqualTo(settingId);
    assertThat(instanceUtilizationData.getCpuUtilizationAvg()).isEqualTo(65.0);
    assertThat(instanceUtilizationData.getCpuUtilizationMax()).isEqualTo(70.0);
    assertThat(instanceUtilizationData.getMemoryUtilizationAvg()).isEqualTo(1267.0);
    assertThat(instanceUtilizationData.getMemoryUtilizationMax()).isEqualTo(1500.0);
    assertThat(instanceUtilizationData.getEndTimestamp()).isEqualTo(14000000000L);
    assertThat(instanceUtilizationData.getStartTimestamp()).isEqualTo(12000000000L);
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
                              Arrays.asList(getPublishedMessage(TEST_ACCOUNT_ID, ecsUtilization))));
    }
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void emptyTimestampListTest() {
    EcsUtilization ecsUtilization = getEmptyTimestampEcsUtilization();

    ecsUtilizationMetricsWriter.write(Arrays.asList(getPublishedMessage(TEST_ACCOUNT_ID, ecsUtilization)));
    ArgumentCaptor<InstanceUtilizationData> instanceUtilizationDataArgumentCaptor =
        ArgumentCaptor.forClass(InstanceUtilizationData.class);
    verify(utilizationDataService).create(instanceUtilizationDataArgumentCaptor.capture());
    InstanceUtilizationData instanceUtilizationData = instanceUtilizationDataArgumentCaptor.getValue();
    assertThat(instanceUtilizationData.getEndTimestamp()).isEqualTo(0L);
    assertThat(instanceUtilizationData.getStartTimestamp()).isEqualTo(0L);
  }

  EcsUtilization getInvalidStatisticEcsUtilization() {
    return EcsUtilization.newBuilder()
        .setClusterArn(TEST_CLUSTER_ARN)
        .setClusterName(TEST_CLUSTER_NAME)
        .setServiceArn(TEST_SERVICE_ARN)
        .setServiceName(TEST_SERVICE_NAME)
        .addMetricValues(MetricValue.newBuilder().setStatistic(INVALID_STATISTIC).build())
        .build();
  }

  EcsUtilization getInvalidMetricNameEcsUtilization(String metricStatistic) {
    return EcsUtilization.newBuilder()
        .setClusterArn(TEST_CLUSTER_ARN)
        .setClusterName(TEST_CLUSTER_NAME)
        .setServiceArn(TEST_SERVICE_ARN)
        .setServiceName(TEST_SERVICE_NAME)
        .addMetricValues(MetricValue.newBuilder().setStatistic(metricStatistic).setMetricName(INVALID_METRIC).build())
        .build();
  }

  EcsUtilization getEmptyTimestampEcsUtilization() {
    return EcsUtilization.newBuilder()
        .setClusterArn(TEST_CLUSTER_ARN)
        .setClusterName(TEST_CLUSTER_NAME)
        .setServiceArn(TEST_SERVICE_ARN)
        .setServiceName(TEST_SERVICE_NAME)
        .addMetricValues(MetricValue.newBuilder()
                             .setStatistic(STATISTIC1)
                             .setMetricName(METRIC)
                             .addValues(1034.0)
                             .addValues(1500.0)

                             .build())
        .build();
  }
}
