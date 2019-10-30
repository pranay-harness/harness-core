package io.harness.ccm.cluster;

import io.harness.ccm.cluster.entities.ClusterRecord;

import java.util.List;

public interface ClusterRecordService {
  ClusterRecord upsert(ClusterRecord cluster);
  List<ClusterRecord> list(String accountId, String cloudProviderId);
  boolean delete(String accountId, String cloudProviderId);
  ClusterRecord attachPerpetualTaskId(ClusterRecord clusterRecord, String taskId);
  ClusterRecord removePerpetualTaskId(ClusterRecord clusterRecord, String taskId);
}
