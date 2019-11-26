package io.harness.perpetualtask.k8s.watch;

import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.event.client.EventPublisher;
import io.harness.event.payloads.NodeMetric;
import io.harness.event.payloads.PodMetric;
import io.harness.event.payloads.Usage;
import io.harness.grpc.utils.AnyUtils;
import io.harness.grpc.utils.HDurations;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskParams;
import io.harness.perpetualtask.k8s.metrics.client.K8sMetricsClient;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.k8s.client.KubernetesClientFactory;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Slf4j
public class K8SWatchTaskExecutor implements PerpetualTaskExecutor {
  @Inject private K8sWatchServiceDelegate k8sWatchServiceDelegate;
  private Map<String, String> taskWatchIdMap = new ConcurrentHashMap<>();

  private final EventPublisher eventPublisher;
  private final KubernetesClientFactory kubernetesClientFactory;

  private K8sMetricsClient k8sMetricsClient;

  @Inject
  public K8SWatchTaskExecutor(EventPublisher eventPublisher, KubernetesClientFactory kubernetesClientFactory) {
    this.eventPublisher = eventPublisher;
    this.kubernetesClientFactory = kubernetesClientFactory;
  }

  @Override
  public boolean runOnce(PerpetualTaskId taskId, PerpetualTaskParams params, Instant heartbeatTime) {
    K8sWatchTaskParams watchTaskParams = AnyUtils.unpack(params.getCustomizedParams(), K8sWatchTaskParams.class);
    String watchId = k8sWatchServiceDelegate.create(watchTaskParams);
    taskWatchIdMap.put(taskId.getId(), watchId);
    logger.info("Created a watch with id {}.", watchId);
    K8sClusterConfig k8sClusterConfig =
        (K8sClusterConfig) KryoUtils.asObject(watchTaskParams.getK8SClusterConfig().toByteArray());
    if (k8sMetricsClient == null) {
      k8sMetricsClient = kubernetesClientFactory.newAdaptedClient(k8sClusterConfig, K8sMetricsClient.class);
    }
    publishNodeMetrics(k8sMetricsClient, eventPublisher, watchTaskParams.getCloudProviderId(), heartbeatTime);
    publishPodMetrics(k8sMetricsClient, eventPublisher, watchTaskParams.getCloudProviderId(), heartbeatTime);
    return true;
  }

  @VisibleForTesting
  static void publishNodeMetrics(
      K8sMetricsClient k8sMetricsClient, EventPublisher eventPublisher, String cloudProviderId, Instant heartbeatTime) {
    k8sMetricsClient.nodeMetrics()
        .list()
        .getItems()
        .stream()
        .map(nodeMetric
            -> NodeMetric.newBuilder()
                   .setCloudProviderId(cloudProviderId)
                   .setName(nodeMetric.getMetadata().getName())
                   .setTimestamp(HTimestamps.parse(nodeMetric.getTimestamp()))
                   .setWindow(HDurations.parse(nodeMetric.getWindow()))
                   .setUsage(Usage.newBuilder()
                                 .setCpu(nodeMetric.getUsage().getCpu())
                                 .setMemory(nodeMetric.getUsage().getMemory()))
                   .build())
        .forEach(nodeMetric -> eventPublisher.publishMessage(nodeMetric, HTimestamps.fromInstant(heartbeatTime)));
  }

  @VisibleForTesting
  static void publishPodMetrics(
      K8sMetricsClient k8sMetricsClient, EventPublisher eventPublisher, String cloudProviderId, Instant heartbeatTime) {
    k8sMetricsClient.podMetrics()
        .inAnyNamespace()
        .list()
        .getItems()
        .stream()
        .map(podMetric
            -> PodMetric.newBuilder()
                   .setCloudProviderId(cloudProviderId)
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
                                                                  .setCpu(container.getUsage().getCpu())
                                                                  .setMemory(container.getUsage().getMemory())
                                                                  .build())
                                                    .build())
                                         .collect(toList()))
                   .build())
        .forEach(podMetric -> eventPublisher.publishMessage(podMetric, HTimestamps.fromInstant(heartbeatTime)));
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskParams params) {
    if (null != taskWatchIdMap.get(taskId.getId())) {
      String watchId = taskWatchIdMap.get(taskId.getId());
      logger.info("Stopping the watch with id {}", watchId);
      k8sWatchServiceDelegate.delete(watchId);
      return true;
    }
    if (k8sMetricsClient != null) {
      k8sMetricsClient.close();
    }
    return false;
  }
}
