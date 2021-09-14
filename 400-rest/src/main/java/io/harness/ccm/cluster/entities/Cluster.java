/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.cluster.entities;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.cluster.entities.ClusterType.AWS_ECS;
import static io.harness.ccm.cluster.entities.ClusterType.AZURE_KUBERNETES;
import static io.harness.ccm.cluster.entities.ClusterType.DIRECT_KUBERNETES;
import static io.harness.ccm.cluster.entities.ClusterType.GCP_KUBERNETES;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

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
@OwnedBy(CE)
@TargetModule(HarnessModule._490_CE_COMMONS)
public interface Cluster {
  String getClusterType();
  String getCloudProviderId();
  String getClusterName();
  void addRequiredQueryFilters(Query<ClusterRecord> query);
}
