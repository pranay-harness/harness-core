package io.harness.perpetualtask.k8s.watch;

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

import io.fabric8.kubernetes.api.model.DoneableNode;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.harness.category.element.UnitTests;
import io.harness.event.client.EventPublisher;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NodeWatcherTest {
  private static final String UID = UUID.randomUUID().toString();
  private static final String NAME = "test-node";

  private NodeWatcher nodeWatcher;
  private EventPublisher eventPublisher;
  private Watch watch;
  String cloudProviderId = "cloud-provider-id";

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
    nodeWatcher = new NodeWatcher(kubernetesClient, cloudProviderId, eventPublisher);
  }

  @Test
  @Owner(emails = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPublishNodeStartedEvent() throws Exception {
    Instant creationTime = now().minus(5, ChronoUnit.MINUTES);
    Node node = node(UID, NAME, creationTime.toString(), new HashMap<>());
    nodeWatcher.eventReceived(Action.ADDED, node);
    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(eventPublisher, times(2)).publishMessage(captor.capture());
    assertThat(captor.getAllValues().get(1)).isInstanceOfSatisfying(NodeEvent.class, nodeEvent -> {
      assertThat(nodeEvent.getNodeUid()).isEqualTo(UID);
      assertThat(nodeEvent.getType()).isEqualTo(EVENT_TYPE_START);
      assertThat(toInstant(nodeEvent.getTimestamp())).isEqualTo(creationTime);
    });
  }

  @Test
  @Owner(emails = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPublishNodeStoppedEvent() throws Exception {
    String creationTimestamp = now().minus(5, ChronoUnit.MINUTES).toString();
    Node node = node(UID, NAME, creationTimestamp, new HashMap<>());
    nodeWatcher.eventReceived(Action.DELETED, node);
    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(eventPublisher, times(2)).publishMessage(captor.capture());
    assertThat(captor.getAllValues().get(1)).isInstanceOfSatisfying(NodeEvent.class, nodeEvent -> {
      assertThat(nodeEvent.getNodeUid()).isEqualTo(UID);
      assertThat(nodeEvent.getType()).isEqualTo(EVENT_TYPE_STOP);
      // approximate check as the time is measured within the NodeWatcher code.
      assertThat(toInstant(nodeEvent.getTimestamp())).isCloseTo(now(), within(5, ChronoUnit.SECONDS));
    });
  }

  @Test
  @Owner(emails = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPublishNodeInfoOnlyOnce() throws Exception {
    Instant creationTime = now().minus(5, ChronoUnit.MINUTES);
    Map<String, String> labels = ImmutableMap.of("k1", "v1", "k2", "v2");
    Node node = node(UID, NAME, creationTime.toString(), labels);
    nodeWatcher.eventReceived(Action.ADDED, node);
    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(eventPublisher, times(2)).publishMessage(captor.capture());
    assertThat(captor.getAllValues().get(0)).isInstanceOfSatisfying(NodeInfo.class, nodeInfo -> {
      assertThat(nodeInfo.getNodeUid()).isEqualTo(UID);
      assertThat(nodeInfo.getNodeName()).isEqualTo(NAME);
      assertThat(toInstant(nodeInfo.getCreationTime())).isEqualTo(creationTime);
      assertThat(nodeInfo.getLabelsMap()).isEqualTo(labels);
    });
    Mockito.reset(eventPublisher);
    captor = ArgumentCaptor.forClass(Message.class);
    nodeWatcher.eventReceived(Action.DELETED, node);
    verify(eventPublisher).publishMessage(captor.capture());
    assertThat(captor.getAllValues()).hasSize(1).isNotInstanceOfAny(NodeInfo.class);
  }

  @Test
  @Owner(emails = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldCloseUnderlyingWatchOnClosingWatcher() throws Exception {
    nodeWatcher.onClose(null);
    verify(watch).close();
  }

  private Node node(String uid, String name, String creationTimestamp, Map<String, String> labels) {
    Node node = new Node();
    ObjectMeta nodeMeta = new ObjectMeta();
    nodeMeta.setUid(uid);
    nodeMeta.setName(name);
    nodeMeta.setCreationTimestamp(creationTimestamp);
    nodeMeta.setLabels(labels);
    node.setMetadata(nodeMeta);
    return node;
  }
}