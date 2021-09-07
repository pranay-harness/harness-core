package io.harness.pms.sdk.core.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.Step;

@OwnedBy(HarnessTeam.PIPELINE)
public interface ExecuteStrategy {
  void start(InvokerPackage invokerPackage);

  default void resume(ResumePackage resumePackage) {
    throw new UnsupportedOperationException();
  }

  <T extends Step> T extractStep(Ambiance ambiance);

  default void progress(ProgressPackage progressPackage) {
    throw new UnsupportedOperationException();
  };
}
