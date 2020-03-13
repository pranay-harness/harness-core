package io.harness.ccm.cluster;

import io.harness.ccm.cluster.entities.ClusterRecord;

public interface ClusterRecordObserver {
  boolean onUpserted(ClusterRecord clusterRecord);
  void onDeleting(ClusterRecord clusterRecord);
  void onDeactivating(ClusterRecord clusterRecord);
}
