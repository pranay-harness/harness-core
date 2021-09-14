/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.instancesync;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import java.util.List;

@OwnedBy(HarnessTeam.DX)
public interface InstanceSyncPerpetualTaskResponse extends DelegateTaskNotifyResponseData {
  List<ServerInstanceInfo> getServerInstanceDetails();
}
