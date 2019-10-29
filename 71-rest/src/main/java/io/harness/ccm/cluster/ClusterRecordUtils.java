package io.harness.ccm.cluster;

import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.ccm.cluster.entities.EcsCluster;
import lombok.experimental.UtilityClass;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

@UtilityClass
public class ClusterRecordUtils {
  static ClusterRecord from(InfrastructureMapping infraMapping) {
    Cluster cluster = null;

    switch (InfrastructureMappingType.valueOf(infraMapping.getInfraMappingType())) {
      case DIRECT_KUBERNETES:
        DirectKubernetesInfrastructureMapping k8sInfraMapping = (DirectKubernetesInfrastructureMapping) infraMapping;
        cluster =
            DirectKubernetesCluster.builder().cloudProviderId(k8sInfraMapping.getComputeProviderSettingId()).build();
        break;
      case AWS_ECS:
        EcsInfrastructureMapping ecsInfraMapping = (EcsInfrastructureMapping) infraMapping;
        cluster = EcsCluster.builder()
                      .cloudProviderId(ecsInfraMapping.getComputeProviderSettingId())
                      .region(ecsInfraMapping.getRegion())
                      .build();
        break;
      default:
        break;
    }

    return ClusterRecord.builder().accountId(infraMapping.getAccountId()).cluster(cluster).build();
  }
}
