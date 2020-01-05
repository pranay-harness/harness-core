package io.harness.ccm.cluster;

import static java.util.Objects.isNull;
import static software.wings.beans.InfrastructureType.AWS_ECS;
import static software.wings.beans.InfrastructureType.AZURE_KUBERNETES;
import static software.wings.beans.InfrastructureType.DIRECT_KUBERNETES;
import static software.wings.beans.InfrastructureType.GCP_KUBERNETES_ENGINE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.cluster.entities.AzureKubernetesCluster;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.ccm.cluster.entities.EcsCluster;
import io.harness.ccm.cluster.entities.GcpKubernetesCluster;
import io.harness.observer.Subject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.SettingAttribute;
import software.wings.infra.AwsEcsInfrastructure;
import software.wings.infra.AzureKubernetesService;
import software.wings.infra.DirectKubernetesInfrastructure;
import software.wings.infra.GoogleKubernetesEngine;
import software.wings.infra.InfraMappingInfrastructureProvider;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.AppService;

import java.util.List;

@Slf4j
@Singleton
public class ClusterRecordServiceImpl implements ClusterRecordService {
  @Inject private ClusterRecordDao clusterRecordDao;
  @Inject private AppService appService;
  @Inject @Getter private Subject<ClusterRecordObserver> subject = new Subject<>();

  @Override
  public ClusterRecord upsert(ClusterRecord clusterRecord) {
    ClusterRecord prevClusterRecord = clusterRecordDao.get(clusterRecord);
    ClusterRecord upsertedClusterRecord = clusterRecordDao.upsertCluster(clusterRecord);

    if (!isNull(prevClusterRecord)) {
      logger.info("Updated the existing {} Cluster with id={}.", upsertedClusterRecord.getCluster().getClusterType(),
          upsertedClusterRecord.getUuid());
    } else {
      logger.info("Upserted a new {} Cluster with id={}.", upsertedClusterRecord.getCluster().getClusterType(),
          upsertedClusterRecord.getUuid());
      try {
        subject.fireInform(ClusterRecordObxxxxxxxx:onUpserted, upsertedClusterRecord);
      } catch (Exception e) {
        logger.error("Failed to inform the observers for the Cluster with id={}", upsertedClusterRecord.getUuid(), e);
      }
    }
    return upsertedClusterRecord;
  }

  @Override
  public ClusterRecord get(String clusterId) {
    return clusterRecordDao.get(clusterId);
  }

  @Override
  public List<ClusterRecord> list(String accountId, String cloudProviderId, Integer count, Integer startIndex) {
    return clusterRecordDao.list(accountId, cloudProviderId, count, startIndex);
  }

  @Override
  public List<ClusterRecord> list(String accountId, String cloudProviderId) {
    return clusterRecordDao.list(accountId, cloudProviderId, 0, 0);
  }

  @Override
  public boolean delete(String accountId, String cloudProviderId) {
    // get the list of Clusters associated with the cloudProvider
    List<ClusterRecord> clusterRecords = list(accountId, cloudProviderId);
    if (isNull(clusterRecords)) {
      logger.warn("Cloud Provider with id={} has no Clusters to be deleted.", cloudProviderId);
    } else {
      for (ClusterRecord clusterRecord : clusterRecords) {
        try {
          subject.fireInform(ClusterRecordObxxxxxxxx:onDeleting, clusterRecord);
        } catch (Exception e) {
          logger.error("Failed to inform the Observers for ClusterRecord with id={}", clusterRecord.getCluster(), e);
        }
      }
    }
    return clusterRecordDao.delete(accountId, cloudProviderId);
  }

  @Override
  public ClusterRecord attachPerpetualTaskId(ClusterRecord clusterRecord, String taskId) {
    return clusterRecordDao.insertTask(clusterRecord, taskId);
  }

  @Override
  public ClusterRecord removePerpetualTaskId(ClusterRecord clusterRecord, String taskId) {
    return clusterRecordDao.removeTask(clusterRecord, taskId);
  }

  @Override
  public ClusterRecord from(SettingAttribute cloudProvider) {
    ClusterRecord clusterRecord = null;
    Cluster cluster = null;

    switch (cloudProvider.getValue().getType()) {
      case "KUBERNETES_CLUSTER":
        cluster = DirectKubernetesCluster.builder()
                      .cloudProviderId(cloudProvider.getUuid())
                      .clusterName(cloudProvider.getName())
                      .build();
        break;
      default:
        break;
    }
    if (!isNull(cluster)) {
      clusterRecord = ClusterRecord.builder().accountId(cloudProvider.getAccountId()).cluster(cluster).build();
    }

    return clusterRecord;
  }

