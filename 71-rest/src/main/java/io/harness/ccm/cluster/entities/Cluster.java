package io.harness.ccm.cluster.entities;

import static io.harness.ccm.cluster.entities.ClusterType.AWS_ECS;
import static io.harness.ccm.cluster.entities.ClusterType.DIRECT_KUBERNETES;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.mongodb.morphia.query.Query;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = DirectKubernetesCluster.class, name = DIRECT_KUBERNETES)
  , @JsonSubTypes.Type(value = EcsCluster.class, name = AWS_ECS)
})
public interface Cluster {
  String getClusterType();
  String getCloudProviderId();
  Query addRequiredQueryFilters(Query<ClusterRecord> query);
}
