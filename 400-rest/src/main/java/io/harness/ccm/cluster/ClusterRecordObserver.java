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

@OwnedBy(CE)
public interface ClusterRecordObserver {
  boolean onUpserted(ClusterRecord clusterRecord);
  void onDeleting(ClusterRecord clusterRecord);
  void onDeactivating(ClusterRecord clusterRecord);
}
