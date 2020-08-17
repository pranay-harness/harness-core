package io.harness.perpetualtask.k8s.watch;

import static io.harness.ccm.health.HealthStatusService.CLUSTER_ID_IDENTIFIER;
import static io.harness.perpetualtask.k8s.watch.NodeEvent.EventType.EVENT_TYPE_START;
import static io.harness.perpetualtask.k8s.watch.NodeEvent.EventType.EVENT_TYPE_STOP;
import static java.util.Optional.ofNullable;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.Timestamp;

import io.harness.event.client.EventPublisher;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.kubernetes.client.informer.EventType;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.util.CallGeneratorParams;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
public class NodeWatcher implements ResourceEventHandler<V1Node> {
  private final EventPublisher eventPublisher;
  private final Set<String> publishedNodes;

  private final String clusterId;
  private final NodeInfo nodeInfoPrototype;
  private final NodeEvent nodeEventPrototype;

  private static final String NODE_EVENT_MSG = "Node: {}, action: {}";
  private static final String ERROR_PUBLISH_MSG = "Error publishing V1Node.{} event.";

  @Inject
  public NodeWatcher(@Assisted ApiClient apiClient, @Assisted ClusterDetails params,
      @Assisted SharedInformerFactory sharedInformerFactory, EventPublisher eventPublisher) {
    logger.info(
        "Creating new NodeWatcher for cluster with id: {} name: {} ", params.getClusterId(), params.getClusterName());

    this.clusterId = params.getClusterId();
    this.eventPublisher = eventPublisher;
    this.publishedNodes = new ConcurrentSkipListSet<>();
    this.nodeInfoPrototype = NodeInfo.newBuilder()
                                 .setCloudProviderId(params.getCloudProviderId())
                                 .setClusterId(clusterId)
                                 .setClusterName(params.getClusterName())
                                 .setKubeSystemUid(params.getKubeSystemUid())
                                 .build();
    this.nodeEventPrototype = NodeEvent.newBuilder()
                                  .setCloudProviderId(params.getCloudProviderId())
                                  .setClusterId(clusterId)
                                  .setKubeSystemUid(params.getKubeSystemUid())
                                  .build();

    CoreV1Api coreV1Api = new CoreV1Api(apiClient);

    sharedInformerFactory
        .sharedIndexInformerFor(
            (CallGeneratorParams callGeneratorParams)
                -> {
              try {
                return coreV1Api.listNodeCall(null, null, null, null, null, null, callGeneratorParams.resourceVersion,
                    callGeneratorParams.timeoutSeconds, callGeneratorParams.watch, null);
              } catch (ApiException e) {
                logger.error("Unknown exception occurred", e);
                throw e;
              }
            },
            V1Node.class, V1NodeList.class)
        .addEventHandler(this);
  }

  @Override
  public void onUpdate(V1Node oldNode, V1Node newNode) {
    try {
      logger.debug(NODE_EVENT_MSG, newNode.getMetadata().getUid(), EventType.MODIFIED);

      publishNodeInfo(newNode);
    } catch (Exception ex) {
      logger.error(ERROR_PUBLISH_MSG, EventType.MODIFIED, ex);
    }
  }

  @Override
  public void onAdd(V1Node node) {
    try {
      logger.debug(NODE_EVENT_MSG, node.getMetadata().getUid(), EventType.ADDED);

      publishNodeInfo(node);
      publishNodeStartedEvent(node);
    } catch (Exception ex) {
      logger.error(ERROR_PUBLISH_MSG, EventType.ADDED, ex);
    }
  }

  @Override
  public void onDelete(V1Node node, boolean deletedFinalStateUnknown) {
    try {
      logger.debug(NODE_EVENT_MSG, node.getMetadata().getUid(), EventType.DELETED);

      publishNodeInfo(node);
      publishNodeStoppedEvent(node);
    } catch (Exception ex) {
      logger.error(ERROR_PUBLISH_MSG, EventType.DELETED.name(), ex);
    }
  }

  public void publishNodeStartedEvent(V1Node node) {
    final Timestamp timestamp = HTimestamps.fromMillis(node.getMetadata().getCreationTimestamp().getMillis());

    NodeEvent nodeStartedEvent = NodeEvent.newBuilder(nodeEventPrototype)
                                     .setNodeUid(node.getMetadata().getUid())
                                     .setNodeName(node.getMetadata().getName())
                                     .setType(EVENT_TYPE_START)
                                     .setTimestamp(timestamp)
                                     .build();

    logger.debug("Publishing event: {}", nodeStartedEvent);
    eventPublisher.publishMessage(nodeStartedEvent, timestamp, ImmutableMap.of(CLUSTER_ID_IDENTIFIER, clusterId));
  }

  public void publishNodeStoppedEvent(V1Node node) {
    final Timestamp timestamp = HTimestamps.fromMillis(
        ofNullable(node.getMetadata().getDeletionTimestamp()).orElse(DateTime.now()).getMillis());

    NodeEvent nodeStoppedEvent = NodeEvent.newBuilder(nodeEventPrototype)
                                     .setNodeUid(node.getMetadata().getUid())
                                     .setNodeName(node.getMetadata().getName())
                                     .setType(EVENT_TYPE_STOP)
                                     .setTimestamp(timestamp)
                                     .build();

    logger.debug("Publishing event: {}", nodeStoppedEvent);
    eventPublisher.publishMessage(nodeStoppedEvent, timestamp, ImmutableMap.of(CLUSTER_ID_IDENTIFIER, clusterId));
    publishedNodes.remove(node.getMetadata().getUid());
  }

  public void publishNodeInfo(V1Node node) {
    if (!publishedNodes.contains(node.getMetadata().getUid())) {
      final Timestamp timestamp = HTimestamps.fromMillis(node.getMetadata().getCreationTimestamp().getMillis());

      NodeInfo nodeInfo =
          NodeInfo.newBuilder(nodeInfoPrototype)
              .setNodeUid(node.getMetadata().getUid())
              .setNodeName(node.getMetadata().getName())
              .setCreationTime(timestamp)
              .setProviderId(ofNullable(node.getSpec().getProviderID()).orElse(""))
              .putAllLabels(node.getMetadata().getLabels())
              .putAllAllocatableResource(K8sResourceUtils.getResourceMap(node.getStatus().getAllocatable()))
              .build();
      eventPublisher.publishMessage(nodeInfo, timestamp, ImmutableMap.of(CLUSTER_ID_IDENTIFIER, clusterId));
      publishedNodes.add(node.getMetadata().getUid());
    } else {
      logger.debug("NodeInfo for uid:{} already published", node.getMetadata().getUid());
    }
  }
}
