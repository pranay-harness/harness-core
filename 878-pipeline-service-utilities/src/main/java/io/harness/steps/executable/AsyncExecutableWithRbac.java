package io.harness.steps.executable;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.security.PmsSecurityContextEventGuard;

@OwnedBy(PIPELINE)
public interface AsyncExecutableWithRbac<T extends StepParameters> extends AsyncExecutable<T> {
  void validateResources(Ambiance ambiance, T stepParameters);

  default AsyncExecutableResponse executeAsync(
      Ambiance ambiance, T stepParameters, StepInputPackage inputPackage, PassThroughData passThroughData) {
    try (PmsSecurityContextEventGuard securityContextEventGuard = new PmsSecurityContextEventGuard(ambiance)) {
      validateResources(ambiance, stepParameters);
      return this.executeAsyncAfterRbac(ambiance, stepParameters, inputPackage);
    } catch (Exception ex) {
      throw new UnexpectedException("Failed to execute Async task", ex);
    }
  }

  AsyncExecutableResponse executeAsyncAfterRbac(Ambiance ambiance, T stepParameters, StepInputPackage inputPackage);
}
