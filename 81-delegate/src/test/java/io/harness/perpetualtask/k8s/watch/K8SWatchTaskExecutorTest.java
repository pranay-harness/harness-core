package io.harness.perpetualtask.k8s.watch;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.harness.ccm.health.HealthStatusService.CLUSTER_ID_IDENTIFIER;
import static io.harness.rule.OwnerRule.AVMOHAN;
import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.harness.DelegateTest;
import io.harness.category.element.UnitTests;
import io.harness.event.client.EventPublisher;
import io.harness.grpc.utils.HTimestamps;
import io.harness.k8s.apiclient.ApiClientFactoryImpl;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.perpetualtask.PerpetualTaskExecutionParams;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.perpetualtask.k8s.metrics.client.impl.DefaultK8sMetricsClient;
import io.harness.perpetualtask.k8s.metrics.client.model.Usage;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetricsList;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetricsList;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1NamespaceBuilder;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeBuilder;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1NodeListBuilder;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ObjectMetaBuilder;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodBuilder;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodListBuilder;
import io.kubernetes.client.util.ClientBuilder;
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
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

import java.time.Instant;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class K8SWatchTaskExecutorTest extends DelegateTest {
  @Rule public final WireMockRule wireMockRule = new WireMockRule(65217);

  private ApiClient apiClient;
  private DefaultK8sMetricsClient k8sMetricClient;
  private K8SWatchTaskExecutor k8SWatchTaskExecutor;

  @Mock EventPublisher eventPublisher;
  @Mock K8sWatchServiceDelegate k8sWatchServiceDelegate;
  @Mock ApiClientFactoryImpl apiClientFactory;
  @Mock ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Captor ArgumentCaptor<Message> messageArgumentCaptor;
  @Captor ArgumentCaptor<Map<String, String>> mapArgumentCaptor;

  @Inject KryoSerializer kryoSerializer;

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

    apiClient = new ClientBuilder().setBasePath("http://localhost:" + wireMockRule.port()).build();
    doReturn(apiClient).when(apiClientFactory).getClient(any(KubernetesConfig.class));

    k8SWatchTaskExecutor = new K8SWatchTaskExecutor(
        eventPublisher, k8sWatchServiceDelegate, apiClientFactory, kryoSerializer, containerDeploymentDelegateHelper);

    stubFor(get(urlPathEqualTo("/api/v1/namespaces/kube-system"))
                .willReturn(aResponse().withStatus(200).withBody(new Gson().toJson(new V1NamespaceBuilder()
                                                                                       .withApiVersion("v1")
                                                                                       .withKind("Namespace")
                                                                                       .withNewMetadata()
                                                                                       .withName("kube-system")
                                                                                       .withUid(KUBE_SYSTEM_ID)
                                                                                       .endMetadata()
                                                                                       .build()))));

    stubFor(
        get(urlPathEqualTo("/apis/metrics.k8s.io/v1beta1/nodes"))
            .willReturn(aResponse().withStatus(200).withBody(new Gson().toJson(
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
                    .build()))));

    stubFor(get(urlPathEqualTo("/api/v1/nodes"))
                .willReturn(aResponse().withStatus(200).withBody(new Gson().toJson(getNodeList()))));
    stubFor(get(urlPathEqualTo("/api/v1/pods"))
                .willReturn(aResponse().withStatus(200).withBody(new Gson().toJson(getPodList()))));

    stubFor(get(urlPathEqualTo("/apis/metrics.k8s.io/v1beta1/pods"))
                .willReturn(aResponse().withStatus(200).withBody(new Gson().toJson(
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
                        .build()))));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldRunK8sPerpetualTask() {
    Instant heartBeatTime = Instant.now();
    K8sWatchTaskParams k8sWatchTaskParams = getK8sWatchTaskParams();
    when(k8sWatchServiceDelegate.create(k8sWatchTaskParams)).thenReturn(WATCH_ID);
    PerpetualTaskExecutionParams params =
        PerpetualTaskExecutionParams.newBuilder().setCustomizedParams(Any.pack(k8sWatchTaskParams)).build();
    PerpetualTaskId perpetualTaskId = PerpetualTaskId.newBuilder().setId(PERPETUAL_TASK_ID).build();
    PerpetualTaskResponse perpetualTaskResponse = k8SWatchTaskExecutor.runOnce(perpetualTaskId, params, heartBeatTime);
    assertThat(perpetualTaskResponse.getPerpetualTaskState()).isEqualTo(PerpetualTaskState.TASK_RUN_SUCCEEDED);
  }

  private K8sWatchTaskParams getK8sWatchTaskParams() {
    K8sClusterConfig config = K8sClusterConfig.builder().build();
    ByteString bytes = ByteString.copyFrom(kryoSerializer.asBytes(config));

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
    k8sMetricClient = new DefaultK8sMetricsClient(apiClient);
    K8sWatchTaskParams k8sWatchTaskParams = getK8sWatchTaskParams();
    Instant pollTime = Instant.now();
    doNothing()
        .when(eventPublisher)
        .publishMessage(
            messageArgumentCaptor.capture(), eq(HTimestamps.fromInstant(pollTime)), mapArgumentCaptor.capture());
    K8SWatchTaskExecutor.publishClusterSyncEvent(k8sMetricClient, eventPublisher, k8sWatchTaskParams, pollTime);
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

  private V1NodeList getNodeList() {
    return new V1NodeListBuilder().withItems(ImmutableList.of(getNode(NODE_ONE_UID), getNode(NODE_TWO_UID))).build();
  }

  private V1PodList getPodList() {
    return new V1PodListBuilder().withItems(ImmutableList.of(getPod(POD_ONE_UID), getPod(POD_TWO_UID))).build();
  }

  private V1Node getNode(String nodeUid) {
    return new V1NodeBuilder().withMetadata(getObjectMeta(nodeUid)).build();
  }

  private V1ObjectMeta getObjectMeta(String uid) {
    return new V1ObjectMetaBuilder().withUid(uid).build();
  }

  private V1Pod getPod(String podUid) {
    return new V1PodBuilder()
        .withMetadata(getObjectMeta(podUid))
        .withNewStatus()
        .withPhase("Running")
        .endStatus()
        .build();
  }
}
