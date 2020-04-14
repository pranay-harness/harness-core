package io.harness.perpetualtask.k8s.watch;

import static com.google.common.base.MoreObjects.firstNonNull;
import static io.harness.ccm.health.HealthStatusService.CLUSTER_ID_IDENTIFIER;
import static io.harness.perpetualtask.k8s.watch.PodEvent.EventType.EVENT_TYPE_SCHEDULED;
import static io.harness.perpetualtask.k8s.watch.PodEvent.EventType.EVENT_TYPE_TERMINATED;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.TypeRegistry;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.harness.event.client.EventPublisher;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class PodWatcher implements Watcher<Pod> {
  private static final TypeRegistry TYPE_REGISTRY =
      TypeRegistry.newBuilder().add(PodInfo.getDescriptor()).add(PodEvent.getDescriptor()).build();

  private final Watch watch;
  private final String clusterId;
  private final EventPublisher eventPublisher;
  private final Set<String> publishedPods;
  private final KubernetesClient client;

  private final PodInfo podInfoPrototype;
  private final PodEvent podEventPrototype;

  @Inject
  public PodWatcher(@Assisted KubernetesClient client, @Assisted ClusterDetails params, EventPublisher eventPublisher) {
    logger.info(
        "Creating new PodWatcher for cluster with id: {} name: {} ", params.getClusterId(), params.getClusterName());
    this.client = client;
    this.watch = client.pods().inAnyNamespace().watch(this);
    this.clusterId = params.getClusterId();
    this.publishedPods = new HashSet<>();
    this.eventPublisher = eventPublisher;
    podInfoPrototype = PodInfo.newBuilder()
                           .setCloudProviderId(params.getCloudProviderId())
                           .setClusterId(clusterId)
                           .setClusterName(params.getClusterName())
                           .setKubeSystemUid(params.getKubeSystemUid())
                           .build();
    podEventPrototype = PodEvent.newBuilder()
                            .setCloudProviderId(params.getCloudProviderId())
                            .setClusterId(clusterId)
                            .setKubeSystemUid(params.getKubeSystemUid())
                            .build();
  }

  @Override
  public void eventReceived(Action action, Pod pod) {
    String uid = pod.getMetadata().getUid();
    logger.debug("Pod Watcher received an event for pod with uid={}, action={}", uid, action);
    PodCondition podScheduledCondition = getPodScheduledCondition(pod);
    if (podScheduledCondition != null && !publishedPods.contains(uid)) {
      Timestamp creationTimestamp = HTimestamps.parse(pod.getMetadata().getCreationTimestamp());

      PodInfo podInfo = PodInfo.newBuilder(podInfoPrototype)
                            .setPodUid(uid)
                            .setPodName(pod.getMetadata().getName())
                            .setNamespace(pod.getMetadata().getNamespace())
                            .setNodeName(pod.getSpec().getNodeName())
                            .setTotalResource(K8sResourceUtils.getTotalResourceRequest(pod.getSpec().getContainers()))
                            .setCreationTimestamp(creationTimestamp)
                            .addAllContainers(getAllContainers(pod.getSpec().getContainers()))
                            .putAllLabels(firstNonNull(pod.getMetadata().getLabels(), Collections.emptyMap()))
                            .setTopLevelOwner(K8sWorkloadUtils.getTopLevelOwner(client, pod))
                            .build();
      logMessage(podInfo);

      eventPublisher.publishMessage(podInfo, creationTimestamp, ImmutableMap.of(CLUSTER_ID_IDENTIFIER, clusterId));
      final Timestamp timestamp = HTimestamps.parse(podScheduledCondition.getLastTransitionTime());
      PodEvent podEvent = PodEvent.newBuilder(podEventPrototype)
                              .setPodUid(uid)
                              .setType(EVENT_TYPE_SCHEDULED)
                              .setTimestamp(timestamp)
                              .build();
      logMessage(podEvent);
      eventPublisher.publishMessage(podEvent, timestamp, ImmutableMap.of(CLUSTER_ID_IDENTIFIER, clusterId));
      publishedPods.add(uid);
    }

    if (isPodDeleted(pod)) {
      String deletionTimestamp = pod.getMetadata().getDeletionTimestamp();
      Timestamp timestamp = HTimestamps.parse(deletionTimestamp);
      PodEvent podEvent = PodEvent.newBuilder(podEventPrototype)
                              .setPodUid(uid)
                              .setType(EVENT_TYPE_TERMINATED)
                              .setTimestamp(timestamp)
                              .build();
      logMessage(podEvent);
      eventPublisher.publishMessage(podEvent, timestamp, ImmutableMap.of(CLUSTER_ID_IDENTIFIER, clusterId));
      publishedPods.remove(uid);

    } else if (isPodInTerminalPhase(pod)) {
      Timestamp timestamp = HTimestamps.fromInstant(Instant.now());
      PodEvent podEvent = PodEvent.newBuilder(podEventPrototype)
                              .setPodUid(uid)
                              .setType(EVENT_TYPE_TERMINATED)
                              .setTimestamp(timestamp)
                              .build();
      logMessage(podEvent);
      eventPublisher.publishMessage(podEvent, timestamp, ImmutableMap.of(CLUSTER_ID_IDENTIFIER, clusterId));
      publishedPods.remove(uid);
    }
  }

  private boolean isPodDeleted(Pod pod) {
    return pod.getMetadata().getDeletionTimestamp() != null && pod.getMetadata().getDeletionGracePeriodSeconds() == 0L;
  }

  private boolean isPodInTerminalPhase(Pod pod) {
    String phase = pod.getStatus().getPhase();
    return "Succeeded".equals(phase) || "Failed".equals(phase);
  }

  @Override
  public void onClose(KubernetesClientException e) {
    logger.info("Watcher onClose");
    if (watch != null) {
      watch.close();
    }
    if (e != null) {
      logger.error(e.getMessage(), e);
    }
  }

  private List<Container> getAllContainers(List<io.fabric8.kubernetes.api.model.Container> k8sContainerList) {
    List<Container> containerList = new ArrayList<>();
    for (io.fabric8.kubernetes.api.model.Container k8sContainer : k8sContainerList) {
      Container container = Container.newBuilder()
                                .setName(k8sContainer.getName())
                                .setImage(k8sContainer.getImage())
                                .setResource(K8sResourceUtils.getResource(k8sContainer))
                                .build();
      containerList.add(container);
    }
    return containerList;
  }

  /**
   * Get the pod condition with type PodScheduled=true.
   * A pod occupies resource when type=PodScheduled and status=True.
   */
  private PodCondition getPodScheduledCondition(Pod pod) {
    return pod.getStatus()
        .getConditions()
        .stream()
        .filter(c -> "PodScheduled".equals(c.getType()) && "True".equals(c.getStatus()))
        .findFirst()
        .orElse(null);
  }

  private static void logMessage(Message message) {
    if (logger.isDebugEnabled()) {
      try {
        logger.debug(JsonFormat.printer().usingTypeRegistry(TYPE_REGISTRY).print(message));
      } catch (InvalidProtocolBufferException e) {
        logger.error(e.getMessage());
      }
    }
  }
}
