package io.harness.perpetualtask.k8s.watch;

import static io.harness.ccm.health.HealthStatusService.CLUSTER_ID_IDENTIFIER;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.event.client.EventPublisher;
import io.harness.event.payloads.CeExceptionMessage;
import io.harness.grpc.utils.AnyUtils;
import io.harness.grpc.utils.HTimestamps;
import io.harness.k8s.apiclient.ApiClientFactoryImpl;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.logging.AutoLogContext;
import io.harness.perpetualtask.PerpetualTaskExecutionParams;
import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskLogContext;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.metrics.client.impl.DefaultK8sMetricsClient;
import io.harness.perpetualtask.k8s.metrics.collector.K8sMetricCollector;
import io.harness.serializer.KryoSerializer;

import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.Timestamp;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import io.kubernetes.client.openapi.models.V1Pod;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class K8SWatchTaskExecutor implements PerpetualTaskExecutor {
  private static final String MESSAGE_PROCESSOR_TYPE = "EXCEPTION";
  private final Map<String, String> taskWatchIdMap = new ConcurrentHashMap<>();
  private final Map<String, K8sMetricCollector> metricCollectors = new ConcurrentHashMap<>();
  private final Map<String, Instant> clusterSyncLastPublished = new ConcurrentHashMap<>();

  private final EventPublisher eventPublisher;
  private final K8sWatchServiceDelegate k8sWatchServiceDelegate;
  private final ApiClientFactoryImpl apiClientFactory;
  private final KryoSerializer kryoSerializer;
  private final ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  private final Cache<String, Boolean> recentlyLoggedExceptions;

  @Inject
  public K8SWatchTaskExecutor(EventPublisher eventPublisher, K8sWatchServiceDelegate k8sWatchServiceDelegate,
      ApiClientFactoryImpl apiClientFactory, KryoSerializer kryoSerializer,
      ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper) {
    this.eventPublisher = eventPublisher;
    this.k8sWatchServiceDelegate = k8sWatchServiceDelegate;
    this.apiClientFactory = apiClientFactory;
    this.kryoSerializer = kryoSerializer;
    this.containerDeploymentDelegateHelper = containerDeploymentDelegateHelper;
    recentlyLoggedExceptions = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();
  }

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    K8sWatchTaskParams watchTaskParams = AnyUtils.unpack(params.getCustomizedParams(), K8sWatchTaskParams.class);
    try (AutoLogContext ignore1 = new PerpetualTaskLogContext(taskId.getId(), OVERRIDE_ERROR)) {
      try {
        Instant now = Instant.now();
        String watchId = k8sWatchServiceDelegate.create(watchTaskParams);
        log.info("Ensured watch exists with id {}.", watchId);
        K8sClusterConfig k8sClusterConfig =
            (K8sClusterConfig) kryoSerializer.asObject(watchTaskParams.getK8SClusterConfig().toByteArray());

        KubernetesConfig kubernetesConfig =
            containerDeploymentDelegateHelper.getKubernetesConfig(k8sClusterConfig, false);

        DefaultK8sMetricsClient k8sMetricsClient =
            new DefaultK8sMetricsClient(apiClientFactory.getClient(kubernetesConfig));

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
                  return new K8sMetricCollector(eventPublisher, clusterDetails, heartbeatTime);
                })
            .collectAndPublishMetrics(k8sMetricsClient, now);

      } catch (JsonSyntaxException ex) {
        logIfNotSeenRecently(ex, "Encountered json deserialization error while parsing Kubernetes Api response");
        publishError(CeExceptionMessage.newBuilder()
                         .setClusterId(watchTaskParams.getClusterId())
                         .setMessage(ex.toString())
                         .build(),
            taskId);
      } catch (ApiException ex) {
        logIfNotSeenRecently(ex, String.format("ApiException: %s", ex.getResponseBody()));
        publishError(CeExceptionMessage.newBuilder()
                         .setClusterId(watchTaskParams.getClusterId())
                         .setMessage(format(
                             "code=[%s] message=[%s] body=[%s]", ex.getCode(), ex.getMessage(), ex.getResponseBody()))
                         .build(),
            taskId);
      } catch (Exception ex) {
        log.error("Unknown error occured from {} while collecting metrics.", taskId, ex);
      }
      return PerpetualTaskResponse.builder().responseCode(200).responseMessage("success").build();
    }
  }

  private void publishError(CeExceptionMessage ceExceptionMessage, PerpetualTaskId taskId) {
    try {
      eventPublisher.publishMessage(
          ceExceptionMessage, HTimestamps.fromInstant(Instant.now()), Collections.emptyMap(), MESSAGE_PROCESSOR_TYPE);
    } catch (Exception ex) {
      log.error("Failed to publish failure from {} to the Event Server.", taskId, ex);
    }
  }

  private void logIfNotSeenRecently(Exception ex, String msg) {
    recentlyLoggedExceptions.get(msg, k -> {
      log.error(msg, ex);
      return Boolean.TRUE;
    });
  }

  @VisibleForTesting
  static void publishClusterSyncEvent(DefaultK8sMetricsClient client, EventPublisher eventPublisher,
      K8sWatchTaskParams watchTaskParams, Instant pollTime) throws ApiException {
    List<String> nodeUidList = client.listNode(null, null, null, null, null, null, null, null, null)
                                   .getItems()
                                   .stream()
                                   .map(V1Node::getMetadata)
                                   .map(V1ObjectMeta::getUid)
                                   .collect(Collectors.toList());
    List<String> podUidList = client.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null)
                                  .getItems()
                                  .stream()
                                  .filter(pod -> "Running".equals(pod.getStatus().getPhase()))
                                  .map(V1Pod::getMetadata)
                                  .map(V1ObjectMeta::getUid)
                                  .collect(Collectors.toList());
    List<String> pvUidList = new ArrayList<>();

    // optional as of now, will remove when the permission is mandatory.
    try {
      pvUidList.addAll(client.listPersistentVolume(null, null, null, null, null, null, null, null, null)
                           .getItems()
                           .stream()
                           .map(V1PersistentVolume::getMetadata)
                           .map(V1ObjectMeta::getUid)
                           .collect(Collectors.toList()));
    } catch (ApiException ex) {
      log.warn("ListPersistentVolume failed: code=[{}], headers=[{}]", ex.getCode(), ex.getResponseHeaders(), ex);
    }

    Timestamp timestamp = HTimestamps.fromInstant(pollTime);
    K8SClusterSyncEvent k8SClusterSyncEvent = K8SClusterSyncEvent.newBuilder()
                                                  .setClusterId(watchTaskParams.getClusterId())
                                                  .setCloudProviderId(watchTaskParams.getCloudProviderId())
                                                  .setClusterName(watchTaskParams.getClusterName())
                                                  .setKubeSystemUid(K8sWatchServiceDelegate.getKubeSystemUid(client))
                                                  .addAllActiveNodeUids(nodeUidList)
                                                  .addAllActivePodUids(podUidList)
                                                  .addAllActivePvUids(pvUidList)
                                                  .setLastProcessedTimestamp(timestamp)
                                                  .build();
    eventPublisher.publishMessage(
        k8SClusterSyncEvent, timestamp, ImmutableMap.of(CLUSTER_ID_IDENTIFIER, watchTaskParams.getClusterId()));
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    try (AutoLogContext ignore1 = new PerpetualTaskLogContext(taskId.getId(), OVERRIDE_ERROR)) {
      if (taskWatchIdMap.get(taskId.getId()) == null) {
        return false;
      }
      String watchId = taskWatchIdMap.get(taskId.getId());
      log.info("Stopping the watch with id {}", watchId);
      k8sWatchServiceDelegate.delete(watchId);
      taskWatchIdMap.remove(taskId.getId());

      metricCollectors.computeIfPresent(taskId.getId(), (id, metricCollector) -> {
        metricCollector.publishPending(Instant.now());
        return null;
      });
      return true;
    }
  }
}
