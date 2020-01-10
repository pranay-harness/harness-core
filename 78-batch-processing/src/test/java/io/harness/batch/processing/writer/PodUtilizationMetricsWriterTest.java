package io.harness.batch.processing.writer;

import static io.harness.batch.processing.ccm.UtilizationInstanceType.K8S_POD;
import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;

import io.harness.CategoryTest;
import io.harness.batch.processing.billing.timeseries.data.K8sGranularUtilizationData;
import io.harness.batch.processing.billing.timeseries.service.impl.K8sUtilizationGranularDataServiceImpl;
import io.harness.batch.processing.integration.EcsEventGenerator;
import io.harness.category.element.UnitTests;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.PodMetric;
import io.harness.event.payloads.PodMetric.Container;
import io.harness.event.payloads.Usage;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class PodUtilizationMetricsWriterTest extends CategoryTest implements EcsEventGenerator {
  @InjectMocks private PodUtilizationMetricsWriter podUtilizationMetricsWriter;
  @Mock private K8sUtilizationGranularDataServiceImpl k8sUtilizationGranularDataService;

  private final String ACCOUNT_ID = "ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String INSTANCEID = "INSTANCEID" + this.getClass().getSimpleName();
  private final String INSTANCETYPE = K8S_POD;
  private final String SETTINGID = "SETTINGID" + this.getClass().getSimpleName();
  private final long START_TIME_STAMP = 1000000000L;
  private final long END_TIME_STAMP = 1200000000L;
  private final long WINDOW = 200000000L;
  private final long CPU = 2 * 1_000_000_000L;
  private final long MEMORY = 1024 * (1 << 20);

  @Captor private ArgumentCaptor<List<K8sGranularUtilizationData>> K8sGranularUtilizationDataArgumentCaptor;

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shouldWritePodUtilizationMetrics() {
    PublishedMessage podUtilizationMetricsMessages = getPodUtilizationMetricsMessages();
    podUtilizationMetricsWriter.write(Collections.singletonList(podUtilizationMetricsMessages));
    verify(k8sUtilizationGranularDataService).create(K8sGranularUtilizationDataArgumentCaptor.capture());
    K8sGranularUtilizationData k8sGranularUtilizationData = K8sGranularUtilizationDataArgumentCaptor.getValue().get(0);
    assertThat(k8sGranularUtilizationData.getCpu()).isEqualTo(2048);
    assertThat(k8sGranularUtilizationData.getMemory()).isEqualTo(1024);
    assertThat(k8sGranularUtilizationData.getStartTimestamp()).isEqualTo(START_TIME_STAMP * 1000);
    assertThat(k8sGranularUtilizationData.getEndTimestamp()).isEqualTo(END_TIME_STAMP * 1000);
    assertThat(k8sGranularUtilizationData.getInstanceId()).isEqualTo(INSTANCEID);
    assertThat(k8sGranularUtilizationData.getInstanceType()).isEqualTo(INSTANCETYPE);
    assertThat(k8sGranularUtilizationData.getSettingId()).isEqualTo(SETTINGID);
  }

  PublishedMessage getPodUtilizationMetricsMessages() {
    PodMetric podMetric =
        PodMetric.newBuilder()
            .setName(INSTANCEID)
            .setCloudProviderId(SETTINGID)
            .setTimestamp(Timestamp.newBuilder().setSeconds(END_TIME_STAMP).build())
            .setWindow(Duration.newBuilder().setSeconds(WINDOW).build())
            .addContainers(Container.newBuilder()
                               .setUsage(Usage.newBuilder().setCpuNano(CPU).setMemoryByte(MEMORY).build())
                               .build())
            .build();

    return getPublishedMessage(ACCOUNT_ID, podMetric);
  }
}
