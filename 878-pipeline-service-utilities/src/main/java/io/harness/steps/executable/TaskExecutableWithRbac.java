/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps.executable;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.security.PmsSecurityContextEventGuard;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import lombok.SneakyThrows;

@OwnedBy(PIPELINE)
public interface TaskExecutableWithRbac<T extends StepParameters, R extends ResponseData> extends TaskExecutable<T, R> {
  void validateResources(Ambiance ambiance, T stepParameters);

  @Override
  @SneakyThrows
  default TaskRequest obtainTask(Ambiance ambiance, T stepParameters, StepInputPackage inputPackage) {
    try (PmsSecurityContextEventGuard securityContextEventGuard = new PmsSecurityContextEventGuard(ambiance)) {
      validateResources(ambiance, stepParameters);
      return this.obtainTaskAfterRbac(ambiance, stepParameters, inputPackage);
    }
  }

  default StepResponse handleTaskResult(Ambiance ambiance, T stepParameters, ThrowingSupplier<R> responseDataSupplier)
      throws Exception {
    try (PmsSecurityContextEventGuard securityContextEventGuard = new PmsSecurityContextEventGuard(ambiance)) {
      return handleTaskResultWithSecurityContext(ambiance, stepParameters, responseDataSupplier);
    }
  }

  StepResponse handleTaskResultWithSecurityContext(
      Ambiance ambiance, T stepParameters, ThrowingSupplier<R> responseDataSupplier) throws Exception;

  TaskRequest obtainTaskAfterRbac(Ambiance ambiance, T stepParameters, StepInputPackage inputPackage);
}
