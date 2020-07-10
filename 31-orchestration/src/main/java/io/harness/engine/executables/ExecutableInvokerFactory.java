package io.harness.engine.executables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;
import com.google.inject.Injector;

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
import io.harness.tasks.TaskMode;

@OwnedBy(CDC)
@Redesign
public class ExecutableInvokerFactory {
  @Inject Injector injector;

  public ExecutableInvoker obtainInvoker(ExecutionMode mode) {
    InvokeStrategy invokeStrategy;
    switch (mode) {
      case ASYNC:
        invokeStrategy = new AsyncStrategy();
        break;
      case SYNC:
        invokeStrategy = new SyncStrategy();
        break;
      case CHILDREN:
        invokeStrategy = new ChildrenStrategy();
        break;
      case CHILD:
        invokeStrategy = new ChildStrategy();
        break;
      case TASK:
        invokeStrategy = new TaskStrategy(TaskMode.DELEGATE_TASK_V1);
        break;
      case TASK_V2:
        invokeStrategy = new TaskStrategy(TaskMode.DELEGATE_TASK_V2);
        break;
      case TASK_CHAIN:
        invokeStrategy = new TaskChainStrategy(TaskMode.DELEGATE_TASK_V1);
        break;
      case TASK_CHAIN_V2:
        invokeStrategy = new TaskChainStrategy(TaskMode.DELEGATE_TASK_V2);
        break;
      case CHILD_CHAIN:
        invokeStrategy = new ChildChainStrategy();
        break;
      default:
        throw new InvalidRequestException("No Invoker present for execution mode :" + mode);
    }
    injector.injectMembers(invokeStrategy);
    return new ExecutableInvoker(invokeStrategy);
  }
}
