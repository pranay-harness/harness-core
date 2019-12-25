package io.harness.ccm.cluster.entities;

import static io.harness.ccm.cluster.entities.ClusterType.AWS_ECS;
import static io.harness.ccm.cluster.entities.ClusterType.AZURE_KUBERNETES;
import static io.harness.ccm.cluster.entities.ClusterType.DIRECT_KUBERNETES;
import static io.harness.ccm.cluster.entities.ClusterType.GCP_KUBERNETES;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.mongodb.morphia.query.Query;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = EcsCluster.class, name = AWS_ECS)
  , @JsonSubTypes.Type(value = DirectKubernetesCluster.class, name = DIRECT_KUBERNETES),
      @JsonSubTypes.Type(value = GcpKubernetesCluster.class, name = GCP_KUBERNETES),
      @JsonSubTypes.Type(value = AzureKubernetesCluster.class, name = AZURE_KUBERNETES)
})
public interface Cluster {
  String getClusterType();
  String getCloudProviderId();
  void addRequiredQueryFilters(Query<ClusterRecord> query);
}
