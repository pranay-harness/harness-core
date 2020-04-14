package io.harness.perpetualtask.k8s.watch;

import static io.harness.ccm.health.HealthStatusService.CLUSTER_ID_IDENTIFIER;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.Timestamp;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.event.client.EventPublisher;
import io.harness.event.payloads.CeExceptionMessage;
import io.harness.event.payloads.NodeMetric;
import io.harness.event.payloads.PodMetric;
import io.harness.event.payloads.Usage;
import io.harness.grpc.utils.AnyUtils;
import io.harness.grpc.utils.HDurations;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskParams;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.metrics.client.K8sMetricsClient;
import io.harness.perpetualtask.k8s.metrics.collector.K8sMetricCollector;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.k8s.client.KubernetesClientFactory;
import software.wings.delegatetasks.k8s.exception.K8sClusterException;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class K8SWatchTaskExecutor implements PerpetualTaskExecutor {
  private static final String MESSAGE_PROCESSOR_TYPE = "EXCEPTION";
  private Map<String, String> taskWatchIdMap = new ConcurrentHashMap<>();
  private Map<String, K8sMetricCollector> metricCollectors = new ConcurrentHashMap<>();
  private Map<String, Instant> clusterSyncLastPublished = new ConcurrentHashMap<>();

  private final EventPublisher eventPublisher;
  private final KubernetesClientFactory kubernetesClientFactory;
  private final K8sWatchServiceDelegate k8sWatchServiceDelegate;

  @Inject
  public K8SWatchTaskExecutor(EventPublisher eventPublisher, KubernetesClientFactory kubernetesClientFactory,
      K8sWatchServiceDelegate k8sWatchServiceDelegate) {
    this.eventPublisher = eventPublisher;
    this.kubernetesClientFactory = kubernetesClientFactory;
    this.k8sWatchServiceDelegate = k8sWatchServiceDelegate;
  }

  @Override
  public PerpetualTaskResponse runOnce(PerpetualTaskId taskId, PerpetualTaskParams params, Instant heartbeatTime) {
    K8sWatchTaskParams watchTaskParams = AnyUtils.unpack(params.getCustomizedParams(), K8sWatchTaskParams.class);
    try {
      Instant now = Instant.now();
      String watchId = k8sWatchServiceDelegate.create(watchTaskParams);
      logger.info("Created a watch with id {}.", watchId);
      K8sClusterConfig k8sClusterConfig =
          (K8sClusterConfig) KryoUtils.asObject(watchTaskParams.getK8SClusterConfig().toByteArray());
      K8sMetricsClient k8sMetricsClient =
          kubernetesClientFactory.newAdaptedClient(k8sClusterConfig, K8sMetricsClient.class);
      taskWatchIdMap.putIfAbsent(taskId.getId(), watchId);
      clusterSyncLastPublished.putIfAbsent(taskId.getId(), heartbeatTime);
      if (clusterSyncLastPublished.get(taskId.getId()).plus(Duration.ofHours(1)).isBefore(now)) {
        publishClusterSyncEvent(k8sMetricsClient, eventPublisher, watchTaskParams, now);
        clusterSyncLastPublished.put(taskId.getId(), now);
      }
      metricCollectors
          .computeIfAbsent(taskId.getId(),
              key -> {
                ClusterDetails clusterDetails =
                    ClusterDetails.builder()
                        .clusterId(watchTaskParams.getClusterId())
                        .cloudProviderId(watchTaskParams.getCloudProviderId())
                        .clusterName(watchTaskParams.getClusterName())
                        .kubeSystemUid(K8sWatchServiceDelegate.getKubeSystemUid(k8sMetricsClient))
                        .build();
                return new K8sMetricCollector(eventPublisher, k8sMetricsClient, clusterDetails, heartbeatTime);
              })
          .collectAndPublishMetrics(now);

      // to be removed after batch processing changes
      publishNodeMetrics(k8sMetricsClient, eventPublisher, watchTaskParams, heartbeatTime);
      publishPodMetrics(k8sMetricsClient, eventPublisher, watchTaskParams, heartbeatTime);
    } catch (K8sClusterException ke) {
      try {
        eventPublisher.publishMessage(CeExceptionMessage.newBuilder()
                                          .setClusterId(watchTaskParams.getClusterId())
                                          .setMessage((String) ke.getParams().get("reason"))
                                          .build(),
            HTimestamps.fromInstant(Instant.now()), Collections.emptyMap(), MESSAGE_PROCESSOR_TYPE);
      } catch (Exception ex) {
        logger.error("Failed to publish failure from {} to the Event Server.", taskId);
      }
    } catch (Exception e) {
      logger.error(String.format("Encountered exceptions when executing perpetual task with id=%s", taskId), e);
    }
    return PerpetualTaskResponse.builder()
        .responseCode(200)
        .perpetualTaskState(PerpetualTaskState.TASK_RUN_SUCCEEDED)
        .responseMessage(PerpetualTaskState.TASK_RUN_SUCCEEDED.name())
        .build();
  }

  @VisibleForTesting
  static void publishClusterSyncEvent(
      KubernetesClient client, EventPublisher eventPublisher, K8sWatchTaskParams watchTaskParams, Instant pollTime) {
    List<String> nodeUidList = client.nodes()
                                   .list()
                                   .getItems()
                                   .stream()
                                   .map(Node::getMetadata)
                                   .map(ObjectMeta::getUid)
                                   .collect(Collectors.toList());
    List<String> podUidList = client.pods()
                                  .inAnyNamespace()
                                  .list()
                                  .getItems()
                                  .stream()
                                  .filter(pod -> "Running".equals(pod.getStatus().getPhase()))
                                  .map(Pod::getMetadata)
                                  .map(ObjectMeta::getUid)
                                  .collect(Collectors.toList());
    Timestamp timestamp = HTimestamps.fromInstant(pollTime);
    K8SClusterSyncEvent k8SClusterSyncEvent = K8SClusterSyncEvent.newBuilder()
                                                  .setClusterId(watchTaskParams.getClusterId())
                                                  .setCloudProviderId(watchTaskParams.getCloudProviderId())
                                                  .setClusterName(watchTaskParams.getClusterName())
                                                  .setKubeSystemUid(K8sWatchServiceDelegate.getKubeSystemUid(client))
                                                  .addAllActiveNodeUids(nodeUidList)
                                                  .addAllActivePodUids(podUidList)
                                                  .setLastProcessedTimestamp(timestamp)
                                                  .build();
    eventPublisher.publishMessage(
        k8SClusterSyncEvent, timestamp, ImmutableMap.of(CLUSTER_ID_IDENTIFIER, watchTaskParams.getClusterId()));
  }

  @VisibleForTesting
  static void publishNodeMetrics(K8sMetricsClient k8sMetricsClient, EventPublisher eventPublisher,
      K8sWatchTaskParams watchTaskParams, Instant heartbeatTime) {
    String kubeSystemUid = K8sWatchServiceDelegate.getKubeSystemUid(k8sMetricsClient);
    k8sMetricsClient.nodeMetrics()
        .list()
        .getItems()
        .stream()
        .map(nodeMetric -> {
          return NodeMetric.newBuilder()
              .setCloudProviderId(watchTaskParams.getCloudProviderId())
              .setClusterId(watchTaskParams.getClusterId())
              .setKubeSystemUid(kubeSystemUid)
              .setName(nodeMetric.getMetadata().getName())
              .setTimestamp(HTimestamps.parse(nodeMetric.getTimestamp()))
              .setWindow(HDurations.parse(nodeMetric.getWindow()))
              .setUsage(Usage.newBuilder()
                            .setCpuNano(K8sResourceStandardizer.getCpuNano(nodeMetric.getUsage().getCpu()))
                            .setMemoryByte(K8sResourceStandardizer.getMemoryByte(nodeMetric.getUsage().getMemory())))
              .build();
        })
        .forEach(nodeMetric
            -> eventPublisher.publishMessage(nodeMetric, HTimestamps.fromInstant(heartbeatTime),
                ImmutableMap.of(CLUSTER_ID_IDENTIFIER, watchTaskParams.getClusterId())));
  }

  @VisibleForTesting
  static void publishPodMetrics(K8sMetricsClient k8sMetricsClient, EventPublisher eventPublisher,
      K8sWatchTaskParams watchTaskParams, Instant heartbeatTime) {
    String kubeSystemUid = K8sWatchServiceDelegate.getKubeSystemUid(k8sMetricsClient);
    k8sMetricsClient.podMetrics()
        .inAnyNamespace()
        .list()
        .getItems()
        .stream()
        .filter(podMetric -> isNotEmpty(podMetric.getContainers()))
        .map(podMetric
            -> PodMetric.newBuilder()
                   .setCloudProviderId(watchTaskParams.getCloudProviderId())
                   .setClusterId(watchTaskParams.getClusterId())
                   .setKubeSystemUid(kubeSystemUid)
                   .setNamespace(podMetric.getMetadata().getNamespace())
                   .setName(podMetric.getMetadata().getName())
                   .setTimestamp(HTimestamps.parse(podMetric.getTimestamp()))
                   .setWindow(HDurations.parse(podMetric.getWindow()))
                   .addAllContainers(podMetric.getContainers()
                                         .stream()
                                         .map(container
                                             -> PodMetric.Container.newBuilder()
                                                    .setName(container.getName())
                                                    .setUsage(Usage.newBuilder()
                                                                  .setCpuNano(K8sResourceStandardizer.getCpuNano(
                                                                      container.getUsage().getCpu()))
                                                                  .setMemoryByte(K8sResourceStandardizer.getMemoryByte(
                                                                      container.getUsage().getMemory()))
                                                                  .build())
                                                    .build())
                                         .collect(toList()))
                   .build())
        .forEach(podMetric
            -> eventPublisher.publishMessage(podMetric, HTimestamps.fromInstant(heartbeatTime),
                ImmutableMap.of(CLUSTER_ID_IDENTIFIER, watchTaskParams.getClusterId())));
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskParams params) {
    if (taskWatchIdMap.get(taskId.getId()) == null) {
      return false;
    }
    String watchId = taskWatchIdMap.get(taskId.getId());
    logger.info("Stopping the watch with id {}", watchId);
    k8sWatchServiceDelegate.delete(watchId);
    taskWatchIdMap.remove(taskId.getId());

    metricCollectors.computeIfPresent(taskId.getId(), (id, metricCollector) -> {
      metricCollector.publishPending(Instant.now());
      return null;
    });
    return true;
  }
}
