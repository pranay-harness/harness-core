package io.harness.perpetualtask.k8s.metrics.collector;

import static io.harness.ccm.recommender.k8sworkload.RecommenderUtils.RECOMMENDER_VERSION;
import static io.harness.ccm.recommender.k8sworkload.RecommenderUtils.checkpointToProto;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.ccm.health.HealthStatusService;
import io.harness.event.client.EventPublisher;
import io.harness.event.payloads.AggregatedUsage;
import io.harness.event.payloads.ContainerStateProto;
import io.harness.event.payloads.NodeMetric;
import io.harness.event.payloads.PodMetric;
import io.harness.grpc.utils.HDurations;
import io.harness.grpc.utils.HTimestamps;
import io.harness.histogram.HistogramCheckpoint;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.metrics.client.K8sMetricsClient;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetrics;
import io.harness.perpetualtask.k8s.metrics.recommender.ContainerState;
import io.harness.perpetualtask.k8s.watch.K8sResourceStandardizer;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.List;
import javax.annotation.Nullable;

@Slf4j
public class K8sMetricCollector {
  private static final TemporalAmount AGGREGATION_WINDOW = Duration.ofMinutes(20);

  @Value
  @Builder
  private static class CacheKey {
    String name;
    @Nullable String namespace;
    @Nullable String containerName;
  }

  private final EventPublisher eventPublisher;
  private final K8sMetricsClient k8sMetricsClient;
  private final ClusterDetails clusterDetails;

  private final Cache<CacheKey, Aggregates> podMetricsCache = Caffeine.newBuilder().build();
  private final Cache<CacheKey, Aggregates> nodeMetricsCache = Caffeine.newBuilder().build();

  private final Cache<CacheKey, ContainerState> containerStatesCache = Caffeine.newBuilder().build();

  private Instant lastMetricPublished;

  public K8sMetricCollector(EventPublisher eventPublisher, K8sMetricsClient k8sMetricsClient,
      ClusterDetails clusterDetails, Instant lastMetricPublished) {
    this.eventPublisher = eventPublisher;
    this.k8sMetricsClient = k8sMetricsClient;
    this.clusterDetails = clusterDetails;
    this.lastMetricPublished = lastMetricPublished;
  }

  public void collectAndPublishMetrics(Instant now) {
    collectNodeMetrics();
    collectPodMetricsAndContainerStates();
    if (now.isAfter(this.lastMetricPublished.plus(AGGREGATION_WINDOW))) {
      publishPending(now);
    }
  }

  public void publishPending(Instant now) {
    publishNodeMetrics();
    publishPodMetrics();
    publishContainerStates();
    this.lastMetricPublished = now;
  }

  private void collectNodeMetrics() {
    List<NodeMetrics> nodeMetricsList = k8sMetricsClient.nodeMetrics().list().getObject().getItems();
    for (NodeMetrics nodeMetrics : nodeMetricsList) {
      long nodeCpuNano = K8sResourceStandardizer.getCpuNano(nodeMetrics.getUsage().getCpu());
      long nodeMemoryBytes = K8sResourceStandardizer.getMemoryByte(nodeMetrics.getUsage().getMemory());
      requireNonNull(nodeMetricsCache.get(CacheKey.builder().name(nodeMetrics.getMetadata().getName()).build(),
                         key -> new Aggregates(HDurations.parse(nodeMetrics.getWindow()))))
          .update(nodeCpuNano, nodeMemoryBytes, nodeMetrics.getTimestamp());
    }
  }

  private void collectPodMetricsAndContainerStates() {
    List<PodMetrics> podMetricsList = k8sMetricsClient.podMetrics().list().getObject().getItems();
    for (PodMetrics podMetrics : podMetricsList) {
      if (!isEmpty(podMetrics.getContainers())) {
        long podCpuNano = 0;
        long podMemoryBytes = 0;
        for (PodMetrics.Container container : podMetrics.getContainers()) {
          long containerCpuNano = K8sResourceStandardizer.getCpuNano(container.getUsage().getCpu());
          podCpuNano += containerCpuNano;
          long containerMemoryBytes = K8sResourceStandardizer.getMemoryByte(container.getUsage().getMemory());
          podMemoryBytes += containerMemoryBytes;

          ContainerState containerState =
              requireNonNull(containerStatesCache.get(CacheKey.builder()
                                                          .name(podMetrics.getMetadata().getName())
                                                          .namespace(podMetrics.getMetadata().getNamespace())
                                                          .containerName(container.getName())
                                                          .build(),
                  key -> new ContainerState(key.namespace, key.name, key.containerName)));
          double containerCpuCores = K8sResourceStandardizer.getCpuCores(container.getUsage().getCpu()).doubleValue();
          containerState.addCpuSample(containerCpuCores, podMetrics.getTimestamp());
          containerState.addMemorySample(containerMemoryBytes, podMetrics.getTimestamp());
        }
        requireNonNull(podMetricsCache.get(CacheKey.builder()
                                               .name(podMetrics.getMetadata().getName())
                                               .namespace(podMetrics.getMetadata().getNamespace())
                                               .build(),
                           key -> new Aggregates(HDurations.parse(podMetrics.getWindow()))))
            .update(podCpuNano, podMemoryBytes, podMetrics.getTimestamp());
      }
    }
  }

