package io.harness.perpetualtask.k8s.watch;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.delegatetasks.k8s.client.KubernetesClientFactory;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

import java.util.UUID;

@Slf4j
public class K8sWatchServiceDelegateTest extends CategoryTest {
  private static final String RESOURCE_KIND_POD = "Pod";
  private static final String RESOURCE_KIND_NODE = "Node";

  private K8sWatchServiceDelegate k8sWatchServiceDelegate;
  private WatcherFactory watcherFactory;

  private PodWatcher podWatcher;
  private NodeWatcher nodeWatcher;

  @Before
  public void setUp() throws Exception {
    KubernetesClientFactory kubernetesClientFactory = mock(KubernetesClientFactory.class);
    watcherFactory = mock(WatcherFactory.class);
    this.k8sWatchServiceDelegate = new K8sWatchServiceDelegate(watcherFactory, kubernetesClientFactory);
    podWatcher = mock(PodWatcher.class);
    nodeWatcher = mock(NodeWatcher.class);
    when(watcherFactory.createPodWatcher(any(), any())).thenReturn(podWatcher);
    when(watcherFactory.createNodeWatcher(any(), any())).thenReturn(nodeWatcher);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldCreatePodWatch() throws Exception {
    String cloudProviderId = UUID.randomUUID().toString();
    ByteString k8sClusterConfig = ByteString.copyFrom(KryoUtils.asBytes(
        K8sClusterConfig.builder().clusterName("test-cluster").namespace("namespace").cloudProvider(null).build()));
    K8sWatchTaskParams k8sWatchTaskParams = K8sWatchTaskParams.newBuilder()
                                                .setK8SResourceKind(RESOURCE_KIND_POD)
                                                .setK8SClusterConfig(k8sClusterConfig)
                                                .setCloudProviderId(cloudProviderId)
                                                .build();
    String watchId = k8sWatchServiceDelegate.create(k8sWatchTaskParams);
    assertThat(watchId).isNotNull();
    assertThat(k8sWatchServiceDelegate.watchIds()).contains(watchId);
    verify(watcherFactory, atLeastOnce()).createPodWatcher(any(), any());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldCreateNodeWatch() throws Exception {
    String cloudProviderId = UUID.randomUUID().toString();
    ByteString k8sClusterConfig = ByteString.copyFrom(KryoUtils.asBytes(
        K8sClusterConfig.builder().clusterName("test-cluster").namespace("namespace").cloudProvider(null).build()));
    K8sWatchTaskParams k8sWatchTaskParams = K8sWatchTaskParams.newBuilder()
                                                .setK8SResourceKind(RESOURCE_KIND_NODE)
                                                .setK8SClusterConfig(k8sClusterConfig)
                                                .setCloudProviderId(cloudProviderId)
                                                .build();
    String watchId = k8sWatchServiceDelegate.create(k8sWatchTaskParams);
    assertThat(watchId).isNotNull();
    assertThat(k8sWatchServiceDelegate.watchIds()).contains(watchId);
    verify(watcherFactory, atLeastOnce()).createNodeWatcher(any(), any());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldNotCreateDuplicateWatch() throws Exception {
    String cloudProviderId = UUID.randomUUID().toString();
    ByteString k8sClusterConfig = ByteString.copyFrom(KryoUtils.asBytes(
        K8sClusterConfig.builder().clusterName("test-cluster").namespace("namespace").cloudProvider(null).build()));
    K8sWatchTaskParams k8sWatchTaskParams = K8sWatchTaskParams.newBuilder()
                                                .setK8SResourceKind(RESOURCE_KIND_POD)
                                                .setK8SClusterConfig(k8sClusterConfig)
                                                .setCloudProviderId(cloudProviderId)
                                                .build();
    String watch1 = k8sWatchServiceDelegate.create(k8sWatchTaskParams);
    String watch2 = k8sWatchServiceDelegate.create(k8sWatchTaskParams);
    assertThat(watch2).isEqualTo(watch1);
    verify(watcherFactory, times(1)).createPodWatcher(any(KubernetesClient.class), any());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldCreateMultipleWatchesIfNotDuplicate() throws Exception {
    String cloudProviderId1 = UUID.randomUUID().toString();
    ByteString k8sClusterConfig1 = ByteString.copyFrom(KryoUtils.asBytes(
        K8sClusterConfig.builder().clusterName("test-cluster1").namespace("namespace1").cloudProvider(null).build()));
    K8sWatchTaskParams k8sWatchTaskParams1 = K8sWatchTaskParams.newBuilder()
                                                 .setK8SResourceKind(RESOURCE_KIND_POD)
                                                 .setK8SClusterConfig(k8sClusterConfig1)
                                                 .setCloudProviderId(cloudProviderId1)
                                                 .build();
    String cloudProviderId2 = UUID.randomUUID().toString();
    ByteString k8sClusterConfig2 = ByteString.copyFrom(KryoUtils.asBytes(
        K8sClusterConfig.builder().clusterName("test-cluster2").namespace("namespace2").cloudProvider(null).build()));
    K8sWatchTaskParams k8sWatchTaskParams2 = K8sWatchTaskParams.newBuilder()
                                                 .setK8SResourceKind(RESOURCE_KIND_POD)
                                                 .setK8SClusterConfig(k8sClusterConfig2)
                                                 .setCloudProviderId(cloudProviderId2)
                                                 .build();
    String watch1 = k8sWatchServiceDelegate.create(k8sWatchTaskParams1);
    String watch2 = k8sWatchServiceDelegate.create(k8sWatchTaskParams2);
    assertThat(watch2).isNotEqualTo(watch1);
    verify(watcherFactory, times(2)).createPodWatcher(any(KubernetesClient.class), any());
    assertThat(k8sWatchServiceDelegate.watchIds()).containsExactlyInAnyOrder(watch1, watch2);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldDeletePodWatch() throws Exception {
    String cloudProviderId = UUID.randomUUID().toString();
    ByteString k8sClusterConfig = ByteString.copyFrom(KryoUtils.asBytes(
        K8sClusterConfig.builder().clusterName("test-cluster").namespace("namespace").cloudProvider(null).build()));
    K8sWatchTaskParams k8sWatchTaskParams = K8sWatchTaskParams.newBuilder()
                                                .setK8SResourceKind(RESOURCE_KIND_POD)
                                                .setK8SClusterConfig(k8sClusterConfig)
                                                .setCloudProviderId(cloudProviderId)
                                                .build();
    String watchId = k8sWatchServiceDelegate.create(k8sWatchTaskParams);
    assertThat(k8sWatchServiceDelegate.watchIds()).contains(watchId);
    k8sWatchServiceDelegate.delete(watchId);
    verify(podWatcher).onClose(null);
    assertThat(k8sWatchServiceDelegate.watchIds()).doesNotContain(watchId);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldDeleteNodeWatch() throws Exception {
    String cloudProviderId = UUID.randomUUID().toString();
    ByteString k8sClusterConfig = ByteString.copyFrom(KryoUtils.asBytes(
        K8sClusterConfig.builder().clusterName("test-cluster").namespace("namespace").cloudProvider(null).build()));
    K8sWatchTaskParams k8sWatchTaskParams = K8sWatchTaskParams.newBuilder()
                                                .setK8SResourceKind(RESOURCE_KIND_NODE)
                                                .setK8SClusterConfig(k8sClusterConfig)
                                                .setCloudProviderId(cloudProviderId)
                                                .build();
    String watchId = k8sWatchServiceDelegate.create(k8sWatchTaskParams);
    assertThat(k8sWatchServiceDelegate.watchIds()).contains(watchId);
    k8sWatchServiceDelegate.delete(watchId);
    verify(nodeWatcher).onClose(null);
    assertThat(k8sWatchServiceDelegate.watchIds()).doesNotContain(watchId);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldThrowIaeIfUnknownResourceKind() throws Exception {
    String cloudProviderId = UUID.randomUUID().toString();
    ByteString k8sClusterConfig = ByteString.copyFrom(KryoUtils.asBytes(
        K8sClusterConfig.builder().clusterName("test-cluster").namespace("namespace").cloudProvider(null).build()));
    K8sWatchTaskParams k8sWatchTaskParams = K8sWatchTaskParams.newBuilder()
                                                .setK8SResourceKind("Deployment")
                                                .setK8SClusterConfig(k8sClusterConfig)
                                                .setCloudProviderId(cloudProviderId)
                                                .build();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> k8sWatchServiceDelegate.create(k8sWatchTaskParams))
        .withMessage("Resource kind %s is not watchable", "Deployment");
  }
}