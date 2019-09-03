package io.harness.perpetualtask.k8s.watch;

import static io.harness.event.payloads.NodeEvent.EventType.EVENT_TYPE_START;
import static io.harness.event.payloads.NodeEvent.EventType.EVENT_TYPE_STOP;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.harness.event.client.EventPublisher;
import io.harness.event.payloads.NodeEvent;
import io.harness.event.payloads.NodeInfo;
import io.harness.grpc.utils.HTimestamps;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
public class NodeWatcher implements Watcher<Node> {
  private final EventPublisher eventPublisher;
  private final Watch watch;

  private final Set<String> publishedNodes;

  @Inject
  public NodeWatcher(KubernetesClient client, EventPublisher eventPublisher) {
    this.watch = client.nodes().watch(this);
    this.eventPublisher = eventPublisher;
    this.publishedNodes = new ConcurrentSkipListSet<>();
  }

  @Override
  public void eventReceived(Action action, Node node) {
    logger.trace("Node: {}, action: {}", node, action);
    publishNodeInfo(node);
    if (action == Action.ADDED) {
      publishNodeStartedEvent(node);
    } else if (action == Action.DELETED) {
      publishNodeStoppedEvent(node);
    }
  }

  private void publishNodeStartedEvent(Node node) {
    NodeEvent nodeStartedEvent = NodeEvent.newBuilder()
                                     .setUid(node.getMetadata().getUid())
                                     .setType(EVENT_TYPE_START)
                                     .setEventTime(HTimestamps.parse(node.getMetadata().getCreationTimestamp()))
                                     .build();
    logger.debug("Publishing event: {}", nodeStartedEvent);
    eventPublisher.publishMessage(nodeStartedEvent);
  }

  private void publishNodeStoppedEvent(Node node) {
    NodeEvent nodeStoppedEvent = NodeEvent.newBuilder()
                                     .setUid(node.getMetadata().getUid())
                                     .setType(EVENT_TYPE_STOP)
                                     .setEventTime(HTimestamps.fromInstant(Instant.now()))
                                     .build();
    logger.debug("Publishing event: {}", nodeStoppedEvent);
    eventPublisher.publishMessage(nodeStoppedEvent);
  }

  private void publishNodeInfo(Node node) {
    if (!publishedNodes.contains(node.getMetadata().getUid())) {
      NodeInfo nodeInfo = NodeInfo.newBuilder()
                              .setUid(node.getMetadata().getUid())
                              .setName(node.getMetadata().getName())
                              .setCreationTime(HTimestamps.parse(node.getMetadata().getCreationTimestamp()))
                              .putAllLabels(node.getMetadata().getLabels())
                              .build();
      eventPublisher.publishMessage(nodeInfo);
      publishedNodes.add(node.getMetadata().getUid());
    }
  }

  @Override
  public void onClose(KubernetesClientException cause) {
    if (cause != null) {
      logger.error("Closing node watch with error ", cause);
    }
    watch.close();
  }
}
