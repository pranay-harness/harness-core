package io.harness.ccm.cluster.entities;

import static io.harness.ccm.cluster.entities.ClusterType.AZURE_KUBERNETES;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.ccm.cluster.entities.ClusterRecord.ClusterRecordKeys;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.query.Query;

@Data
@JsonTypeName("AZURE_KUBERNETES")
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "AzureKubernetesClusterKeys")
public class AzureKubernetesCluster implements Cluster {
  final String cloudProviderId;
  final String clusterName;

  public static final String cloudProviderField =
      ClusterRecordKeys.cluster + "." + AzureKubernetesClusterKeys.cloudProviderId;
  public static final String clusterNameField =
      ClusterRecordKeys.cluster + "." + AzureKubernetesClusterKeys.clusterName;

  @Builder
  public AzureKubernetesCluster(String cloudProviderId, String clusterName) {
    this.cloudProviderId = cloudProviderId;
    this.clusterName = clusterName;
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
}
