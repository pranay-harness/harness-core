package software.wings.delegatetasks;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.task.TaskParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.delegation.ShellScriptParameters;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class ShellScriptTask extends AbstractDelegateRunnableTask {
  @Inject private ShellScriptTaskHandler shellScriptTaskHandler;

  public ShellScriptTask(DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateTaskPackage, postExecute, preExecute);
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
