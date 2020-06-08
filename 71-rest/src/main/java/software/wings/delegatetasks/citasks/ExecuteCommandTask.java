package software.wings.delegatetasks.citasks;

/**
 * Delegate task to execute a command on CI build environment.
 */

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.task.TaskParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.ci.ExecuteCommandTaskParams;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public class ExecuteCommandTask extends AbstractDelegateRunnableTask {
  @Inject private ExecuteCommandTaskHandler executeCommandTaskHandler;

  public ExecuteCommandTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }
  @Override
  public ResponseData run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }

  @Override
  public ResponseData run(TaskParameters parameters) {
    ExecuteCommandTaskParams executeCommandTaskParams = (ExecuteCommandTaskParams) parameters;
    return executeCommandTaskHandler.executeTaskInternal(executeCommandTaskParams);
  }
}