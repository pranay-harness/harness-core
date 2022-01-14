package io.harness.steps.executable;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildrenExecutableResponse;
import io.harness.pms.sdk.core.steps.executables.ChildrenExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.security.PmsSecurityContextEventGuard;

import lombok.SneakyThrows;

@OwnedBy(PIPELINE)
public interface ChildrenExecutableWithRbac<T extends StepParameters> extends ChildrenExecutable<T> {
  void validateResources(Ambiance ambiance, T stepParameters);

  @SneakyThrows
  default ChildrenExecutableResponse obtainChildren(
      Ambiance ambiance, T stepParameters, StepInputPackage inputPackage) {
    try (PmsSecurityContextEventGuard securityContextEventGuard = new PmsSecurityContextEventGuard(ambiance)) {
      validateResources(ambiance, stepParameters);
      return this.obtainChildrenAfterRbac(ambiance, stepParameters, inputPackage);
    }
  }

  ChildrenExecutableResponse obtainChildrenAfterRbac(
      Ambiance ambiance, T stepParameters, StepInputPackage inputPackage);
}
