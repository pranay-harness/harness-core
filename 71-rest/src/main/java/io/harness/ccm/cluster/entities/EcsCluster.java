package io.harness.ccm.cluster.entities;

import static io.harness.ccm.cluster.entities.ClusterType.AWS_ECS;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.ccm.cluster.entities.ClusterRecord.ClusterRecordKeys;
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

  public static final String cloudProviderField = ClusterRecordKeys.cluster + "." + EcsClusterKeys.cloudProviderId;
  public static final String regionField = ClusterRecordKeys.cluster + "." + EcsClusterKeys.region;
  public static final String clusterNameField = ClusterRecordKeys.cluster + "." + EcsClusterKeys.clusterName;

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
  public void addRequiredQueryFilters(Query<ClusterRecord> query) {
    query.field(cloudProviderField)
        .equal(this.getCloudProviderId())
        .field(regionField)
        .equal(this.getRegion())
        .field(clusterNameField)
        .equal(this.getClusterName());
  }
}
