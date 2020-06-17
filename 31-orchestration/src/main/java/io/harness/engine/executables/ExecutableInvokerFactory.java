package io.harness.engine.executables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executables.invokers.AsyncExecutableInvoker;
import io.harness.engine.executables.invokers.ChildChainExecutableInvoker;
import io.harness.engine.executables.invokers.ChildExecutableInvoker;
import io.harness.engine.executables.invokers.ChildrenExecutableInvoker;
import io.harness.engine.executables.invokers.SyncExecutableInvoker;
import io.harness.engine.executables.invokers.TaskChainExecutableInvoker;
import io.harness.engine.executables.invokers.TaskExecutableInvoker;
import io.harness.exception.InvalidRequestException;
import io.harness.facilitator.modes.ExecutionMode;

@OwnedBy(CDC)
@Redesign
public class ExecutableInvokerFactory {
  @Inject private AsyncExecutableInvoker asyncExecutableInvoker;
  @Inject private SyncExecutableInvoker syncExecutableInvoker;
  @Inject private ChildrenExecutableInvoker childrenExecutableInvoker;
  @Inject private ChildExecutableInvoker childExecutableInvoker;
  @Inject private TaskExecutableInvoker taskExecutableInvoker;
  @Inject private TaskChainExecutableInvoker taskChainExecutableInvoker;
  @Inject private ChildChainExecutableInvoker childChainExecutableInvoker;

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
      case TASK:
        return taskExecutableInvoker;
      case TASK_CHAIN:
        return taskChainExecutableInvoker;
      case CHILD_CHAIN:
        return childChainExecutableInvoker;
      default:
        throw new InvalidRequestException("No Invoker present for execution mode :" + mode);
    }
  }
}
