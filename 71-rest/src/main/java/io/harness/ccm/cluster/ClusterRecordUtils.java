package io.harness.ccm.cluster;

import static java.util.Objects.isNull;

import com.google.inject.Singleton;

import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.ccm.cluster.entities.EcsCluster;
import lombok.experimental.UtilityClass;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

@Singleton
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
      default:
        break;
    }

    if (!isNull(cluster)) {
      clusterRecord = ClusterRecord.builder().accountId(infraMapping.getAccountId()).cluster(cluster).build();
    }

    return clusterRecord;
  }
}
