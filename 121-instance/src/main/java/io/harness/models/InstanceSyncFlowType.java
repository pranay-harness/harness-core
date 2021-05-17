package io.harness.models;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DX)
public enum InstanceSyncFlowType {
  NEW_DEPLOYMENT,
  PERPETUAL_TASK,
  MANUAL;
}
