package io.harness.ccm.cluster;

import io.harness.ccm.cluster.entities.ClusterRecord;

public interface ClusterRecordObserver {
  void onUpserted(ClusterRecord clusterRecord);
  void onDeleted(String accountId, String cloudProviderId);
}
