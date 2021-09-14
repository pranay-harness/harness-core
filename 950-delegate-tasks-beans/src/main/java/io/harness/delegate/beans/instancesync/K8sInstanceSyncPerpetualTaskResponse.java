/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.instancesync;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.logging.CommandExecutionStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
public class K8sInstanceSyncPerpetualTaskResponse implements InstanceSyncPerpetualTaskResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private List<ServerInstanceInfo> serverInstanceDetails;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}
