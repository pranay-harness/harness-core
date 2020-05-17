package software.wings.delegatetasks;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.service.intfc.ServiceCommandExecutorService;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class CommandTask extends AbstractDelegateRunnableTask {
  @Inject private ServiceCommandExecutorService serviceCommandExecutorService;

  public CommandTask(DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateTaskPackage, postExecute, preExecute);
  }

  @Override
  public CommandExecutionResult run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public CommandExecutionResult run(Object[] parameters) {
    return run((Command) parameters[0], (CommandExecutionContext) parameters[1]);
  }

  private CommandExecutionResult run(Command command, CommandExecutionContext commandExecutionContext) {
    CommandExecutionStatus commandExecutionStatus;
    String errorMessage = null;

    try {
      commandExecutionStatus = serviceCommandExecutorService.execute(command, commandExecutionContext);
    } catch (Exception e) {
      logger.warn("Exception while executing task {}: {}", getTaskId(), ExceptionUtils.getMessage(e), e);
      errorMessage = ExceptionUtils.getMessage(e);
      commandExecutionStatus = CommandExecutionStatus.FAILURE;
    }

    return CommandExecutionResult.builder()
        .status(commandExecutionStatus)
        .errorMessage(errorMessage)
        .commandExecutionData(commandExecutionContext.getCommandExecutionData())
        .build();
  }
}
