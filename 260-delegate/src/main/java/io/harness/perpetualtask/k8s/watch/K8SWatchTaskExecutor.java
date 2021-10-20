package io.harness.perpetualtask.k8s.watch;

import static io.harness.ccm.commons.constants.Constants.CLUSTER_ID_IDENTIFIER;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.ccm.K8sClusterInfo;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.task.citasks.cik8handler.K8sConnectorHelper;
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
import io.harness.perpetualtask.k8s.utils.ApiExceptionLogger;
import io.harness.serializer.KryoSerializer;

import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.Timestamp;
import io.kubernetes.client.ProtoClient;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.proto.Meta;
import io.kubernetes.client.proto.V1;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(HarnessTeam.CE)
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
  private final K8sConnectorHelper k8sConnectorHelper;

  @Inject
  public K8SWatchTaskExecutor(EventPublisher eventPublisher, K8sWatchServiceDelegate k8sWatchServiceDelegate,
      ApiClientFactoryImpl apiClientFactory, KryoSerializer kryoSerializer,
      ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper, K8sConnectorHelper k8sConnectorHelper) {
    this.eventPublisher = eventPublisher;
    this.k8sWatchServiceDelegate = k8sWatchServiceDelegate;
    this.apiClientFactory = apiClientFactory;
    this.kryoSerializer = kryoSerializer;
    this.containerDeploymentDelegateHelper = containerDeploymentDelegateHelper;
    this.k8sConnectorHelper = k8sConnectorHelper;
  }

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    K8sWatchTaskParams watchTaskParams = AnyUtils.unpack(params.getCustomizedParams(), K8sWatchTaskParams.class);
    try (AutoLogContext ignore1 = new PerpetualTaskLogContext(taskId.getId(), OVERRIDE_ERROR)) {
      try {
        KubernetesConfig kubernetesConfig = getKubernetesConfig(watchTaskParams);

        String watchId = k8sWatchServiceDelegate.create(watchTaskParams, kubernetesConfig);
        log.info("Ensured watch exists with id {}.", watchId);
        taskWatchIdMap.putIfAbsent(taskId.getId(), watchId);

        ApiClient apiClient = apiClientFactory.getClient(kubernetesConfig);
        DefaultK8sMetricsClient k8sMetricsClient = new DefaultK8sMetricsClient(apiClient);

        final Instant now = Instant.now();

        clusterSyncLastPublished.putIfAbsent(taskId.getId(), Instant.EPOCH);
        if (clusterSyncLastPublished.get(taskId.getId()).plus(Duration.ofHours(1)).isBefore(now)) {
          log.info("Publishing k8SClusterSyncEvent for clusterId: {} at lastProcessedTimestamp: {}",
              watchTaskParams.getClusterId(), now);
          // Use protobuf encoding for Kubernetes API calls.
          // Refer: https://kubernetes.io/docs/reference/using-api/api-concepts/#alternate-representations-of-resources
          ProtoClient pc = new ProtoClient(apiClient);
          publishClusterSyncEvent(k8sMetricsClient, pc, eventPublisher, watchTaskParams, now);
          log.info("Published k8SClusterSyncEvent for clusterId: {} at lastProcessedTimestamp: {}",
              watchTaskParams.getClusterId(), now);
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
        ApiExceptionLogger.logErrorIfNotSeenRecently(
            ex, "Encountered json deserialization error while parsing Kubernetes Api response");
        publishError(CeExceptionMessage.newBuilder()
                         .setClusterId(watchTaskParams.getClusterId())
                         .setMessage(ex.toString())
                         .build(),
            taskId);
      } catch (ApiException ex) {
        ApiExceptionLogger.logErrorIfNotSeenRecently(ex, String.format("ApiException: %s", ex.getResponseBody()));
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

  @VisibleForTesting
  static void publishClusterSyncEvent(DefaultK8sMetricsClient client, ProtoClient pc, EventPublisher eventPublisher,
      K8sWatchTaskParams watchTaskParams, Instant pollTime) throws ApiException, IOException {
    String clusterName = watchTaskParams.getClusterName();
    log.info("Querying nodes from cluster {}", clusterName);
    ProtoClient.ObjectOrStatus<V1.NodeList> nodes = pc.list(V1.NodeList.newBuilder(), "/api/v1/nodes");
    Map<String, String> nodeUidNameMap =
        nodes.object.getItemsList()
            .stream()
            .map(V1.Node::getMetadata)
            .collect(Collectors.toMap(Meta.ObjectMeta::getUid, Meta.ObjectMeta::getName));

    log.info("Querying pods from cluster {}", clusterName);
    ProtoClient.ObjectOrStatus<V1.PodList> pods = pc.list(V1.PodList.newBuilder(), "/api/v1/pods");
    Map<String, String> podUidNameMap =
        pods.object.getItemsList()
            .stream()
            .filter(pod -> "Running".equals(pod.getStatus().getPhase()))
            .map(V1.Pod::getMetadata)
            .collect(Collectors.toMap(Meta.ObjectMeta::getUid, Meta.ObjectMeta::getName));

    log.info("Querying persistent-volumes from cluster {}", clusterName);
    ProtoClient.ObjectOrStatus<V1.PersistentVolumeList> pvs =
        pc.list(V1.PersistentVolumeList.newBuilder(), "/api/v1/persistentvolumes");
    Map<String, String> pvUidNameMap =
        pvs.object.getItemsList()
            .stream()
            .map(V1.PersistentVolume::getMetadata)
            .collect(Collectors.toMap(Meta.ObjectMeta::getUid, Meta.ObjectMeta::getName));

    Timestamp timestamp = HTimestamps.fromInstant(pollTime);
    K8SClusterSyncEvent k8SClusterSyncEvent = K8SClusterSyncEvent.newBuilder()
                                                  .setClusterId(watchTaskParams.getClusterId())
                                                  .setCloudProviderId(watchTaskParams.getCloudProviderId())
                                                  .setClusterName(clusterName)
                                                  .setKubeSystemUid(K8sWatchServiceDelegate.getKubeSystemUid(client))
                                                  .putAllActiveNodeUidsMap(nodeUidNameMap)
                                                  .putAllActivePodUidsMap(podUidNameMap)
                                                  .putAllActivePvUidsMap(pvUidNameMap)
                                                  .setLastProcessedTimestamp(timestamp)
                                                  .setVersion(2)
                                                  .build();
    log.info("Publishing cluster sync event for cluster {} with {} nodes, {} pods, and {} pvs", clusterName,
        nodeUidNameMap.size(), podUidNameMap.size(), pvUidNameMap.size());
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

  private KubernetesConfig getKubernetesConfig(K8sWatchTaskParams watchTaskParams) {
    if (watchTaskParams.getK8SClusterConfig().size() != 0) {
      // Supporting deprecated K8sWatchTaskParams field
      K8sClusterConfig k8sClusterConfig =
          (K8sClusterConfig) kryoSerializer.asObject(watchTaskParams.getK8SClusterConfig().toByteArray());

      return containerDeploymentDelegateHelper.getKubernetesConfig(k8sClusterConfig, false);
    }

    K8sClusterInfo k8sClusterInfo =
        (K8sClusterInfo) kryoSerializer.asObject(watchTaskParams.getK8SClusterInfo().toByteArray());

    return k8sConnectorHelper.getKubernetesConfig(
        (KubernetesClusterConfigDTO) k8sClusterInfo.getConnectorConfigDTO(), k8sClusterInfo.getEncryptedDataDetails());
  }
}
