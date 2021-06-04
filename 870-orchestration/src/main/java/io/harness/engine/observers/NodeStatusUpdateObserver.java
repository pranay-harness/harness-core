package io.harness.engine.observers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
public interface NodeStatusUpdateObserver {
  void onNodeStatusUpdate(@NotNull NodeUpdateInfo nodeUpdateInfo);
}
