package io.harness.engine.executables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executables.invokers.AsyncStrategy;
import io.harness.engine.executables.invokers.ChildChainStrategy;
import io.harness.engine.executables.invokers.ChildStrategy;
import io.harness.engine.executables.invokers.ChildrenStrategy;
import io.harness.engine.executables.invokers.SyncStrategy;
import io.harness.engine.executables.invokers.TaskChainStrategy;
import io.harness.engine.executables.invokers.TaskStrategy;
import io.harness.exception.InvalidRequestException;
import io.harness.facilitator.modes.ExecutionMode;

@OwnedBy(CDC)
@Redesign
public class ExecutableInvokerFactory {
  @Inject private AsyncStrategy asyncStrategy;
  @Inject private SyncStrategy syncStrategy;
  @Inject private ChildrenStrategy childrenStrategy;
  @Inject private ChildStrategy childStrategy;
  @Inject private TaskStrategy taskStrategy;
  @Inject private TaskChainStrategy taskChainStrategy;
  @Inject private ChildChainStrategy childChainStrategy;

  public ExecutableInvoker obtainInvoker(ExecutionMode mode) {
    switch (mode) {
      case ASYNC:
        return new ExecutableInvoker(asyncStrategy);
      case SYNC:
        return new ExecutableInvoker(syncStrategy);
      case CHILDREN:
        return new ExecutableInvoker(childrenStrategy);
      case CHILD:
        return new ExecutableInvoker(childStrategy);
      case TASK:
        return new ExecutableInvoker(taskStrategy);
      case TASK_CHAIN:
        return new ExecutableInvoker(taskChainStrategy);
      case CHILD_CHAIN:
        return new ExecutableInvoker(childChainStrategy);
      default:
        throw new InvalidRequestException("No Invoker present for execution mode :" + mode);
    }
  }
}