  private void publishNodeMetrics() {
    nodeMetricsCache.asMap()
        .entrySet()
        .stream()
        .map(e -> {
          Aggregates aggregates = e.getValue();
          return NodeMetric.newBuilder()
              .setCloudProviderId(clusterDetails.getCloudProviderId())
              .setClusterId(clusterDetails.getClusterId())
              .setKubeSystemUid(clusterDetails.getKubeSystemUid())
              .setName(e.getKey().getName())
              .setTimestamp(aggregates.getAggregateTimestamp())
              .setWindow(aggregates.getAggregateWindow())
              .setAggregatedUsage(AggregatedUsage.newBuilder()
                                      .setAvgCpuNano(aggregates.getCpu().getAverage())
                                      .setMaxCpuNano(aggregates.getCpu().getMax())
                                      .setAvgMemoryByte(aggregates.getMemory().getAverage())
                                      .setMaxMemoryByte(aggregates.getMemory().getMax())
                                      .build())
              .build();
        })
        .forEach(nodeMetric
            -> eventPublisher.publishMessage(nodeMetric, nodeMetric.getTimestamp(),
                ImmutableMap.of(HealthStatusService.CLUSTER_ID_IDENTIFIER, clusterDetails.getClusterId())));
    nodeMetricsCache.invalidateAll();
  }

  private void publishPodMetrics() {
    podMetricsCache.asMap()
        .entrySet()
        .stream()
        .map(e -> {
          Aggregates aggregates = e.getValue();
          return PodMetric.newBuilder()
              .setCloudProviderId(clusterDetails.getCloudProviderId())
              .setClusterId(clusterDetails.getClusterId())
              .setKubeSystemUid(clusterDetails.getKubeSystemUid())
              .setNamespace(e.getKey().getNamespace())
              .setName(e.getKey().getName())
              .setTimestamp(aggregates.getAggregateTimestamp())
              .setWindow(aggregates.getAggregateWindow())
              .setAggregatedUsage(AggregatedUsage.newBuilder()
                                      .setAvgCpuNano(aggregates.getCpu().getAverage())
                                      .setMaxCpuNano(aggregates.getCpu().getMax())
                                      .setAvgMemoryByte(aggregates.getMemory().getAverage())
                                      .setMaxMemoryByte(aggregates.getMemory().getMax())
                                      .build())
              .build();
        })
        .forEach(podMetric
            -> eventPublisher.publishMessage(podMetric, podMetric.getTimestamp(),
                ImmutableMap.of(HealthStatusService.CLUSTER_ID_IDENTIFIER, clusterDetails.getClusterId())));
    podMetricsCache.invalidateAll();
  }

  private void publishContainerStates() {
    containerStatesCache.asMap()
        .entrySet()
        .stream()
        .map(e -> {
          ContainerState containerState = e.getValue();
          HistogramCheckpoint histogramCheckpoint = containerState.getCpuHistogram().saveToCheckpoint();
          return ContainerStateProto.newBuilder()
              .setCloudProviderId(clusterDetails.getCloudProviderId())
              .setClusterId(clusterDetails.getClusterId())
              .setKubeSystemUid(clusterDetails.getKubeSystemUid())
              .setNamespace(e.getKey().getNamespace())
              .setPodName(e.getKey().getName())
              .setContainerName(e.getKey().getContainerName())
              .setMemoryPeak(containerState.getMemoryPeak())
              .setMemoryPeakTime(HTimestamps.fromInstant(containerState.getMemoryPeakTime()))
              .setCpuHistogram(checkpointToProto(histogramCheckpoint))
              .setFirstSampleStart(HTimestamps.fromInstant(containerState.getFirstSampleStart()))
              .setLastSampleStart(HTimestamps.fromInstant(containerState.getLastSampleStart()))
              .setTotalSamplesCount(containerState.getTotalSamplesCount())
              .setVersion(RECOMMENDER_VERSION)
              .build();
        })
        .forEach(containerStateProto
            -> eventPublisher.publishMessage(containerStateProto, containerStateProto.getFirstSampleStart(),
                ImmutableMap.of(HealthStatusService.CLUSTER_ID_IDENTIFIER, clusterDetails.getClusterId())));
    containerStatesCache.invalidateAll();
  }
}
