package software.wings.delegatetasks.k8s.watch;

import com.google.inject.Inject;
import com.google.protobuf.Any;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.harness.event.PublishMessage;
import io.harness.event.payloads.Container;
import io.harness.event.payloads.Container.Resource;
import io.harness.event.payloads.Container.Resource.Quantity;
import io.harness.event.payloads.Container.Resource.Quantity.Builder;
import io.harness.event.payloads.Owner;
import io.harness.event.payloads.PodEvent;
import io.harness.event.payloads.PodEvent.EventType;
import io.harness.event.payloads.PodInfo;
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

  //@Inject private EventPublisher eventPublisher;

  @Inject
  public PodWatcher(KubernetesClient client, String namespace) {
    watch = client.pods().inNamespace(namespace).watch(this);
    podMap = new LRUMap<String, Boolean>(POD_MAP_MAX_SIZE);
  }

  @Override
  public void eventReceived(Action action, Pod pod) {
    String uid = pod.getMetadata().getUid();
    logger.debug("Pod Watcher received an event for pod with uid {}.", uid);

    PodCondition podScheduledCondition = getPodScheduledCondition(pod);

    // check if the deletion timestamp is specified
    boolean isPodDeleted = false;
    String deletionTimestamp = pod.getMetadata().getDeletionTimestamp();
    Long deletionGracePeriodSeconds = pod.getMetadata().getDeletionGracePeriodSeconds();
    if (deletionTimestamp != null && deletionGracePeriodSeconds == 0L) {
      isPodDeleted = true;
    }

    String creationTimestamp = pod.getMetadata().getCreationTimestamp();

    if (podScheduledCondition != null && !podMap.containsKey(uid)) {
      // put the pod in the map and publish the spec
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
      PublishMessage publishMessage = PublishMessage.newBuilder().setPayload(Any.pack(podInfo)).build();
      logger.info(publishMessage.toString());
      // eventPublisher.publish(publishMessage);
    }

    if (podScheduledCondition != null) {
      String timestamp = podScheduledCondition.getLastTransitionTime();
      PodEvent podEvent =
          PodEvent.newBuilder().setUid(uid).setType(EventType.SCHEDULED).setTimestamp(timestamp).build();
      PublishMessage publishMessage = PublishMessage.newBuilder().setPayload(Any.pack(podEvent)).build();
      logger.info(publishMessage.toString());
      // eventPublisher.publish(publishMessage);
    }

    if (isPodDeleted) {
      String timestamp = deletionTimestamp;
      PodEvent podEvent = PodEvent.newBuilder().setUid(uid).setType(EventType.DELETED).setTimestamp(timestamp).build();
      PublishMessage publishMessage = PublishMessage.newBuilder().setPayload(Any.pack(podEvent)).build();
      logger.info(publishMessage.toString());
      // eventPublisher.publish(publishMessage);
    }

    logger.info("\n");
  }

  @Override
  public void onClose(KubernetesClientException e) {
    logger.info("Watcher onClose");
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
          if (StringUtils.isBlank(cpuLimitAmount)) {
            cpuLimitBuilder.setAmount(cpuLimitAmount);
          }
          String cpuLimitFormat = resourceLimitsMap.get("cpu").getFormat();
          if (StringUtils.isBlank(cpuLimitFormat)) {
            cpuLimitBuilder.setFormat(cpuLimitFormat);
          }
        }

        Builder memLimitBuilder = Quantity.newBuilder();
        if (resourceLimitsMap.get("memory") != null) {
          String memLimitAmount = resourceLimitsMap.get("memory").getAmount();
          if (StringUtils.isBlank(memLimitAmount)) {
            memLimitBuilder.setAmount(memLimitAmount);
          }
          String memLimitFormat = resourceLimitsMap.get("memory").getFormat();
          if (StringUtils.isBlank(memLimitFormat)) {
            memLimitBuilder.setAmount(memLimitFormat);
          }
        }
        resourceBuilder.putLimits("cpu", cpuLimitBuilder.build()).putLimits("memory", memLimitBuilder.build());
      }

      if (resourceRequestsMap != null) {
        Builder cpuRequestBuilder = Quantity.newBuilder();
        if (resourceRequestsMap.get("cpu") != null) {
          String cpuRequestAmount = resourceRequestsMap.get("cpu").getAmount();
          if (StringUtils.isBlank(cpuRequestAmount)) {
            cpuRequestBuilder.setAmount(cpuRequestAmount);
          }
          String cpuRequestFormat = resourceRequestsMap.get("cpu").getFormat();
          if (StringUtils.isBlank(cpuRequestFormat)) {
            cpuRequestBuilder.setFormat(cpuRequestAmount);
          }
        }

        Builder memRequestBuilder = Quantity.newBuilder();
        if (resourceRequestsMap.get("memory") != null) {
          String memRequestAmount = resourceRequestsMap.get("memory").getAmount();
          if (StringUtils.isBlank(memRequestAmount)) {
            memRequestBuilder.setAmount(memRequestAmount);
          }
          String memRequestFormat = resourceRequestsMap.get("memory").getFormat();
          if (StringUtils.isBlank(memRequestFormat)) {
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
