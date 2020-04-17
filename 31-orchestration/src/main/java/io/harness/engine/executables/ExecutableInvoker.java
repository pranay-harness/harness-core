package io.harness.engine.executables;

import io.harness.annotations.Redesign;

@Redesign
public interface ExecutableInvoker {
  void invokeExecutable(InvokerPackage invokerPackage);
}
