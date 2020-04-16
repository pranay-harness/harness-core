package io.harness.engine.executables.handlers;

import io.harness.annotations.Redesign;
import io.harness.engine.executables.InvokerPackage;

@Redesign
public interface ExecutableInvoker {
  void invokeExecutable(InvokerPackage invokerPackage);
}
