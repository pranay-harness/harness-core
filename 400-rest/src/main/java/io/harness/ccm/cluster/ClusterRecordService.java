/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.cluster;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.cluster.entities.ClusterRecord;

import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.infra.InfrastructureDefinition;

import java.util.List;

@OwnedBy(CE)
public interface ClusterRecordService {
  ClusterRecord upsert(ClusterRecord cluster);
  ClusterRecord get(String clusterId);
  List<ClusterRecord> list(String accountId, String clusterType);
  List<ClusterRecord> list(String accountId, String clusterType, String cloudProviderId);
  List<ClusterRecord> list(String accountId, String cloudProviderId, boolean isDeactivated);
  List<ClusterRecord> list(
      String accountId, String cloudProviderId, boolean isDeactivated, Integer count, Integer startIndex);
  boolean delete(String accountId, String cloudProviderId);
  ClusterRecord deactivate(String accountId, String cloudProviderId);
  ClusterRecord attachPerpetualTaskId(ClusterRecord clusterRecord, String taskId);
  ClusterRecord removePerpetualTaskId(ClusterRecord clusterRecord, String taskId);

  ClusterRecord from(SettingAttribute cloudProvider);
  ClusterRecord from(InfrastructureDefinition infrastructureDefinition);
  ClusterRecord from(InfrastructureMapping infrastructureMapping);
  List<ClusterRecord> listCeEnabledClusters(String accountId);
}
