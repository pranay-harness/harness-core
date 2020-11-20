package io.harness.delegate.task.citasks;

/**
 * Delegate task to execute a command on CI build environment.
 */

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ci.ExecuteCommandTaskParams;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public class ExecuteCommandTask extends AbstractDelegateRunnableTask {
  @Inject private ExecuteCommandTaskHandler executeCommandTaskHandler;

  public ExecuteCommandTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }
  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    ExecuteCommandTaskParams executeCommandTaskParams = (ExecuteCommandTaskParams) parameters;
    return executeCommandTaskHandler.executeTaskInternal(executeCommandTaskParams);
  }
}
