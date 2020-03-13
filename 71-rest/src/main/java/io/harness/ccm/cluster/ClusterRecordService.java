package io.harness.ccm.cluster;

import io.harness.ccm.cluster.entities.ClusterRecord;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.infra.InfrastructureDefinition;

import java.util.List;

public interface ClusterRecordService {
  ClusterRecord upsert(ClusterRecord cluster);
  ClusterRecord get(String clusterId);
  List<ClusterRecord> list(String accountId, String cloudProviderId);
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
}
