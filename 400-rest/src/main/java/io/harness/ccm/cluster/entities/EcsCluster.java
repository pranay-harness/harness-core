/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.cluster.entities;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.cluster.entities.ClusterType.AWS_ECS;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.cluster.entities.ClusterRecord.ClusterRecordKeys;

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
@OwnedBy(CE)
@TargetModule(HarnessModule._490_CE_COMMONS)
public class EcsCluster implements Cluster {
  String cloudProviderId;
  String region;
  String clusterName;

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
