package software.wings.delegatetasks;

import static java.util.stream.Collectors.toList;
import static software.wings.beans.command.CommandExecutionResult.Builder.aCommandExecutionResult;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.common.cache.ResponseCodeCache;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ServiceCommandExecutorService;
import software.wings.waitnotify.NotifyResponseData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by peeyushaggarwal on 1/6/17.
 */
public class CommandTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(CommandTask.class);

  @Inject private ServiceCommandExecutorService serviceCommandExecutorService;

  public CommandTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
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
      logger.warn("Exception while executing task {}: {}", getTaskId(), e.getMessage(), e);
      if (e instanceof WingsException) {
        WingsException ex = (WingsException) e;
        errorMessage = Joiner.on(",").join(
            ex.getResponseMessageList()
                .stream()
                .map(responseMessage -> ResponseCodeCache.getInstance().rebuildMessage(responseMessage, ex.getParams()))
                .collect(toList()));
      } else {
        errorMessage = e.getMessage();
      }
      commandExecutionStatus = CommandExecutionStatus.FAILURE;
    }

    return aCommandExecutionResult()
        .withStatus(commandExecutionStatus)
        .withErrorMessage(errorMessage)
        .withCommandExecutionData(commandExecutionContext.getCommandExecutionData())
        .build();
  }
}
