package io.harness.perpetualtask.k8s.watch;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.harness.DelegateTest;
import io.harness.category.element.UnitTests;
import io.harness.k8s.apiclient.ApiClientFactory;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.informer.SharedInformerFactoryFactory;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.V1NamespaceBuilder;
import io.kubernetes.client.util.ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

import java.util.UUID;

@Slf4j
public class K8sWatchServiceDelegateTest extends DelegateTest {
  private static final String CLUSTER_ID = "cluster-id";
  private K8sWatchServiceDelegate k8sWatchServiceDelegate;
  private WatcherFactory watcherFactory;

  @Rule public WireMockRule wireMockRule = new WireMockRule(65221);
  @Inject KryoSerializer kryoSerializer;

  private static final String URL_REGEX_SUFFIX = "(\\?(.*))?";

  @Before
  public void setUp() throws Exception {
    watcherFactory = mock(WatcherFactory.class);
    SharedInformerFactoryFactory sharedInformerFactoryFactory = mock(SharedInformerFactoryFactory.class);
    ApiClientFactory apiClientFactory = mock(ApiClientFactory.class);
    ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper = mock(ContainerDeploymentDelegateHelper.class);

    this.k8sWatchServiceDelegate = new K8sWatchServiceDelegate(watcherFactory, sharedInformerFactoryFactory,
        apiClientFactory, kryoSerializer, containerDeploymentDelegateHelper);

    SharedInformerFactory sharedInformerFactory = mock(SharedInformerFactory.class);
    when(sharedInformerFactoryFactory.createSharedInformerFactory(any(), any())).thenReturn(sharedInformerFactory);
    SharedIndexInformer sharedIndexInformer = mock(SharedIndexInformer.class);
    when(sharedInformerFactory.getExistingSharedIndexInformer(any())).thenReturn(sharedIndexInformer);
    when(sharedIndexInformer.hasSynced()).thenReturn(true);

    Indexer indexer = mock(Indexer.class);
    when(sharedIndexInformer.getIndexer()).thenReturn(indexer);

    when(apiClientFactory.getClient(any()))
        .thenReturn(new ClientBuilder().setBasePath("http://localhost:" + wireMockRule.port()).build());

    stubFor(get(urlMatching("^/api/v1/namespaces/kube-system" + URL_REGEX_SUFFIX))
                .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(new JSON().serialize(new V1NamespaceBuilder()
                                                                   .withKind("Namespace")
                                                                   .withApiVersion("v1")
                                                                   .withNewMetadata()
                                                                   .withName("kube-system")
                                                                   .withUid("ed044e6a-8b7f-456c-b035-f05e9ce56a60")
                                                                   .endMetadata()
                                                                   .build()))));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldCreateAllWatchersAndFetchers() throws Exception {
    String cloudProviderId = UUID.randomUUID().toString();

    ByteString k8sClusterConfig = ByteString.copyFrom(kryoSerializer.asBytes(
        K8sClusterConfig.builder().clusterName("test-cluster").namespace("namespace").cloudProvider(null).build()));

    K8sWatchTaskParams k8sWatchTaskParams = K8sWatchTaskParams.newBuilder()
                                                .setK8SClusterConfig(k8sClusterConfig)
                                                .setCloudProviderId(cloudProviderId)
                                                .setClusterId(CLUSTER_ID)
                                                .build();
    String watchId = k8sWatchServiceDelegate.create(k8sWatchTaskParams);

    assertThat(watchId).isNotNull();
    assertThat(k8sWatchServiceDelegate.watchIds()).contains(watchId);

    verify(watcherFactory, atLeastOnce()).createPodWatcher(any(), any(), any(), any(), any(), any());
    verify(watcherFactory, atLeastOnce()).createNodeWatcher(any(), any(), any());
    verify(watcherFactory, atLeastOnce()).createPVCFetcher(any(ApiClient.class), any(SharedInformerFactory.class));
    verify(watcherFactory, atLeastOnce())
        .createPVWatcher(any(ApiClient.class), any(ClusterDetails.class), any(SharedInformerFactory.class));
    verify(watcherFactory, atLeastOnce())
        .createNamespaceFetcher(any(ApiClient.class), any(SharedInformerFactory.class));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldCreateClusterEventWatch() throws Exception {
    String cloudProviderId = UUID.randomUUID().toString();
    ByteString k8sClusterConfig = ByteString.copyFrom(kryoSerializer.asBytes(
        K8sClusterConfig.builder().clusterName("test-cluster").namespace("namespace").cloudProvider(null).build()));
    K8sWatchTaskParams k8sWatchTaskParams = K8sWatchTaskParams.newBuilder()
                                                .setK8SClusterConfig(k8sClusterConfig)
                                                .setCloudProviderId(cloudProviderId)
                                                .setClusterId(CLUSTER_ID)
                                                .build();
    String watchId = k8sWatchServiceDelegate.create(k8sWatchTaskParams);
    assertThat(watchId).isNotNull();
    assertThat(k8sWatchServiceDelegate.watchIds()).contains(watchId);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldNotCreateDuplicateWatch() throws Exception {
    String cloudProviderId = UUID.randomUUID().toString();
    ByteString k8sClusterConfig = ByteString.copyFrom(kryoSerializer.asBytes(
        K8sClusterConfig.builder().clusterName("test-cluster").namespace("namespace").cloudProvider(null).build()));
    K8sWatchTaskParams k8sWatchTaskParams = K8sWatchTaskParams.newBuilder()
                                                .setK8SClusterConfig(k8sClusterConfig)
                                                .setCloudProviderId(cloudProviderId)
                                                .setClusterId(CLUSTER_ID)
                                                .build();
    String watch1 = k8sWatchServiceDelegate.create(k8sWatchTaskParams);
    String watch2 = k8sWatchServiceDelegate.create(k8sWatchTaskParams);
    assertThat(watch2).isEqualTo(watch1);
    verify(watcherFactory, times(1)).createPodWatcher(any(ApiClient.class), any(), any(), any(), any(), any());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldCreateMultipleWatchesIfNotDuplicate() throws Exception {
    String cloudProviderId1 = UUID.randomUUID().toString();
    ByteString k8sClusterConfig1 = ByteString.copyFrom(kryoSerializer.asBytes(
        K8sClusterConfig.builder().clusterName("test-cluster1").namespace("namespace1").cloudProvider(null).build()));
    K8sWatchTaskParams k8sWatchTaskParams1 = K8sWatchTaskParams.newBuilder()
                                                 .setClusterId("clusterId1")
                                                 .setK8SClusterConfig(k8sClusterConfig1)
                                                 .setCloudProviderId(cloudProviderId1)
                                                 .setClusterId(CLUSTER_ID)
                                                 .build();
    String cloudProviderId2 = UUID.randomUUID().toString();
    ByteString k8sClusterConfig2 = ByteString.copyFrom(kryoSerializer.asBytes(
        K8sClusterConfig.builder().clusterName("test-cluster2").namespace("namespace2").cloudProvider(null).build()));
    K8sWatchTaskParams k8sWatchTaskParams2 = K8sWatchTaskParams.newBuilder()
                                                 .setClusterId("clusterId2")
                                                 .setK8SClusterConfig(k8sClusterConfig2)
                                                 .setCloudProviderId(cloudProviderId2)
                                                 .build();
    String watch1 = k8sWatchServiceDelegate.create(k8sWatchTaskParams1);
    String watch2 = k8sWatchServiceDelegate.create(k8sWatchTaskParams2);
    assertThat(watch2).isNotEqualTo(watch1);
    verify(watcherFactory, times(2)).createPodWatcher(any(ApiClient.class), any(), any(), any(), any(), any());
    assertThat(k8sWatchServiceDelegate.watchIds()).containsExactlyInAnyOrder(watch1, watch2);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldDeletePodWatch() throws Exception {
    String cloudProviderId = UUID.randomUUID().toString();
    ByteString k8sClusterConfig = ByteString.copyFrom(kryoSerializer.asBytes(
        K8sClusterConfig.builder().clusterName("test-cluster").namespace("namespace").cloudProvider(null).build()));
    K8sWatchTaskParams k8sWatchTaskParams = K8sWatchTaskParams.newBuilder()
                                                .setK8SClusterConfig(k8sClusterConfig)
                                                .setCloudProviderId(cloudProviderId)
                                                .setClusterId(CLUSTER_ID)
                                                .build();
    String watchId = k8sWatchServiceDelegate.create(k8sWatchTaskParams);
    assertThat(k8sWatchServiceDelegate.watchIds()).contains(watchId);
    k8sWatchServiceDelegate.delete(watchId);
    assertThat(k8sWatchServiceDelegate.watchIds()).doesNotContain(watchId);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldDeleteNodeWatch() throws Exception {
    String cloudProviderId = UUID.randomUUID().toString();
    ByteString k8sClusterConfig = ByteString.copyFrom(kryoSerializer.asBytes(
        K8sClusterConfig.builder().clusterName("test-cluster").namespace("namespace").cloudProvider(null).build()));
    K8sWatchTaskParams k8sWatchTaskParams = K8sWatchTaskParams.newBuilder()
                                                .setK8SClusterConfig(k8sClusterConfig)
                                                .setCloudProviderId(cloudProviderId)
                                                .setClusterId(CLUSTER_ID)
                                                .build();
    String watchId = k8sWatchServiceDelegate.create(k8sWatchTaskParams);
    assertThat(k8sWatchServiceDelegate.watchIds()).contains(watchId);
    k8sWatchServiceDelegate.delete(watchId);
    assertThat(k8sWatchServiceDelegate.watchIds()).doesNotContain(watchId);
  }
}
