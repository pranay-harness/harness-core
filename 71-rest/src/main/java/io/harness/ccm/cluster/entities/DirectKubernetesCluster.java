package io.harness.ccm.cluster.entities;

import static io.harness.ccm.cluster.entities.ClusterType.DIRECT_KUBERNETES;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.ccm.cluster.entities.ClusterRecord.ClusterRecordKeys;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.query.Query;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.settings.SettingValue;

import java.util.List;

@Data
@JsonTypeName("DIRECT_KUBERNETES")
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "DirectKubernetesClusterKeys")
public class DirectKubernetesCluster implements Cluster, KubernetesCluster {
  private String cloudProviderId;
  private String clusterName;

  private static final String cloudProviderIdField =
      ClusterRecordKeys.cluster + "." + DirectKubernetesClusterKeys.cloudProviderId;
  private static final String clusterNameField =
      ClusterRecordKeys.cluster + "." + DirectKubernetesClusterKeys.clusterName;

  @Builder
  public DirectKubernetesCluster(String cloudProviderId, String clusterName) {
    this.cloudProviderId = cloudProviderId;
    this.clusterName = clusterName;
  }

  @Override
  public String getClusterType() {
    return DIRECT_KUBERNETES;
  }

  @Override
  public void addRequiredQueryFilters(Query<ClusterRecord> query) {
    query.field(cloudProviderIdField).equal(this.getCloudProviderId());
  }

  @Override
  public K8sClusterConfig toK8sClusterConfig(SettingValue cloudProvider, List<EncryptedDataDetail> encryptionDetails) {
    return K8sClusterConfig.builder()
        .cloudProvider(cloudProvider)
        .cloudProviderEncryptionDetails(encryptionDetails)
        .build();
  }
}
