package io.harness.ccm.cluster.entities;

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

import static io.harness.ccm.cluster.entities.ClusterType.AZURE_KUBERNETES;

@Data
@JsonTypeName("AZURE_KUBERNETES")
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "AzureKubernetesClusterKeys")
public class AzureKubernetesCluster implements Cluster, KubernetesCluster {
  private String cloudProviderId;
  private String clusterName;
  private String resourceGroup;
  private String subscriptionId;

  public static final String cloudProviderField =
      ClusterRecordKeys.cluster + "." + AzureKubernetesClusterKeys.cloudProviderId;
  public static final String clusterNameField =
      ClusterRecordKeys.cluster + "." + AzureKubernetesClusterKeys.clusterName;

  @Builder
  public AzureKubernetesCluster(
      String cloudProviderId, String clusterName, String resourceGroup, String subscriptionId) {
    this.cloudProviderId = cloudProviderId;
    this.clusterName = clusterName;
    this.resourceGroup = resourceGroup;
    this.subscriptionId = subscriptionId;
  }

  @Override
  public String getClusterType() {
    return AZURE_KUBERNETES;
  }

  @Override
  public void addRequiredQueryFilters(Query<ClusterRecord> query) {
    query.field(cloudProviderField)
        .equal(this.getCloudProviderId())
        .field(clusterNameField)
        .equal(this.getClusterName());
  }

  @Override
  public K8sClusterConfig toK8sClusterConfig(SettingValue cloudProvider, List<EncryptedDataDetail> encryptionDetails) {
    return K8sClusterConfig.builder()
        .cloudProvider(cloudProvider)
        .azureKubernetesCluster(software.wings.beans.AzureKubernetesCluster.builder()
                                    .subscriptionId(subscriptionId)
                                    .resourceGroup(resourceGroup)
                                    .name(clusterName)
                                    .build())
        .cloudProviderEncryptionDetails(encryptionDetails)
        .build();
  }
}