  @Override
  public ClusterRecord from(InfrastructureDefinition infrastructureDefinition) {
    ClusterRecord clusterRecord = null;
    Cluster cluster = null;

    InfraMappingInfrastructureProvider infraMappingInfrastructureProvider =
        infrastructureDefinition.getInfrastructure();
    switch (infraMappingInfrastructureProvider.getInfrastructureType()) {
      case AWS_ECS:
        AwsEcsInfrastructure ecsInfrastructure = (AwsEcsInfrastructure) infraMappingInfrastructureProvider;
        cluster = EcsCluster.builder()
                      .cloudProviderId(ecsInfrastructure.getCloudProviderId())
                      .region(ecsInfrastructure.getRegion())
                      .clusterName(ecsInfrastructure.getClusterName())
                      .build();
        break;
      case DIRECT_KUBERNETES:
        DirectKubernetesInfrastructure k8sInfrastructure =
            (DirectKubernetesInfrastructure) infraMappingInfrastructureProvider;
        cluster = DirectKubernetesCluster.builder()
                      .cloudProviderId(k8sInfrastructure.getCloudProviderId())
                      .clusterName(k8sInfrastructure.getClusterName())
                      .build();
        break;
      case GCP_KUBERNETES_ENGINE:
        GoogleKubernetesEngine gcpK8SInfrastructure = (GoogleKubernetesEngine) infraMappingInfrastructureProvider;
        cluster = GcpKubernetesCluster.builder()
                      .cloudProviderId(gcpK8SInfrastructure.getCloudProviderId())
                      .clusterName(gcpK8SInfrastructure.getClusterName())
                      .build();
        break;
      case AZURE_KUBERNETES:
        AzureKubernetesService azureKubernetesInfrastructure =
            (AzureKubernetesService) infraMappingInfrastructureProvider;
        cluster = AzureKubernetesCluster.builder()
                      .cloudProviderId(azureKubernetesInfrastructure.getCloudProviderId())
                      .clusterName(azureKubernetesInfrastructure.getClusterName())
                      .build();
        break;
      default:
        break;
    }

    if (!isNull(cluster)) {
      Application application = appService.get(infrastructureDefinition.getAppId());
      clusterRecord = ClusterRecord.builder().accountId(application.getAccountId()).cluster(cluster).build();
    }
    return clusterRecord;
  }

  @Override
  public ClusterRecord from(InfrastructureMapping infraMapping) {
    ClusterRecord clusterRecord = null;
    Cluster cluster = null;

    switch (InfrastructureMappingType.valueOf(infraMapping.getInfraMappingType())) {
      case DIRECT_KUBERNETES:
        DirectKubernetesInfrastructureMapping k8sInfraMapping = (DirectKubernetesInfrastructureMapping) infraMapping;
        cluster = DirectKubernetesCluster.builder()
                      .cloudProviderId(k8sInfraMapping.getComputeProviderSettingId())
                      .clusterName(k8sInfraMapping.getComputeProviderName())
                      .build();
        break;
      case AWS_ECS:
        EcsInfrastructureMapping ecsInfraMapping = (EcsInfrastructureMapping) infraMapping;
        cluster = EcsCluster.builder()
                      .cloudProviderId(ecsInfraMapping.getComputeProviderSettingId())
                      .region(ecsInfraMapping.getRegion())
                      .clusterName(ecsInfraMapping.getClusterName())
                      .build();
        break;
      case GCP_KUBERNETES:
        GcpKubernetesInfrastructureMapping gcpKubernetesInfrastructureMapping =
            (GcpKubernetesInfrastructureMapping) infraMapping;
        cluster = GcpKubernetesCluster.builder()
                      .cloudProviderId(gcpKubernetesInfrastructureMapping.getComputeProviderSettingId())
                      .clusterName(gcpKubernetesInfrastructureMapping.getClusterName())
                      .build();
        break;
      case AZURE_KUBERNETES:
        AzureKubernetesInfrastructureMapping azureKubernetesInfrastructureMapping =
            (AzureKubernetesInfrastructureMapping) infraMapping;
        cluster = AzureKubernetesCluster.builder()
                      .cloudProviderId(azureKubernetesInfrastructureMapping.getComputeProviderSettingId())
                      .clusterName(azureKubernetesInfrastructureMapping.getClusterName())
                      .build();
        break;
      default:
        break;
    }

    if (!isNull(cluster)) {
      clusterRecord = ClusterRecord.builder().accountId(infraMapping.getAccountId()).cluster(cluster).build();
    }

    return clusterRecord;
  }
}
