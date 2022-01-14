package io.harness.pms.sdk.core;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.execution.ExecuteStrategy;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.ProgressPackage;
import io.harness.pms.sdk.core.steps.Step;

@OwnedBy(HarnessTeam.PIPELINE)
public class DummyExecutionStrategy implements ExecuteStrategy {
  public DummyExecutionStrategy() {}

  @Override
  public void start(InvokerPackage invokerPackage) {
    // do Nothing
  }

  @Override
  public <T extends Step> T extractStep(Ambiance ambiance) {
    return null;
  }

  @Override
  public void progress(ProgressPackage progressPackage) {
    // do Nothing
  }
}
