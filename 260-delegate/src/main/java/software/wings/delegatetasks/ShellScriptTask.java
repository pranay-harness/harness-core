package software.wings.delegatetasks;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;

import software.wings.beans.delegation.ShellScriptParameters;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class ShellScriptTask extends AbstractDelegateRunnableTask {
  @Inject private ShellScriptTaskHandler shellScriptTaskHandler;

  public ShellScriptTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> postExecute, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, postExecute, preExecute);
  }

  @Override
  public CommandExecutionResult run(TaskParameters parameters) {
    return shellScriptTaskHandler.handle((ShellScriptParameters) parameters);
  }

  @Override
  public CommandExecutionResult run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }
}
