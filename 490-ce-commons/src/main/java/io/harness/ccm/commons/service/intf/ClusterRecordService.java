/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.commons.service.intf;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.ClusterRecord;

@OwnedBy(CE)
public interface ClusterRecordService {
  ClusterRecord upsert(ClusterRecord clusterRecord);
  ClusterRecord get(String uuid);
  ClusterRecord get(String accountId, String k8sBaseConnectorRefIdentifier);
  boolean delete(String accountId, String ceK8sConnectorIdentifier);
  ClusterRecord getByCEK8sIdentifier(String accountId, String ceK8sConnectorIdentifier);
  ClusterRecord attachTask(ClusterRecord clusterRecord, String taskId);
}
