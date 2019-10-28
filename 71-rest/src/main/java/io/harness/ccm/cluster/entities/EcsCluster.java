package io.harness.ccm.cluster.entities;

import static io.harness.ccm.cluster.entities.ClusterType.AWS_ECS;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.query.Query;

@Data
@JsonTypeName("AWS_ECS")
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "EcsClusterKeys")
public class EcsCluster implements Cluster {
  final String cloudProviderId;
  final String region;
  final String clusterName;

  @Builder
  public EcsCluster(String cloudProviderId, String region, String clusterName) {
    this.cloudProviderId = cloudProviderId;
    this.region = region;
    this.clusterName = clusterName;
  }

  @Override
  public String getClusterType() {
    return AWS_ECS;
  }

  @Override
  public Query addRequiredQueryFilters(Query<ClusterRecord> query) {
    return query.field("cluster.cloudProviderId")
        .equal(this.getCloudProviderId())
        .field("cluster.region")
        .equal(this.getRegion())
        .field("cluster.clusterName")
        .equal(this.getClusterName());
  }
}
