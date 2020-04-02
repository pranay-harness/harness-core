package io.harness.perpetualtask.k8s.metrics.collector;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;

import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.health.HealthStatusService;
import io.harness.event.client.EventPublisher;
import io.harness.event.payloads.AggregatedUsage;
import io.harness.event.payloads.NodeMetric;
import io.harness.event.payloads.PodMetric;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.metrics.client.K8sMetricsClient;
import io.harness.perpetualtask.k8s.metrics.client.K8sMetricsExtensionAdapter;
import io.harness.perpetualtask.k8s.metrics.client.model.Usage;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetricsList;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetricsList;
import io.harness.rule.Owner;
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
import java.time.temporal.ChronoUnit;
import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class K8sMetricCollectorTest extends CategoryTest {
  private static final ClusterDetails CLUSTER_DETAILS = ClusterDetails.builder()
                                                            .clusterName("test-cluster-name")
                                                            .clusterId("5c14dfd1-24a2-4ccf-a95c-dbe1d2879345")
                                                            .cloudProviderId("4412d653-3c5d-46bc-ba37-d926f99c7a4a")
                                                            .kubeSystemUid("aa4062a7-d214-4642-8bb5-dfc32e750ed0")
                                                            .build();
  @Rule public final KubernetesServer server = new KubernetesServer();
  private K8sMetricCollector k8sMetricCollector;

  @Mock private EventPublisher eventPublisher;
  @Captor private ArgumentCaptor<Message> messageArgumentCaptor;
  private K8sMetricsClient k8sMetricsClient;

  @Before
  public void setUp() throws Exception {
    eventPublisher = mock(EventPublisher.class);
    k8sMetricsClient = new K8sMetricsExtensionAdapter().adapt(server.getClient());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldNotPublishMetricsIfNotAggregationWindowPassed() throws Exception {
    Instant now = Instant.now();
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
    k8sMetricCollector =
        new K8sMetricCollector(eventPublisher, k8sMetricsClient, CLUSTER_DETAILS, now.minus(10, ChronoUnit.MINUTES));
    doNothing()
        .when(eventPublisher)
        .publishMessage(messageArgumentCaptor.capture(), any(Timestamp.class),
            eq(Collections.singletonMap(HealthStatusService.CLUSTER_ID_IDENTIFIER, CLUSTER_DETAILS.getClusterId())));
    k8sMetricCollector.collectAndPublishMetrics(now);
    verifyZeroInteractions(eventPublisher);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPublishMetrics() throws Exception {
    Instant now = Instant.now();
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
    server.expect()
        .withPath("/apis/metrics.k8s.io/v1beta1/nodes")
        .andReturn(200,
            NodeMetricsList.builder()
                .items(ImmutableList.of(NodeMetrics.builder()
                                            .name("node1-name")
                                            .timestamp("2019-11-26T07:01:02Z")
                                            .window("30s")
                                            .usage(Usage.builder().cpu("826640233n").memory("7443423Ki").build())
                                            .build(),
                    NodeMetrics.builder()
                        .name("node2-name")
                        .timestamp("2019-11-26T07:00:58Z")
                        .window("30s")
                        .usage(Usage.builder().cpu("3425434234n").memory("25316652Ki").build())
                        .build()))
                .build())
        .once();
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
    server.expect()
        .withPath("/apis/metrics.k8s.io/v1beta1/pods")
        .andReturn(200,
            PodMetricsList.builder()
                .item(PodMetrics.builder()
                          .name("pod1")
                          .namespace("ns1")
                          .timestamp("2019-11-26T07:01:02Z")
                          .window("30s")
                          .container(PodMetrics.Container.builder()
                                         .name("p1-ctr1")
                                         .usage(Usage.builder().cpu("45299563n").memory("122865Ki").build())
                                         .build())
                          .build())
                .item(PodMetrics.builder()
                          .name("pod2")
                          .namespace("ns1")
                          .timestamp("2019-11-26T07:01:02Z")
                          .window("30s")
                          .container(PodMetrics.Container.builder()
                                         .name("p2-ctr1")
                                         .usage(Usage.builder().cpu("209618n").memory("9101Ki").build())
                                         .build())
                          .container(PodMetrics.Container.builder()
                                         .name("p2-ctr2")
                                         .usage(Usage.builder().cpu("647260232n").memory("240904Ki").build())
                                         .build())
                          .build())
                .build())
        .always();

    k8sMetricCollector = new K8sMetricCollector(eventPublisher, k8sMetricsClient, CLUSTER_DETAILS, now);
    doNothing()
        .when(eventPublisher)
        .publishMessage(messageArgumentCaptor.capture(), any(Timestamp.class),
            eq(Collections.singletonMap(HealthStatusService.CLUSTER_ID_IDENTIFIER, CLUSTER_DETAILS.getClusterId())));
    k8sMetricCollector.collectAndPublishMetrics(now.plus(30, ChronoUnit.SECONDS));
    k8sMetricCollector.collectAndPublishMetrics(now.plus(30, ChronoUnit.MINUTES));
    assertThat(messageArgumentCaptor.getAllValues())
        .hasSize(4)
        .containsExactlyInAnyOrder(NodeMetric.newBuilder()
                                       .setCloudProviderId(CLUSTER_DETAILS.getCloudProviderId())
                                       .setClusterId(CLUSTER_DETAILS.getClusterId())
                                       .setKubeSystemUid(CLUSTER_DETAILS.getKubeSystemUid())
                                       .setName("node1-name")
                                       .setTimestamp(HTimestamps.parse("2019-11-26T07:00:32Z"))
                                       .setWindow(Durations.fromSeconds(60))
                                       .setAggregatedUsage(AggregatedUsage.newBuilder()
                                                               .setAvgCpuNano(786640371L)
                                                               .setAvgMemoryByte(7305496064L)
                                                               .setMaxCpuNano(826640233L)
                                                               .setMaxMemoryByte(7622065152L)
                                                               .build())
                                       .build(),
            NodeMetric.newBuilder()
                .setCloudProviderId(CLUSTER_DETAILS.getCloudProviderId())
                .setClusterId(CLUSTER_DETAILS.getClusterId())
                .setKubeSystemUid(CLUSTER_DETAILS.getKubeSystemUid())
                .setName("node2-name")
                .setTimestamp(HTimestamps.parse("2019-11-26T07:00:28Z"))
                .setWindow(Durations.fromSeconds(60))
                .setAggregatedUsage(AggregatedUsage.newBuilder()
                                        .setAvgCpuNano(3182104014L)
                                        .setAvgMemoryByte(22322382848L)
                                        .setMaxCpuNano(3425434234L)
                                        .setMaxMemoryByte(25924251648L)
                                        .build())
                .build(),
            PodMetric.newBuilder()
                .setCloudProviderId(CLUSTER_DETAILS.getCloudProviderId())
                .setClusterId(CLUSTER_DETAILS.getClusterId())
                .setKubeSystemUid(CLUSTER_DETAILS.getKubeSystemUid())
                .setName("pod1")
                .setNamespace("ns1")
                .setTimestamp(HTimestamps.parse("2019-11-26T07:00:32Z"))
                .setWindow(Durations.fromSeconds(60))
                .setAggregatedUsage(AggregatedUsage.newBuilder()
                                        .setAvgCpuNano(43240492L)
                                        .setAvgMemoryByte(134230528L)
                                        .setMaxCpuNano(45299563L)
                                        .setMaxMemoryByte(142647296L)
                                        .build())
                .build(),
            PodMetric.newBuilder()
                .setCloudProviderId(CLUSTER_DETAILS.getCloudProviderId())
                .setClusterId(CLUSTER_DETAILS.getClusterId())
                .setKubeSystemUid(CLUSTER_DETAILS.getKubeSystemUid())
                .setName("pod2")
                .setNamespace("ns1")
                .setTimestamp(HTimestamps.parse("2019-11-26T07:00:32Z"))
                .setWindow(Durations.fromSeconds(60))
                .setAggregatedUsage(AggregatedUsage.newBuilder()
                                        .setAvgCpuNano(691589172)
                                        .setAvgMemoryByte(247095808L)
                                        .setMaxCpuNano(735708495)
                                        .setMaxMemoryByte(256005120L)
                                        .build())
                .build());
  }
}
