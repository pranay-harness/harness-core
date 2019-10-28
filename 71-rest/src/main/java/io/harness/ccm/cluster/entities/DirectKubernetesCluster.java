package io.harness.ccm.cluster.entities;

import static io.harness.ccm.cluster.entities.ClusterType.DIRECT_KUBERNETES;

import com.fasterxml.jackson.annotation.JsonTypeName;
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

  @Builder
  public DirectKubernetesCluster(String cloudProviderId) {
    this.cloudProviderId = cloudProviderId;
  }

  @Override
  public String getClusterType() {
    return DIRECT_KUBERNETES;
  }

  @Override
  public Query addRequiredQueryFilters(Query<ClusterRecord> query) {
    return query.field("cluster.cloudProviderId").equal(this.getCloudProviderId());
  }
}
