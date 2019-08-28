package io.harness.perpetualtask.k8s.watch;

import static io.harness.event.payloads.PodEvent.EventType.EVENT_TYPE_DELETED;
import static io.harness.event.payloads.PodEvent.EventType.EVENT_TYPE_SCHEDULED;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
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
import io.harness.event.payloads.Container;
import io.harness.event.payloads.Container.Resource;
import io.harness.event.payloads.Container.Resource.Quantity;
import io.harness.event.payloads.Container.Resource.Quantity.Builder;
import io.harness.event.payloads.Owner;
import io.harness.event.payloads.PodEvent;
import io.harness.event.payloads.PodInfo;
import io.harness.grpc.utils.HTimestamps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class PodWatcher implements Watcher<Pod> {
  private Watch watch;
  private static final int POD_MAP_MAX_SIZE = 5000;
  private LRUMap podMap;
  TypeRegistry typeRegistry;

  private EventPublisher eventPublisher;

  @Inject
  public PodWatcher(KubernetesClient client, EventPublisher eventPublisher) {
    watch = client.pods().inAnyNamespace().watch(this);
    podMap = new LRUMap<String, Boolean>(POD_MAP_MAX_SIZE);
    typeRegistry = TypeRegistry.newBuilder().add(PodInfo.getDescriptor()).add(PodEvent.getDescriptor()).build();
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void eventReceived(Action action, Pod pod) {
    String uid = pod.getMetadata().getUid();
    logger.debug("Pod Watcher received an event for pod with uid {}.", uid);
    PodCondition podScheduledCondition = getPodScheduledCondition(pod); // check if the deletion timestamp is specified
    boolean isPodDeleted = false;
    String deletionTimestamp = pod.getMetadata().getDeletionTimestamp();
    Long deletionGracePeriodSeconds = pod.getMetadata().getDeletionGracePeriodSeconds();
    if (deletionTimestamp != null && deletionGracePeriodSeconds == 0L) {
      isPodDeleted = true;
    }

    if (podScheduledCondition != null && !podMap.containsKey(uid)) {
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
      try {
        logger.debug(JsonFormat.printer().usingTypeRegistry(typeRegistry).print(podInfo));
      } catch (InvalidProtocolBufferException e) {
        logger.error(e.getMessage());
      }
      eventPublisher.publishMessage(podInfo);
    }

    if (podScheduledCondition != null) {
      Timestamp timestamp = HTimestamps.parse(podScheduledCondition.getLastTransitionTime());
      PodEvent podEvent =
          PodEvent.newBuilder().setUid(uid).setType(EVENT_TYPE_SCHEDULED).setTimestamp(timestamp).build();
      try {
        logger.debug(JsonFormat.printer().usingTypeRegistry(typeRegistry).print(podEvent));
      } catch (InvalidProtocolBufferException e) {
        logger.error(e.getMessage());
      }
      eventPublisher.publishMessage(podEvent);
    }

    if (isPodDeleted) {
      Timestamp timestamp = HTimestamps.parse(deletionTimestamp);
      PodEvent podEvent = PodEvent.newBuilder().setUid(uid).setType(EVENT_TYPE_DELETED).setTimestamp(timestamp).build();
      try {
        logger.debug(JsonFormat.printer().usingTypeRegistry(typeRegistry).print(podEvent));
      } catch (InvalidProtocolBufferException e) {
        logger.error(e.getMessage());
      }
      eventPublisher.publishMessage(podEvent);
    }

    logger.info("\n");
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
            memLimitBuilder.setAmount(memLimitFormat);
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
            cpuRequestBuilder.setFormat(cpuRequestAmount);
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

  private List<Owner> getAllOwners(List<io.fabric8.kubernetes.api.model.OwnerReference> k8sOwnerReferenceList) {
    List<Owner> ownerList = new ArrayList<>();
    for (io.fabric8.kubernetes.api.model.OwnerReference ownerReference : k8sOwnerReferenceList) {
      ownerList.add(Owner.newBuilder()
                        .setUid(ownerReference.getUid())
                        .setName(ownerReference.getName())
                        .setKind(ownerReference.getKind())
                        .build());
    }

    return ownerList;
  }

  /**
   * Get the pod condition with type PodScheduled=true.
   * A pod occupies resource when type=PodScheduled and status=True.
   */
  private PodCondition getPodScheduledCondition(Pod pod) {
    PodCondition podScheduledCondition = null;
    List<PodCondition> conditions = pod.getStatus().getConditions();
    for (PodCondition c : conditions) {
      if ("PodScheduled".equals(c.getType()) && "True".equals(c.getStatus())) {
        podScheduledCondition = c;
      }
    }
    return podScheduledCondition;
  }
}
