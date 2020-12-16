package io.harness.engine.executables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.sdk.core.execution.ExecuteStrategy;
import io.harness.pms.sdk.core.execution.invokers.AsyncStrategy;
import io.harness.pms.sdk.core.execution.invokers.ChildChainStrategy;
import io.harness.pms.sdk.core.execution.invokers.ChildStrategy;
import io.harness.pms.sdk.core.execution.invokers.ChildrenStrategy;
import io.harness.pms.sdk.core.execution.invokers.SyncStrategy;
import io.harness.pms.sdk.core.execution.invokers.TaskChainStrategy;
import io.harness.pms.sdk.core.execution.invokers.TaskStrategy;

import com.google.inject.Inject;
import com.google.inject.Injector;

@OwnedBy(CDC)
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
        executeStrategy = new TaskStrategy();
        break;
      case TASK_CHAIN:
        executeStrategy = new TaskChainStrategy();
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
