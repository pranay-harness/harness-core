package io.harness.perpetualtask.k8s.watch;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;

import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.client.EventPublisher;
import io.harness.event.payloads.NodeMetric;
import io.harness.event.payloads.PodMetric;
import io.harness.grpc.utils.HDurations;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.metrics.client.K8sMetricsClient;
import io.harness.perpetualtask.k8s.metrics.client.K8sMetricsExtensionAdapter;
import io.harness.perpetualtask.k8s.metrics.client.model.Usage;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetricsList;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetricsList;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Instant;

@RunWith(MockitoJUnitRunner.class)
public class K8SWatchTaskExecutorTest extends CategoryTest {
  private static final String CLOUD_PROVIDER_ID = "cloud-provider-id";

  @Rule public final KubernetesServer server = new KubernetesServer();

  private K8sMetricsClient k8sMetricClient;

  @Mock EventPublisher eventPublisher;
  @Captor ArgumentCaptor<Message> messageArgumentCaptor;

  @Before
  public void setUp() throws Exception {
    k8sMetricClient = new K8sMetricsExtensionAdapter().adapt(server.getClient());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPublishNodeMetrics() throws Exception {
    server.expect()
        .withPath("/apis/metrics.k8s.io/v1beta1/nodes")
        .andReturn(200,
            NodeMetricsList.builder()
                .items(ImmutableList.of(NodeMetrics.builder()
                                            .name("node1-name")
                                            .timestamp("2019-11-26T07:00:32Z")
                                            .window("30s")
                                            .usage(Usage.builder().cpu("746640510n").memory("6825124Ki").build())
                                            .build(),
                    NodeMetrics.builder()
                        .name("node2-name")
                        .timestamp("2019-11-26T07:00:28Z")
                        .window("30s")
                        .usage(Usage.builder().cpu("2938773795n").memory("18281752Ki").build())
                        .build()))
                .build())
        .once();
    Instant heartbeatTime = Instant.now();
    doNothing()
        .when(eventPublisher)
        .publishMessage(messageArgumentCaptor.capture(), eq(HTimestamps.fromInstant(heartbeatTime)));
    K8SWatchTaskExecutor.publishNodeMetrics(k8sMetricClient, eventPublisher, CLOUD_PROVIDER_ID, heartbeatTime);
    assertThat(messageArgumentCaptor.getAllValues())
        .hasSize(2)
        .containsExactlyInAnyOrder(
            NodeMetric.newBuilder()
                .setCloudProviderId(CLOUD_PROVIDER_ID)
                .setName("node1-name")
                .setTimestamp(HTimestamps.parse("2019-11-26T07:00:32Z"))
                .setWindow(HDurations.parse("30s"))
                .setUsage(
                    io.harness.event.payloads.Usage.newBuilder().setCpu("746640510n").setMemory("6825124Ki").build())
                .build(),
            NodeMetric.newBuilder()
                .setCloudProviderId(CLOUD_PROVIDER_ID)
                .setName("node2-name")
                .setTimestamp(HTimestamps.parse("2019-11-26T07:00:28Z"))
                .setWindow(HDurations.parse("30s"))
                .setUsage(
                    io.harness.event.payloads.Usage.newBuilder().setCpu("2938773795n").setMemory("18281752Ki").build())
                .build());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPublishPodMetrics() throws Exception {
    server.expect()
        .withPath("/apis/metrics.k8s.io/v1beta1/pods")
        .andReturn(200,
            PodMetricsList.builder()
                .item(PodMetrics.builder()
                          .name("pod1")
                          .namespace("ns1")
                          .timestamp("2019-11-26T07:00:32Z")
                          .window("30s")
                          .container(PodMetrics.Container.builder()
                                         .name("p1-ctr1")
                                         .usage(Usage.builder().cpu("41181421n").memory("139304Ki").build())
                                         .build())
                          .build())
                .item(PodMetrics.builder()
                          .name("pod2")
                          .namespace("ns1")
                          .timestamp("2019-11-26T07:00:32Z")
                          .window("30s")
                          .container(PodMetrics.Container.builder()
                                         .name("p2-ctr1")
                                         .usage(Usage.builder().cpu("185503n").memory("7460Ki").build())
                                         .build())
                          .container(PodMetrics.Container.builder()
                                         .name("p2-ctr2")
                                         .usage(Usage.builder().cpu("735522992n").memory("225144Ki").build())
                                         .build())
                          .build())
                .build())
        .once();
    Instant heartbeatTime = Instant.now();
    doNothing()
        .when(eventPublisher)
        .publishMessage(messageArgumentCaptor.capture(), eq(HTimestamps.fromInstant(heartbeatTime)));
    K8SWatchTaskExecutor.publishPodMetrics(k8sMetricClient, eventPublisher, CLOUD_PROVIDER_ID, heartbeatTime);
    assertThat(messageArgumentCaptor.getAllValues())
        .hasSize(2)
        .containsExactlyInAnyOrder(PodMetric.newBuilder()
                                       .setCloudProviderId(CLOUD_PROVIDER_ID)
                                       .setName("pod1")
                                       .setNamespace("ns1")
                                       .setTimestamp(HTimestamps.parse("2019-11-26T07:00:32Z"))
                                       .setWindow(HDurations.parse("30s"))
                                       .addContainers(PodMetric.Container.newBuilder()
                                                          .setName("p1-ctr1")
                                                          .setUsage(io.harness.event.payloads.Usage.newBuilder()
                                                                        .setCpu("41181421n")
                                                                        .setMemory("139304Ki")
                                                                        .build())
                                                          .build())
                                       .build(),
            PodMetric.newBuilder()
                .setCloudProviderId(CLOUD_PROVIDER_ID)
                .setName("pod2")
                .setNamespace("ns1")
                .setTimestamp(HTimestamps.parse("2019-11-26T07:00:32Z"))
                .setWindow(HDurations.parse("30s"))
                .addContainers(
                    PodMetric.Container.newBuilder()
                        .setName("p2-ctr1")
                        .setUsage(
                            io.harness.event.payloads.Usage.newBuilder().setCpu("185503n").setMemory("7460Ki").build())
                        .build())
                .addContainers(PodMetric.Container.newBuilder()
                                   .setName("p2-ctr2")
                                   .setUsage(io.harness.event.payloads.Usage.newBuilder()
                                                 .setCpu("735522992n")
                                                 .setMemory("225144Ki")
                                                 .build())
                                   .build())
                .build());
  }
}