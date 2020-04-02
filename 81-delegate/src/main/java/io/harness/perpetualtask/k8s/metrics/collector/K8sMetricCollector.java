package io.harness.perpetualtask.k8s.metrics.collector;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.ccm.health.HealthStatusService;
import io.harness.event.client.EventPublisher;
import io.harness.event.payloads.AggregatedUsage;
import io.harness.event.payloads.NodeMetric;
import io.harness.event.payloads.PodMetric;
import io.harness.grpc.utils.HDurations;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.metrics.client.K8sMetricsClient;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetrics;
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
  }

  private final EventPublisher eventPublisher;
  private final K8sMetricsClient k8sMetricsClient;
  private final ClusterDetails clusterDetails;

  private final Cache<CacheKey, Aggregates> podMetricsCache = Caffeine.newBuilder().build();
  private final Cache<CacheKey, Aggregates> nodeMetricsCache = Caffeine.newBuilder().build();

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
    collectPodMetrics();
    if (now.isAfter(this.lastMetricPublished.plus(AGGREGATION_WINDOW))) {
      publishPending(now);
    }
  }

  public void publishPending(Instant now) {
    publishNodeMetrics();
    publishPodMetrics();
    this.lastMetricPublished = now;
  }

  private void collectNodeMetrics() {
    List<NodeMetrics> nodeMetricsList = k8sMetricsClient.nodeMetrics().list().getItems();
    for (NodeMetrics nodeMetrics : nodeMetricsList) {
      long nodeCpuNano = K8sResourceStandardizer.getCpuNano(nodeMetrics.getUsage().getCpu());
      long nodeMemoryBytes = K8sResourceStandardizer.getMemoryByte(nodeMetrics.getUsage().getMemory());
      requireNonNull(nodeMetricsCache.get(CacheKey.builder().name(nodeMetrics.getMetadata().getName()).build(),
                         key -> new Aggregates(HDurations.parse(nodeMetrics.getWindow()))))
          .update(nodeCpuNano, nodeMemoryBytes, nodeMetrics.getTimestamp());
    }
  }

  private void collectPodMetrics() {
    List<PodMetrics> podMetricsList = k8sMetricsClient.podMetrics().inAnyNamespace().list().getItems();
    for (PodMetrics podMetrics : podMetricsList) {
      long podCpuNano = 0;
      long podMemoryBytes = 0;
      for (PodMetrics.Container container : podMetrics.getContainers()) {
        podCpuNano += K8sResourceStandardizer.getCpuNano(container.getUsage().getCpu());
        podMemoryBytes += K8sResourceStandardizer.getMemoryByte(container.getUsage().getMemory());
      }
      requireNonNull(podMetricsCache.get(CacheKey.builder()
                                             .name(podMetrics.getMetadata().getName())
                                             .namespace(podMetrics.getMetadata().getNamespace())
                                             .build(),
                         key -> new Aggregates(HDurations.parse(podMetrics.getWindow()))))
          .update(podCpuNano, podMemoryBytes, podMetrics.getTimestamp());
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
}
