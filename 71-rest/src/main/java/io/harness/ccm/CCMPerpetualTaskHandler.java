package io.harness.ccm;

import static io.harness.ccm.cluster.entities.ClusterType.DIRECT_KUBERNETES;
import static software.wings.beans.FeatureName.PERPETUAL_TASK_SERVICE;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import io.harness.ccm.cluster.ClusterRecordObserver;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.perpetualtask.k8s.watch.K8WatchPerpetualTaskClientParams;
import io.harness.perpetualtask.k8s.watch.K8sWatchPerpetualTaskServiceClient;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.intfc.FeatureFlagService;

@Slf4j
public class CCMPerpetualTaskHandler implements ClusterRecordObserver {
  private FeatureFlagService featureFlagService;
  private K8sWatchPerpetualTaskServiceClient k8SWatchPerpetualTaskServiceClient;

  @Inject
  public CCMPerpetualTaskHandler(
      FeatureFlagService featureFlagService, K8sWatchPerpetualTaskServiceClient k8SWatchPerpetualTaskServiceClient) {
    this.featureFlagService = featureFlagService;
    this.k8SWatchPerpetualTaskServiceClient = k8SWatchPerpetualTaskServiceClient;
  }

  @Override
  public void onUpserted(ClusterRecord clusterRecord) {
    try {
      startPerpetualTask(clusterRecord);
    } catch (Exception e) {
      logger.error("Failed to create a Perpetual Task for the Cluster with id={}", clusterRecord.getUuid(), e);
    }
  }

  @Override
  public void onDeleted(String accountId, String clusterId) {
    try {
      if (featureFlagService.isGlobalEnabled(PERPETUAL_TASK_SERVICE)) {
        // TODO(Tang) change this delete from perpetual task
        k8SWatchPerpetualTaskServiceClient.delete(accountId, null);
      }
    } catch (Exception e) {
      logger.error("Failed to delete the Perpetual Tasks associated with the Cluster with id={}", clusterId, e);
    }
  }

  @VisibleForTesting
  void startPerpetualTask(ClusterRecord clusterRecord) {
    // the following IF statement is for testing purpose only
    // TODO: make it applicable to more types of clusters
    Cluster cluster = clusterRecord.getCluster();
    if (featureFlagService.isGlobalEnabled(PERPETUAL_TASK_SERVICE)) {
      switch (cluster.getClusterType()) {
        case DIRECT_KUBERNETES:
          K8WatchPerpetualTaskClientParams podWatchParams =
              new K8WatchPerpetualTaskClientParams(((DirectKubernetesCluster) cluster).getCloudProviderId(), "Pod");
          k8SWatchPerpetualTaskServiceClient.create(clusterRecord.getAccountId(), podWatchParams);
          K8WatchPerpetualTaskClientParams nodeWatchParams =
              new K8WatchPerpetualTaskClientParams(((DirectKubernetesCluster) cluster).getCloudProviderId(), "Node");
          k8SWatchPerpetualTaskServiceClient.create(clusterRecord.getAccountId(), nodeWatchParams);
          break;
        default:
          break;
      }
    }
  }
}
