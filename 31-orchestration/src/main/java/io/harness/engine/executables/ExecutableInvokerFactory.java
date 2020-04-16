package io.harness.engine.executables;

import com.google.inject.Inject;

import io.harness.annotations.Redesign;
import io.harness.engine.executables.handlers.AsyncExecutableInvoker;
import io.harness.engine.executables.handlers.ExecutableInvoker;
import io.harness.engine.executables.handlers.SyncExecutableInvoker;
import io.harness.exception.InvalidRequestException;
import io.harness.facilitate.modes.ExecutionMode;

@Redesign
public class ExecutableInvokerFactory {
  @Inject private AsyncExecutableInvoker asyncExecutableInvoker;
  @Inject private SyncExecutableInvoker syncExecutableInvoker;

  public ExecutableInvoker obtainInvoker(ExecutionMode mode) {
    switch (mode) {
      case ASYNC:
        return asyncExecutableInvoker;
      case SYNC:
        return syncExecutableInvoker;
      default:
        throw new InvalidRequestException("No Invoker present for execution mode :" + mode);
    }
  }
}
