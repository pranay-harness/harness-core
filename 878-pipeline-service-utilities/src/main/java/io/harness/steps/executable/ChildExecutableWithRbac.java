package io.harness.steps.executable;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.pms.security.PmsSecurityContextEventGuard;

import lombok.SneakyThrows;

/**
 * Use this interface when you want spawn a child
 *
 * This Node will spawn child and the response is passed to handleChildResponse as {@link
 * StepResponseNotifyData}
 *
 */

@OwnedBy(CDC)
public interface ChildExecutableWithRbac<T extends StepParameters> extends ChildExecutable<T> {
  void validateResources(Ambiance ambiance, T stepParameters);

  @SneakyThrows
  default ChildExecutableResponse obtainChild(Ambiance ambiance, T stepParameters, StepInputPackage inputPackage) {
    try (PmsSecurityContextEventGuard securityContextEventGuard = new PmsSecurityContextEventGuard(ambiance)) {
      validateResources(ambiance, stepParameters);
      return this.obtainChildAfterRbac(ambiance, stepParameters, inputPackage);
    }
  }

  ChildExecutableResponse obtainChildAfterRbac(Ambiance ambiance, T stepParameters, StepInputPackage inputPackage);
}