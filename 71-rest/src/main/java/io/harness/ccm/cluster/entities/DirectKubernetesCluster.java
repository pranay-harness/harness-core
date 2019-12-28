package io.harness.ccm.cluster.entities;

import static io.harness.ccm.cluster.entities.ClusterType.DIRECT_KUBERNETES;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.ccm.cluster.entities.ClusterRecord.ClusterRecordKeys;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.query.Query;

@Data
@JsonTypeName("DIRECT_KUBERNETES")
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "DirectKubernetesClusterKeys")
public class DirectKubernetesCluster implements Cluster {
  final String cloudProviderId;
  final String clusterName;

  public static final String cloudProviderIdField =
      ClusterRecordKeys.cluster + "." + DirectKubernetesClusterKeys.cloudProviderId;
  public static final String clusterNameField =
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
}
