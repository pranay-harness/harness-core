package io.harness.ccm.cluster;

import io.harness.ccm.cluster.entities.ClusterRecord;

public interface ClusterRecordService {
  ClusterRecord upsert(ClusterRecord cluster);
  boolean delete(String accountId, String cloudProviderId);
}
