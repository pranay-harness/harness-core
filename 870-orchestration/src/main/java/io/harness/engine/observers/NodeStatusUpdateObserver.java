package io.harness.engine.observers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;

@OwnedBy(PIPELINE)
public interface NodeStatusUpdateObserver {
  void onNodeStatusUpdate(@NotNull NodeUpdateInfo nodeUpdateInfo);
}
