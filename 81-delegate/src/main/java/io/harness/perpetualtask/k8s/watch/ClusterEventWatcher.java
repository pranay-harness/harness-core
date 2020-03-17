package io.harness.perpetualtask.k8s.watch;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.harness.ccm.health.HealthStatusService;
import io.harness.event.client.EventPublisher;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Responsible for watching the k8s events resource, for the purpose of associating with cluster cost insights.
 */
@Slf4j
public class ClusterEventWatcher implements Watcher<Event> {
  private static final List<Predicate<Event>> whitelistedSourceReasons = Arrays.asList(
      // workload-related
      SourceReasonPredicate.builder().sourceComponent("deployment-controller").reason("ScalingReplicaSet").build(),
      SourceReasonPredicate.builder()
          .sourceComponent("statefulset-controller")
          .reason("SuccessfulCreate")
          .reason("SuccessfulDelete")
          .build(),
      SourceReasonPredicate.builder()
          .sourceComponent("daemonset-controller")
          .reason("SuccessfulCreate")
          .reason("SuccessfulDelete")
          .build(),
      SourceReasonPredicate.builder()
          .sourceComponent("cronjob-controller")
          .reason("SuccessfulCreate")
          .reason("SawCompletedJob")
          .build(),

      // autoscaling-related
      SourceReasonPredicate.builder().sourceComponent("horizontal-pod-autoscaler").reason("SuccessfulRescale").build(),
      SourceReasonPredicate.builder()
          .sourceComponent("cluster-autoscaler")
          .reason("ScaleDown")
          .reason("TriggeredScaleUp")
          .build());

  private final KubernetesClient client;
  private final EventPublisher eventPublisher;
  private final K8sClusterEvent clusterEventPrototype;

  @Value
  @Builder
  private static class SourceReasonPredicate implements Predicate<Event> {
    String sourceComponent;
    @Singular ImmutableSet<String> reasons;

    @Override
    public boolean test(Event event) {
      return event.getSource().getComponent().equals(sourceComponent) && reasons.contains(event.getReason());
    }
  }

  @Inject
  public ClusterEventWatcher(
      @Assisted KubernetesClient client, @Assisted ClusterDetails params, EventPublisher eventPublisher) {
    logger.info("Creating new ClusterEventWatcher for cluster with id: {} name: {} ", params.getClusterId(),
        params.getClusterName());
    this.client = client;
    this.client.events().inAnyNamespace().watch(this);
    this.eventPublisher = eventPublisher;
    clusterEventPrototype = K8sClusterEvent.newBuilder()
                                .setClusterId(params.getClusterId())
                                .setClusterName(params.getClusterName())
                                .setCloudProviderId(params.getCloudProviderId())
                                .setKubeSystemUid(params.getKubeSystemUid())
                                .build();
  }

  @Override
  public void eventReceived(Action action, Event event) {
    if (whitelistedSourceReasons.stream().anyMatch(p -> p.test(event))) {
      final K8sClusterEvent.Builder msgBuilder = K8sClusterEvent.newBuilder(clusterEventPrototype)
                                                     .setSourceComponent(event.getSource().getComponent())
                                                     .setReason(event.getReason())
                                                     .setMessage(event.getMessage());
      addInvolvedObjectInfo(event, msgBuilder);
      eventPublisher.publishMessage(msgBuilder.build(), HTimestamps.parse(event.getLastTimestamp()),
          ImmutableMap.of(HealthStatusService.CLUSTER_ID_IDENTIFIER, clusterEventPrototype.getClusterId()));
    }
  }

  private void addInvolvedObjectInfo(Event event, K8sClusterEvent.Builder builder) {
    builder.setInvolvedObject(K8sObjectReference.newBuilder()
                                  .setKind(event.getInvolvedObject().getKind())
                                  .setName(event.getInvolvedObject().getName())
                                  .setNamespace(defaultIfNull(event.getInvolvedObject().getNamespace(), ""))
                                  .setUid(event.getInvolvedObject().getUid())
                                  .setResourceVersion(event.getInvolvedObject().getResourceVersion())
                                  .build());
  }

  @Override
  public void onClose(KubernetesClientException cause) {
    logger.info("Closing with cause:", cause);
  }
}
