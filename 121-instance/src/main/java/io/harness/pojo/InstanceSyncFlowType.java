package io.harness.pojo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DX)
public enum InstanceSyncFlowType {
  NEW_DEPLOYMENT,
  PERPETUAL_TASK,
  ITERATOR,
  MANUAL;
}
