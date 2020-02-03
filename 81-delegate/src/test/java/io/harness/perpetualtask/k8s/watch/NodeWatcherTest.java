package io.harness.perpetualtask.k8s.watch;

import static io.harness.ccm.health.HealthStatusService.CLUSTER_ID_IDENTIFIER;
import static io.harness.grpc.utils.HTimestamps.toInstant;
import static io.harness.perpetualtask.k8s.watch.NodeEvent.EventType.EVENT_TYPE_START;
import static io.harness.perpetualtask.k8s.watch.NodeEvent.EventType.EVENT_TYPE_STOP;
import static io.harness.rule.OwnerRule.AVMOHAN;
import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;

import io.fabric8.kubernetes.api.model.DoneableNode;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.NodeSpec;
import io.fabric8.kubernetes.api.model.NodeStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.client.EventPublisher;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NodeWatcherTest extends CategoryTest {
  private static final String UID = UUID.randomUUID().toString();
  private static final String NAME = "test-node";
  private static final String GCP_PROVIDER_ID = "gce://ccm-play/us-east4-a/gke-ccm-test-default-pool-d13df1f8-zk7p";

  private NodeWatcher nodeWatcher;
  private EventPublisher eventPublisher;
  private Watch watch;
  @Captor ArgumentCaptor<Map<String, String>> mapArgumentCaptor;

  @Before
  public void setUp() throws Exception {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    eventPublisher = mock(EventPublisher.class);
    @SuppressWarnings("unchecked")
    NonNamespaceOperation<Node, NodeList, DoneableNode, Resource<Node, DoneableNode>> nodeOps =
        mock(NonNamespaceOperation.class);
    watch = mock(Watch.class);
    when(kubernetesClient.nodes()).thenReturn(nodeOps);
    when(nodeOps.watch(any())).thenReturn(watch);
    nodeWatcher = new NodeWatcher(kubernetesClient,
        K8sWatchTaskParams.newBuilder()
            .setClusterName("clusterName")
            .setClusterId("clusterId")
            .setCloudProviderId("cloud-provider-id")
            .build(),
        eventPublisher);
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPublishNodeStartedEvent() throws Exception {
    Instant creationTime = now().minus(5, ChronoUnit.MINUTES);
    Node node = node(UID, NAME, creationTime.toString(), new HashMap<>(), nodeStatus(), GCP_PROVIDER_ID);
    nodeWatcher.eventReceived(Action.ADDED, node);
    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(eventPublisher, times(2))
        .publishMessage(captor.capture(), any(Timestamp.class), mapArgumentCaptor.capture());
    assertThat(captor.getAllValues().get(1)).isInstanceOfSatisfying(NodeEvent.class, nodeEvent -> {
      assertThat(nodeEvent.getNodeUid()).isEqualTo(UID);
      assertThat(nodeEvent.getNodeName()).isEqualTo(NAME);
      assertThat(nodeEvent.getType()).isEqualTo(EVENT_TYPE_START);
      assertThat(toInstant(nodeEvent.getTimestamp())).isEqualTo(creationTime);
      assertThat(mapArgumentCaptor.getValue().keySet()).contains(CLUSTER_ID_IDENTIFIER);
    });
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPublishNodeStoppedEvent() throws Exception {
    String creationTimestamp = now().minus(5, ChronoUnit.MINUTES).toString();
    Node node = node(UID, NAME, creationTimestamp, new HashMap<>(), nodeStatus(), GCP_PROVIDER_ID);
    nodeWatcher.eventReceived(Action.DELETED, node);
    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(eventPublisher, times(2))
        .publishMessage(captor.capture(), any(Timestamp.class), mapArgumentCaptor.capture());
    assertThat(captor.getAllValues().get(1)).isInstanceOfSatisfying(NodeEvent.class, nodeEvent -> {
      assertThat(nodeEvent.getNodeUid()).isEqualTo(UID);
      assertThat(nodeEvent.getNodeName()).isEqualTo(NAME);
      assertThat(nodeEvent.getType()).isEqualTo(EVENT_TYPE_STOP);
      // approximate check as the time is measured within the NodeWatcher code.
      assertThat(toInstant(nodeEvent.getTimestamp())).isCloseTo(now(), within(5, ChronoUnit.SECONDS));
      assertThat(mapArgumentCaptor.getValue().keySet()).contains(CLUSTER_ID_IDENTIFIER);
    });
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPublishNodeInfoOnlyOnce() throws Exception {
    Instant creationTime = now().minus(5, ChronoUnit.MINUTES);
    Map<String, String> labels = ImmutableMap.of("k1", "v1", "k2", "v2");
    Node node = node(UID, NAME, creationTime.toString(), labels, nodeStatus(), GCP_PROVIDER_ID);
    nodeWatcher.eventReceived(Action.ADDED, node);
    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(eventPublisher, times(2))
        .publishMessage(captor.capture(), any(Timestamp.class), mapArgumentCaptor.capture());
    assertThat(captor.getAllValues().get(0)).isInstanceOfSatisfying(NodeInfo.class, nodeInfo -> {
      assertThat(nodeInfo.getNodeUid()).isEqualTo(UID);
      assertThat(nodeInfo.getNodeName()).isEqualTo(NAME);
      assertThat(toInstant(nodeInfo.getCreationTime())).isEqualTo(creationTime);
      assertThat(nodeInfo.getLabelsMap()).isEqualTo(labels);
      assertThat(mapArgumentCaptor.getValue().keySet().contains(CLUSTER_ID_IDENTIFIER));
    });
    Mockito.reset(eventPublisher);
    captor = ArgumentCaptor.forClass(Message.class);
    nodeWatcher.eventReceived(Action.DELETED, node);
    verify(eventPublisher).publishMessage(captor.capture(), any(Timestamp.class), mapArgumentCaptor.capture());
    assertThat(captor.getAllValues()).hasSize(1).isNotInstanceOfAny(NodeInfo.class);
    assertThat(mapArgumentCaptor.getValue().keySet()).contains(CLUSTER_ID_IDENTIFIER);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldCloseUnderlyingWatchOnClosingWatcher() throws Exception {
    nodeWatcher.onClose(null);
    verify(watch).close();
  }

  private NodeStatus nodeStatus() {
    Map<String, Quantity> allocatable = new HashMap<>();
    NodeStatus nodeStatus = new NodeStatus();
    nodeStatus.setAllocatable(allocatable);
    return nodeStatus;
  }

  private Node node(String uid, String name, String creationTimestamp, Map<String, String> labels,
      NodeStatus nodeStatus, String providerId) {
    Node node = new Node();
    ObjectMeta nodeMeta = new ObjectMeta();
    nodeMeta.setUid(uid);
    nodeMeta.setName(name);
    nodeMeta.setCreationTimestamp(creationTimestamp);
    nodeMeta.setLabels(labels);
    node.setMetadata(nodeMeta);
    node.setStatus(nodeStatus);
    NodeSpec spec = new NodeSpec();
    spec.setProviderID(providerId);
    node.setSpec(spec);
    return node;
  }
}
