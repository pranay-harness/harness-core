package io.harness.engine.status;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
public interface StepStatusUpdate {
  void onStepStatusUpdate(@NotNull StepStatusUpdateInfo stepStatusUpdateInfo);
}
