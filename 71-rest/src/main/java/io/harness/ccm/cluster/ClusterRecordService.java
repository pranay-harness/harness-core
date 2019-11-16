package io.harness.ccm.cluster;

import io.harness.ccm.cluster.entities.ClusterRecord;

import java.util.List;

public interface ClusterRecordService {
  ClusterRecord upsert(ClusterRecord cluster);
  ClusterRecord get(String clusterId);
  List<ClusterRecord> list(String accountId, String cloudProviderId);
  List<ClusterRecord> list(String accountId, String cloudProviderId, Integer count, Integer startIndex);
  boolean delete(String accountId, String cloudProviderId);
  ClusterRecord attachPerpetualTaskId(ClusterRecord clusterRecord, String taskId);
  ClusterRecord removePerpetualTaskId(ClusterRecord clusterRecord, String taskId);
}
