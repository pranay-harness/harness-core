package io.harness.engine.status;

import javax.validation.constraints.NotNull;

public interface StepStatusUpdate { void onStepStatusUpdate(@NotNull StepStatusUpdateInfo stepStatusUpdateInfo); }
