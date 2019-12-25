package io.harness.ccm.cluster;

import static java.util.Objects.isNull;

import io.harness.ccm.cluster.entities.AzureKubernetesCluster;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.ccm.cluster.entities.EcsCluster;
import io.harness.ccm.cluster.entities.GcpKubernetesCluster;
import lombok.experimental.UtilityClass;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

@UtilityClass
public class ClusterRecordUtils {
  static ClusterRecord from(InfrastructureMapping infraMapping) {
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
