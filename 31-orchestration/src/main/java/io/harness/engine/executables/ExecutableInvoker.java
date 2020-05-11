package io.harness.engine.executables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
@Redesign
public interface ExecutableInvoker {
  void invokeExecutable(InvokerPackage invokerPackage);
}
