package io.harness.perpetualtask.k8s.watch;

import static io.harness.ccm.health.HealthStatusService.CLUSTER_ID_IDENTIFIER;
import static io.harness.rule.OwnerRule.AVMOHAN;
import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.client.EventPublisher;
import io.harness.event.payloads.NodeMetric;
import io.harness.event.payloads.PodMetric;
import io.harness.grpc.utils.HDurations;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskParams;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.perpetualtask.k8s.metrics.client.K8sMetricsClient;
import io.harness.perpetualtask.k8s.metrics.client.K8sMetricsExtensionAdapter;
import io.harness.perpetualtask.k8s.metrics.client.model.Usage;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetricsList;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetricsList;
import io.harness.rule.Owner;
import io.harness.serializer.KryoUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.delegatetasks.k8s.client.KubernetesClientFactory;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

import java.time.Instant;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class K8SWatchTaskExecutorTest extends CategoryTest {
  @Rule public final KubernetesServer server = new KubernetesServer();

  private K8sMetricsClient k8sMetricClient;
  private KubernetesClient client;
  private K8SWatchTaskExecutor k8SWatchTaskExecutor;

  @Mock EventPublisher eventPublisher;
  @Mock KubernetesClientFactory kubernetesClientFactory;
  @Mock K8sWatchServiceDelegate k8sWatchServiceDelegate;
  @Captor ArgumentCaptor<Message> messageArgumentCaptor;
  @Captor ArgumentCaptor<Map<String, String>> mapArgumentCaptor;

  private static final String KUBE_SYSTEM_ID = "aa4062a7-d214-4642-8bb5-dfc32e750ed0";
  private final String WATCH_ID = "watch-id";
  private final String CLUSTER_ID = "cluster-id";
  private final String POD_ONE_UID = "pod-1-uid";
  private final String POD_TWO_UID = "pod-2-uid";
  private final String NODE_ONE_UID = "node-1-uid";
  private final String NODE_TWO_UID = "node-2-uid";
  private final String CLUSTER_NAME = "cluster-name";
  private final String CLOUD_PROVIDER_ID = "cloud-provider-id";
  private final String PERPETUAL_TASK_ID = "perpetualTaskId";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    k8SWatchTaskExecutor = new K8SWatchTaskExecutor(eventPublisher, kubernetesClientFactory, k8sWatchServiceDelegate);
    k8sMetricClient = new K8sMetricsExtensionAdapter().adapt(server.getClient());
    client = server.getClient();

    server.expect()
        .withPath("/api/v1/namespaces/kube-system")
        .andReturn(200,
            new NamespaceBuilder()
                .withApiVersion("v1")
                .withKind("Namespace")
                .withNewMetadata()
                .withName("kube-system")
                .withUid(KUBE_SYSTEM_ID)
                .endMetadata()
                .build())
        .always();

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
        .always();

    server.expect().withPath("/api/v1/nodes").andReturn(200, getNodeList()).always();
    server.expect().withPath("/api/v1/pods").andReturn(200, getPodList()).always();

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
        .always();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldRunK8sPerpetualTask() throws Exception {
    Instant heartBeatTime = Instant.now();
    K8sClusterConfig k8sClusterConfig = K8sClusterConfig.builder().build();
    K8sWatchTaskParams k8sWatchTaskParams = getK8sWatchTaskParams();
    when(k8sWatchServiceDelegate.create(k8sWatchTaskParams)).thenReturn(WATCH_ID);
    when(kubernetesClientFactory.newKubernetesClient(k8sClusterConfig)).thenReturn(client);
    when(kubernetesClientFactory.newAdaptedClient(k8sClusterConfig, K8sMetricsClient.class))
        .thenReturn(k8sMetricClient);
    PerpetualTaskParams params =
        PerpetualTaskParams.newBuilder().setCustomizedParams(Any.pack(k8sWatchTaskParams)).build();
    PerpetualTaskId perpetualTaskId = PerpetualTaskId.newBuilder().setId(PERPETUAL_TASK_ID).build();
    PerpetualTaskResponse perpetualTaskResponse = k8SWatchTaskExecutor.runOnce(perpetualTaskId, params, heartBeatTime);
    assertThat(perpetualTaskResponse.getPerpetualTaskState()).isEqualTo(PerpetualTaskState.TASK_RUN_SUCCEEDED);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPublishNodeMetrics() throws Exception {
    Instant heartbeatTime = Instant.now();
    doNothing()
        .when(eventPublisher)
        .publishMessage(
            messageArgumentCaptor.capture(), eq(HTimestamps.fromInstant(heartbeatTime)), mapArgumentCaptor.capture());
    K8SWatchTaskExecutor.publishNodeMetrics(k8sMetricClient, eventPublisher, getK8sWatchTaskParams(), heartbeatTime);
    assertThat(messageArgumentCaptor.getAllValues())
        .hasSize(2)
        .containsExactlyInAnyOrder(NodeMetric.newBuilder()
                                       .setCloudProviderId(CLOUD_PROVIDER_ID)
                                       .setClusterId(CLUSTER_ID)
                                       .setKubeSystemUid(KUBE_SYSTEM_ID)
                                       .setName("node1-name")
                                       .setTimestamp(HTimestamps.parse("2019-11-26T07:00:32Z"))
                                       .setWindow(HDurations.parse("30s"))
                                       .setUsage(io.harness.event.payloads.Usage.newBuilder()
                                                     .setCpuNano(746640510L)
                                                     .setMemoryByte(6825124L * 1024)
                                                     .build())
                                       .build(),
            NodeMetric.newBuilder()
                .setCloudProviderId(CLOUD_PROVIDER_ID)
                .setClusterId(CLUSTER_ID)
                .setKubeSystemUid(KUBE_SYSTEM_ID)
                .setName("node2-name")
                .setTimestamp(HTimestamps.parse("2019-11-26T07:00:28Z"))
                .setWindow(HDurations.parse("30s"))
                .setUsage(io.harness.event.payloads.Usage.newBuilder()
                              .setCpuNano(2938773795L)
                              .setMemoryByte(18281752L * 1024)
                              .build())
                .build());
    assertThat(mapArgumentCaptor.getValue().keySet()).contains(CLUSTER_ID_IDENTIFIER);
  }

  private K8sWatchTaskParams getK8sWatchTaskParams() {
    K8sClusterConfig config = K8sClusterConfig.builder().build();
    ByteString bytes = ByteString.copyFrom(KryoUtils.asBytes(config));

    return K8sWatchTaskParams.newBuilder()
        .setCloudProviderId(CLOUD_PROVIDER_ID)
        .setClusterId(CLUSTER_ID)
        .setClusterName(CLUSTER_NAME)
        .setK8SClusterConfig(bytes)
        .build();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldPublishClusterSyncEvent() throws Exception {
    K8sWatchTaskParams k8sWatchTaskParams = getK8sWatchTaskParams();
    Instant pollTime = Instant.now();
    doNothing()
        .when(eventPublisher)
        .publishMessage(
            messageArgumentCaptor.capture(), eq(HTimestamps.fromInstant(pollTime)), mapArgumentCaptor.capture());
    K8SWatchTaskExecutor.publishClusterSyncEvent(client, eventPublisher, k8sWatchTaskParams, pollTime);
    assertThat(messageArgumentCaptor.getAllValues())
        .hasSize(1)
        .contains(K8SClusterSyncEvent.newBuilder()
                      .setClusterId(CLUSTER_ID)
                      .setClusterName(CLUSTER_NAME)
                      .setCloudProviderId(CLOUD_PROVIDER_ID)
                      .setKubeSystemUid(KUBE_SYSTEM_ID)
                      .addAllActivePodUids(ImmutableList.of(POD_ONE_UID, POD_TWO_UID))
                      .addAllActiveNodeUids(ImmutableList.of(NODE_ONE_UID, NODE_TWO_UID))
                      .setLastProcessedTimestamp(HTimestamps.fromInstant(pollTime))
                      .build());
    assertThat(mapArgumentCaptor.getValue().keySet()).contains(CLUSTER_ID_IDENTIFIER);
  }

  private NodeList getNodeList() {
    NodeList nodeList = new NodeList();
    nodeList.setItems(ImmutableList.of(getNode(NODE_ONE_UID), getNode(NODE_TWO_UID)));
    return nodeList;
  }

  private PodList getPodList() {
    PodList podList = new PodList();
    podList.setItems(ImmutableList.of(getPod(POD_ONE_UID), getPod(POD_TWO_UID)));
    return podList;
  }

  private Node getNode(String nodeUid) {
    Node firstNode = new Node();
    firstNode.setMetadata(getObjectMeta(nodeUid));
    return firstNode;
  }

  private ObjectMeta getObjectMeta(String uid) {
    ObjectMeta objectMeta = new ObjectMeta();
    objectMeta.setUid(uid);
    return objectMeta;
  }

  private Pod getPod(String podUid) {
    Pod firstPod = new Pod();
    firstPod.setMetadata(getObjectMeta(podUid));
    return firstPod;
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPublishPodMetrics() throws Exception {
    Instant heartbeatTime = Instant.now();
    doNothing()
        .when(eventPublisher)
        .publishMessage(
            messageArgumentCaptor.capture(), eq(HTimestamps.fromInstant(heartbeatTime)), mapArgumentCaptor.capture());
    K8SWatchTaskExecutor.publishPodMetrics(k8sMetricClient, eventPublisher, getK8sWatchTaskParams(), heartbeatTime);
    assertThat(messageArgumentCaptor.getAllValues())
        .hasSize(2)
        .containsExactlyInAnyOrder(PodMetric.newBuilder()
                                       .setCloudProviderId(CLOUD_PROVIDER_ID)
                                       .setClusterId(CLUSTER_ID)
                                       .setKubeSystemUid(KUBE_SYSTEM_ID)
                                       .setName("pod1")
                                       .setNamespace("ns1")
                                       .setTimestamp(HTimestamps.parse("2019-11-26T07:00:32Z"))
                                       .setWindow(HDurations.parse("30s"))
                                       .addContainers(PodMetric.Container.newBuilder()
                                                          .setName("p1-ctr1")
                                                          .setUsage(io.harness.event.payloads.Usage.newBuilder()
                                                                        .setCpuNano(41181421L)
                                                                        .setMemoryByte(139304L * 1024)
                                                                        .build())
                                                          .build())
                                       .build(),
            PodMetric.newBuilder()
                .setCloudProviderId(CLOUD_PROVIDER_ID)
                .setClusterId(CLUSTER_ID)
                .setKubeSystemUid(KUBE_SYSTEM_ID)
                .setName("pod2")
                .setNamespace("ns1")
                .setTimestamp(HTimestamps.parse("2019-11-26T07:00:32Z"))
                .setWindow(HDurations.parse("30s"))
                .addContainers(PodMetric.Container.newBuilder()
                                   .setName("p2-ctr1")
                                   .setUsage(io.harness.event.payloads.Usage.newBuilder()
                                                 .setCpuNano(185503L)
                                                 .setMemoryByte(7460L * 1024)
                                                 .build())
                                   .build())
                .addContainers(PodMetric.Container.newBuilder()
                                   .setName("p2-ctr2")
                                   .setUsage(io.harness.event.payloads.Usage.newBuilder()
                                                 .setCpuNano(735522992L)
                                                 .setMemoryByte(225144L * 1024)
                                                 .build())
                                   .build())
                .build());
    assertThat(mapArgumentCaptor.getValue().keySet()).contains(CLUSTER_ID_IDENTIFIER);
  }
}
