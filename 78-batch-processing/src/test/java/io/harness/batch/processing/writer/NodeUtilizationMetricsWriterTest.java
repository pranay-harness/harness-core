package io.harness.batch.processing.writer;

import static io.harness.batch.processing.ccm.UtilizationInstanceType.K8S_NODE;
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
import io.harness.event.payloads.NodeMetric;
import io.harness.event.payloads.Usage;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class NodeUtilizationMetricsWriterTest extends CategoryTest implements EcsEventGenerator {
  @InjectMocks private NodeUtilizationMetricsWriter nodeUtilizationMetricsWriter;
  @Mock private K8sUtilizationGranularDataServiceImpl k8sUtilizationGranularDataService;

  private final String ACCOUNT_ID = "ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String INSTANCEID = "INSTANCEID" + this.getClass().getSimpleName();
  private final String INSTANCETYPE = K8S_NODE;
  private final String SETTINGID = "SETTINGID" + this.getClass().getSimpleName();
  private final long START_TIME_STAMP = 1000000000L;
  private final long END_TIME_STAMP = 1200000000L;
  private final long WINDOW = 200000000L;
  private final String CPU = "2";
  private final String MEMORY = "1024";

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shouldWriteNodeUtilizationMetrics() {
    PublishedMessage nodeUtilizationMetricsMessages = getNodeUtilizationMetricsMessages();
    nodeUtilizationMetricsWriter.write(Arrays.asList(nodeUtilizationMetricsMessages));
    ArgumentCaptor<K8sGranularUtilizationData> K8sGranularUtilizationDataArgumentCaptor =
        ArgumentCaptor.forClass(K8sGranularUtilizationData.class);
    verify(k8sUtilizationGranularDataService).create(K8sGranularUtilizationDataArgumentCaptor.capture());
    K8sGranularUtilizationData k8sGranularUtilizationData = K8sGranularUtilizationDataArgumentCaptor.getValue();
    assertThat(k8sGranularUtilizationData.getCpu()).isEqualTo(Double.valueOf(CPU));
    assertThat(k8sGranularUtilizationData.getMemory()).isEqualTo(Double.valueOf(MEMORY));
    assertThat(k8sGranularUtilizationData.getStartTimestamp()).isEqualTo(START_TIME_STAMP * 1000);
    assertThat(k8sGranularUtilizationData.getEndTimestamp()).isEqualTo(END_TIME_STAMP * 1000);
    assertThat(k8sGranularUtilizationData.getInstanceId()).isEqualTo(INSTANCEID);
    assertThat(k8sGranularUtilizationData.getInstanceType()).isEqualTo(INSTANCETYPE);
    assertThat(k8sGranularUtilizationData.getSettingId()).isEqualTo(SETTINGID);
  }

  PublishedMessage getNodeUtilizationMetricsMessages() {
    NodeMetric nodeMetric = NodeMetric.newBuilder()
                                .setName(INSTANCEID)
                                .setCloudProviderId(SETTINGID)
                                .setTimestamp(Timestamp.newBuilder().setSeconds(END_TIME_STAMP).build())
                                .setWindow(Duration.newBuilder().setSeconds(WINDOW).build())
                                .setUsage(Usage.newBuilder().setCpu(CPU).setMemory(MEMORY).build())
                                .build();

    return getPublishedMessage(ACCOUNT_ID, nodeMetric);
  }
}
