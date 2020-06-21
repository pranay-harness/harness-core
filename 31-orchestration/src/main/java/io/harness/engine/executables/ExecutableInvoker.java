package io.harness.engine.executables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import lombok.AllArgsConstructor;

@OwnedBy(CDC)
@Redesign
@AllArgsConstructor
public class ExecutableInvoker {
  InvokeStrategy invokeStrategy;

  public void invokeExecutable(InvokerPackage invokerPackage) {
    invokeStrategy.invoke(invokerPackage);
  }
}
