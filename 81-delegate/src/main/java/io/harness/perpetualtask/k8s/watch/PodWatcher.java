package io.harness.perpetualtask.k8s.watch;

import static io.harness.event.payloads.PodEvent.EventType.EVENT_TYPE_DELETED;
import static io.harness.event.payloads.PodEvent.EventType.EVENT_TYPE_SCHEDULED;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.TypeRegistry;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.harness.event.client.EventPublisher;
import io.harness.event.payloads.Container;
import io.harness.event.payloads.Container.Resource;
import io.harness.event.payloads.Container.Resource.Quantity;
import io.harness.event.payloads.Container.Resource.Quantity.Builder;
import io.harness.event.payloads.Owner;
import io.harness.event.payloads.PodEvent;
import io.harness.event.payloads.PodInfo;
import io.harness.grpc.utils.HTimestamps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class PodWatcher implements Watcher<Pod> {
  private static final TypeRegistry TYPE_REGISTRY =
      TypeRegistry.newBuilder().add(PodInfo.getDescriptor()).add(PodEvent.getDescriptor()).build();

  private final Watch watch;
  private final EventPublisher eventPublisher;
  private final Set<String> publishedPods;

  @Inject
  public PodWatcher(@Assisted KubernetesClient client, EventPublisher eventPublisher) {
    watch = client.pods().inAnyNamespace().watch(this);
    publishedPods = new HashSet<>();
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void eventReceived(Action action, Pod pod) {
    String uid = pod.getMetadata().getUid();
    logger.debug("Pod Watcher received an event for pod with uid={}, action={}", uid, action);
    PodCondition podScheduledCondition = getPodScheduledCondition(pod);
    if (podScheduledCondition != null && !publishedPods.contains(uid)) {
      // put the pod in the map and publish the spec
      Timestamp creationTimestamp = HTimestamps.parse(pod.getMetadata().getCreationTimestamp());
      PodInfo podInfo = PodInfo.newBuilder()
                            .setUid(uid)
                            .setName(pod.getMetadata().getName())
                            .setNamespace(pod.getMetadata().getNamespace())
                            .setNodeName(pod.getSpec().getNodeName())
                            .setCreationTimestamp(creationTimestamp)
                            .addAllContainers(getAllContainers(pod.getSpec().getContainers()))
                            .putAllLabels(pod.getMetadata().getLabels())
                            .addAllOwner(getAllOwners(pod.getMetadata().getOwnerReferences()))
                            .build();
      logMessage(podInfo);
      eventPublisher.publishMessage(podInfo);
      PodEvent podEvent = PodEvent.newBuilder()
                              .setUid(uid)
                              .setType(EVENT_TYPE_SCHEDULED)
                              .setTimestamp(HTimestamps.parse(podScheduledCondition.getLastTransitionTime()))
                              .build();
      logMessage(podEvent);
      eventPublisher.publishMessage(podEvent);
      publishedPods.add(uid);
    }

    if (isPodDeleted(pod)) {
      String deletionTimestamp = pod.getMetadata().getDeletionTimestamp();
      Timestamp timestamp = HTimestamps.parse(deletionTimestamp);
      PodEvent podEvent = PodEvent.newBuilder().setUid(uid).setType(EVENT_TYPE_DELETED).setTimestamp(timestamp).build();
      logMessage(podEvent);
      eventPublisher.publishMessage(podEvent);
      publishedPods.remove(uid);
    }
  }

  private boolean isPodDeleted(Pod pod) {
    return pod.getMetadata().getDeletionTimestamp() != null && pod.getMetadata().getDeletionGracePeriodSeconds() == 0L;
  }

  @Override
  public void onClose(KubernetesClientException e) {
    logger.info("Watcher onClose");
    watch.close();
    if (e != null) {
      logger.error(e.getMessage(), e);
    }
  }

  private List<Container> getAllContainers(List<io.fabric8.kubernetes.api.model.Container> k8sContainerList) {
    List<Container> containerList = new ArrayList<>();
    for (io.fabric8.kubernetes.api.model.Container k8sContainer : k8sContainerList) {
      // get the resource for each container
      Map<String, io.fabric8.kubernetes.api.model.Quantity> resourceLimitsMap = k8sContainer.getResources().getLimits();
      Map<String, io.fabric8.kubernetes.api.model.Quantity> resourceRequestsMap =
          k8sContainer.getResources().getRequests();

      io.harness.event.payloads.Container.Resource.Builder resourceBuilder = Resource.newBuilder();
      if (resourceLimitsMap != null) {
        Builder cpuLimitBuilder = Quantity.newBuilder();
        if (resourceLimitsMap.get("cpu") != null) {
          String cpuLimitAmount = resourceLimitsMap.get("cpu").getAmount();
          if (!StringUtils.isBlank(cpuLimitAmount)) {
            cpuLimitBuilder.setAmount(cpuLimitAmount);
          }
          String cpuLimitFormat = resourceLimitsMap.get("cpu").getFormat();
          if (!StringUtils.isBlank(cpuLimitFormat)) {
            cpuLimitBuilder.setFormat(cpuLimitFormat);
          }
        }

        Builder memLimitBuilder = Quantity.newBuilder();
        if (resourceLimitsMap.get("memory") != null) {
          String memLimitAmount = resourceLimitsMap.get("memory").getAmount();
          if (!StringUtils.isBlank(memLimitAmount)) {
            memLimitBuilder.setAmount(memLimitAmount);
          }
          String memLimitFormat = resourceLimitsMap.get("memory").getFormat();
          if (!StringUtils.isBlank(memLimitFormat)) {
            memLimitBuilder.setFormat(memLimitFormat);
          }
        }
        resourceBuilder.putLimits("cpu", cpuLimitBuilder.build()).putLimits("memory", memLimitBuilder.build());
      }

      if (resourceRequestsMap != null) {
        Builder cpuRequestBuilder = Quantity.newBuilder();
        if (resourceRequestsMap.get("cpu") != null) {
          String cpuRequestAmount = resourceRequestsMap.get("cpu").getAmount();
          if (!StringUtils.isBlank(cpuRequestAmount)) {
            cpuRequestBuilder.setAmount(cpuRequestAmount);
          }
          String cpuRequestFormat = resourceRequestsMap.get("cpu").getFormat();
          if (!StringUtils.isBlank(cpuRequestFormat)) {
            cpuRequestBuilder.setFormat(cpuRequestFormat);
          }
        }

        Builder memRequestBuilder = Quantity.newBuilder();
        if (resourceRequestsMap.get("memory") != null) {
          String memRequestAmount = resourceRequestsMap.get("memory").getAmount();
          if (!StringUtils.isBlank(memRequestAmount)) {
            memRequestBuilder.setAmount(memRequestAmount);
          }
          String memRequestFormat = resourceRequestsMap.get("memory").getFormat();
          if (!StringUtils.isBlank(memRequestFormat)) {
            memRequestBuilder.setFormat(memRequestFormat);
          }
        }
        resourceBuilder.putRequests("cpu", cpuRequestBuilder.build()).putRequests("memory", memRequestBuilder.build());
      }
      Resource resource = resourceBuilder.build();
      Container container = Container.newBuilder()
                                .setName(k8sContainer.getName())
                                .setImage(k8sContainer.getImage())
                                .setResource(resource)
                                .build();
      containerList.add(container);
    }
    return containerList;
  }

  private Set<Owner> getAllOwners(List<OwnerReference> k8sOwnerReferences) {
    return k8sOwnerReferences.stream()
        .map(ownerReference
            -> Owner.newBuilder()
                   .setUid(ownerReference.getUid())
                   .setName(ownerReference.getName())
                   .setKind(ownerReference.getKind())
                   .build())
        .collect(Collectors.toSet());
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
