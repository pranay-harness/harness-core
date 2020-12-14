package io.harness.engine.executables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

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
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.TaskMode;

import com.google.inject.Inject;
import com.google.inject.Injector;

@OwnedBy(CDC)
@Redesign
public class ExecutableProcessorFactory {
  @Inject Injector injector;

  public ExecutableProcessor obtainProcessor(ExecutionMode mode) {
    ExecuteStrategy executeStrategy;
    switch (mode) {
      case ASYNC:
        executeStrategy = new AsyncStrategy();
        break;
      case SYNC:
        executeStrategy = new SyncStrategy();
        break;
      case CHILDREN:
        executeStrategy = new ChildrenStrategy();
        break;
      case CHILD:
        executeStrategy = new ChildStrategy();
        break;
      case TASK:
        executeStrategy = new TaskStrategy(TaskMode.DELEGATE_TASK_V1);
        break;
      case TASK_V2:
        executeStrategy = new TaskStrategy(TaskMode.DELEGATE_TASK_V2);
        break;
      case TASK_V3:
        executeStrategy = new TaskStrategy(TaskMode.DELEGATE_TASK_V3);
        break;
      case TASK_CHAIN:
        executeStrategy = new TaskChainStrategy(TaskMode.DELEGATE_TASK_V1);
        break;
      case TASK_CHAIN_V2:
        executeStrategy = new TaskChainStrategy(TaskMode.DELEGATE_TASK_V2);
        break;
      case TASK_CHAIN_V3:
        executeStrategy = new TaskChainStrategy(TaskMode.DELEGATE_TASK_V3);
        break;
      case CHILD_CHAIN:
        executeStrategy = new ChildChainStrategy();
        break;
      default:
        throw new InvalidRequestException("No Invoker present for execution mode :" + mode);
    }
    injector.injectMembers(executeStrategy);
    return new ExecutableProcessor(executeStrategy);
  }
}
