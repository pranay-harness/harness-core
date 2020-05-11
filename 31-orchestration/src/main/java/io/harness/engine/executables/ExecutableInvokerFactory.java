package io.harness.engine.executables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executables.handlers.AsyncExecutableInvoker;
import io.harness.engine.executables.handlers.ChildExecutableInvoker;
import io.harness.engine.executables.handlers.ChildrenExecutableInvoker;
import io.harness.engine.executables.handlers.SyncExecutableInvoker;
import io.harness.exception.InvalidRequestException;
import io.harness.facilitator.modes.ExecutionMode;

@OwnedBy(CDC)
@Redesign
public class ExecutableInvokerFactory {
  @Inject private AsyncExecutableInvoker asyncExecutableInvoker;
  @Inject private SyncExecutableInvoker syncExecutableInvoker;
  @Inject private ChildrenExecutableInvoker childrenExecutableInvoker;
  @Inject private ChildExecutableInvoker childExecutableInvoker;

  public ExecutableInvoker obtainInvoker(ExecutionMode mode) {
    switch (mode) {
      case ASYNC:
        return asyncExecutableInvoker;
      case SYNC:
        return syncExecutableInvoker;
      case CHILDREN:
        return childrenExecutableInvoker;
      case CHILD:
        return childExecutableInvoker;
      default:
        throw new InvalidRequestException("No Invoker present for execution mode :" + mode);
    }
  }
}
