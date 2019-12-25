package io.harness.ccm.cluster.entities;

import static io.harness.ccm.cluster.entities.ClusterType.GCP_KUBERNETES;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.ccm.cluster.entities.ClusterRecord.ClusterRecordKeys;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.query.Query;

@Data
@JsonTypeName("GCP_KUBERNETES")
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "GcpKubernetesClusterKeys")
public class GcpKubernetesCluster implements Cluster {
  final String cloudProviderId;
  final String clusterName;

  public static final String cloudProviderField =
      ClusterRecordKeys.cluster + "." + GcpKubernetesClusterKeys.cloudProviderId;
  public static final String clusterNameField = ClusterRecordKeys.cluster + "." + GcpKubernetesClusterKeys.clusterName;

  @Builder
  public GcpKubernetesCluster(String cloudProviderId, String clusterName) {
    this.cloudProviderId = cloudProviderId;
    this.clusterName = clusterName;
  }

  @Override
  public String getClusterType() {
    return GCP_KUBERNETES;
  }

  @Override
  public void addRequiredQueryFilters(Query<ClusterRecord> query) {
    query.field(cloudProviderField)
        .equal(this.getCloudProviderId())
        .field(clusterNameField)
        .equal(this.getClusterName());
  }
}
