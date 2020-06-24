package io.harness.ccm;

import static io.harness.ccm.cluster.entities.ClusterType.AWS_ECS;
import static io.harness.ccm.cluster.entities.ClusterType.AZURE_KUBERNETES;
import static io.harness.ccm.cluster.entities.ClusterType.DIRECT_KUBERNETES;
import static io.harness.ccm.cluster.entities.ClusterType.GCP_KUBERNETES;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Objects.isNull;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.ccm.cluster.entities.AzureKubernetesCluster;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.ccm.cluster.entities.EcsCluster;
import io.harness.ccm.cluster.entities.GcpKubernetesCluster;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClientRegistry;
import io.harness.perpetualtask.PerpetualTaskServiceInprocClient;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.ecs.EcsPerpetualTaskClientParams;
import io.harness.perpetualtask.k8s.watch.K8sWatchPerpetualTaskServiceClient;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.SettingAttribute;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Singleton
@Slf4j
public class CEPerpetualTaskManager {
  private ClusterRecordService clusterRecordService;
  private PerpetualTaskService perpetualTaskService;
  private final PerpetualTaskServiceClientRegistry clientRegistry;

  @Inject
  public CEPerpetualTaskManager(ClusterRecordService clusterRecordService, PerpetualTaskService perpetualTaskService,
      PerpetualTaskServiceClientRegistry clientRegistry) {
    this.clusterRecordService = clusterRecordService;
    this.perpetualTaskService = perpetualTaskService;
    this.clientRegistry = clientRegistry;
  }

  public boolean createPerpetualTasks(Account account, String clusterType) {
    List<ClusterRecord> clusterRecords = clusterRecordService.list(account.getUuid(), clusterType);
    clusterRecords.stream()
        .filter(clusterRecord -> isEmpty(clusterRecord.getPerpetualTaskIds()))
        .forEach(this ::createPerpetualTasks);
    return true;
  }

  public boolean deletePerpetualTasks(Account account, String clusterType) {
    List<ClusterRecord> clusterRecords = clusterRecordService.list(account.getUuid(), clusterType);
    clusterRecords.stream()
        .filter(clusterRecord -> isNotEmpty(clusterRecord.getPerpetualTaskIds()))
        .forEach(this ::deletePerpetualTasks);
    return true;
  }

  public boolean createPerpetualTasks(SettingAttribute cloudProvider) {
    List<ClusterRecord> clusterRecords = getClusterRecords(cloudProvider);
    if (!isNull(clusterRecords)) {
      clusterRecords.forEach(this ::createPerpetualTasks);
    }
    return true;
  }

  public boolean resetPerpetualTasks(SettingAttribute cloudProvider) {
    List<ClusterRecord> clusterRecords = getClusterRecords(cloudProvider);
    if (!isNull(clusterRecords)) {
      clusterRecords.forEach(this ::resetPerpetualTasks);
    }
    return true;
  }

  public boolean deletePerpetualTasks(SettingAttribute cloudProvider) {
    List<ClusterRecord> clusterRecords = getClusterRecords(cloudProvider);
    if (!isNull(clusterRecords)) {
      for (ClusterRecord clusterRecord : clusterRecords) {
        deletePerpetualTasks(clusterRecord);
      }
    }
    return true;
  }

  // find all the related Clusters
  private List<ClusterRecord> getClusterRecords(SettingAttribute cloudProvider) {
    return clusterRecordService.list(cloudProvider.getAccountId(), null, cloudProvider.getUuid());
  }

  public boolean createPerpetualTasks(ClusterRecord clusterRecord) {
    Cluster cluster = clusterRecord.getCluster();
    PerpetualTaskServiceInprocClient client = null;
    switch (cluster.getClusterType()) {
      case DIRECT_KUBERNETES:
        client = clientRegistry.getInprocClient(PerpetualTaskType.K8S_WATCH);
        String watcherTaskId = client.create(clusterRecord.getAccountId(),
            K8sWatchPerpetualTaskServiceClient.from(
                cluster, clusterRecord.getUuid(), ((DirectKubernetesCluster) cluster).getClusterName()));
        clusterRecordService.attachPerpetualTaskId(clusterRecord, watcherTaskId);
        break;
      case AWS_ECS:
        EcsCluster ecsCluster = (EcsCluster) cluster;
        client = clientRegistry.getInprocClient(PerpetualTaskType.ECS_CLUSTER);
        if (null != ecsCluster.getRegion() && null != ecsCluster.getClusterName()) {
          EcsPerpetualTaskClientParams ecsPerpetualTaskClientParams =
              new EcsPerpetualTaskClientParams(ecsCluster.getRegion(), ecsCluster.getCloudProviderId(),
                  ecsCluster.getClusterName(), clusterRecord.getUuid());
          String ecsTaskId = client.create(clusterRecord.getAccountId(), ecsPerpetualTaskClientParams);
          clusterRecordService.attachPerpetualTaskId(clusterRecord, ecsTaskId);
        } else {
          logger.info("Not creating perpetual task for cluster {}", clusterRecord.getUuid());
        }
        break;
      case GCP_KUBERNETES:
        GcpKubernetesCluster gcpKubernetesCluster = (GcpKubernetesCluster) cluster;
        client = clientRegistry.getInprocClient(PerpetualTaskType.K8S_WATCH);
        String gcpK8STaskId = client.create(clusterRecord.getAccountId(),
            K8sWatchPerpetualTaskServiceClient.from(
                cluster, clusterRecord.getUuid(), gcpKubernetesCluster.getClusterName()));
        clusterRecordService.attachPerpetualTaskId(clusterRecord, gcpK8STaskId);
        break;
      case AZURE_KUBERNETES:
        AzureKubernetesCluster azureKubernetesCluster = (AzureKubernetesCluster) cluster;
        client = clientRegistry.getInprocClient(PerpetualTaskType.K8S_WATCH);
        String azureK8STaskId = client.create(clusterRecord.getAccountId(),
            K8sWatchPerpetualTaskServiceClient.from(
                cluster, clusterRecord.getUuid(), azureKubernetesCluster.getClusterName()));
        clusterRecordService.attachPerpetualTaskId(clusterRecord, azureK8STaskId);
        break;
      default:
        break;
    }
    return true;
  }

  public void resetPerpetualTasks(ClusterRecord clusterRecord) {
    // find all the existing perpetual Tasks for these clusters
    List<String> taskIds = Arrays.asList(clusterRecord.getPerpetualTaskIds());
    // reset all the existing perpetual tasks
    taskIds.forEach(taskId -> perpetualTaskService.resetTask(clusterRecord.getAccountId(), taskId));
  }

  // delete all of the perpetual tasks associated with the Cluster
  public boolean deletePerpetualTasks(ClusterRecord clusterRecord) {
    List<String> taskIds =
        Arrays.asList(Optional.ofNullable(clusterRecord.getPerpetualTaskIds()).orElse(new String[0]));
    for (String taskId : taskIds) {
      perpetualTaskService.deleteTask(clusterRecord.getAccountId(), taskId);
      clusterRecordService.removePerpetualTaskId(clusterRecord, taskId);
    }
    return true;
  }
}
