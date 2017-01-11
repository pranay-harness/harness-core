package software.wings.delegatetasks;

import static java.util.stream.Collectors.toList;
import static software.wings.beans.command.AbstractCommandUnit.ExecutionResult.ExecutionResultData.Builder.anExecutionResultData;

import com.google.common.base.Joiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.command.AbstractCommandUnit.ExecutionResult;
import software.wings.beans.command.AbstractCommandUnit.ExecutionResult.ExecutionResultData;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.common.cache.ResponseCodeCache;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ServiceCommandExecutorService;

import java.util.function.Consumer;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 1/6/17.
 */
public class CommandTask extends AbstractDelegateRunnableTask<ExecutionResultData> {
  private static final Logger logger = LoggerFactory.getLogger(CommandTask.class);

  @Inject private ServiceCommandExecutorService serviceCommandExecutorService;
  public CommandTask(String delegateId, DelegateTask delegateTask, Consumer<ExecutionResultData> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  public ExecutionResultData run(Object[] parameters) {
    return run((Command) parameters[0], (CommandExecutionContext) parameters[1]);
  }

  private ExecutionResultData run(Command command, CommandExecutionContext commandExecutionContext) {
    ExecutionResult executionResult = ExecutionResult.SUCCESS;
    String errorMessage = null;
    try {
      executionResult = serviceCommandExecutorService.execute(command, commandExecutionContext);
    } catch (Exception e) {
      logger.warn("Exception: ", e);
      if (e instanceof WingsException) {
        WingsException ex = (WingsException) e;
        errorMessage = Joiner.on(",").join(ex.getResponseMessageList()
                                               .stream()
                                               .map(responseMessage
                                                   -> ResponseCodeCache.getInstance()
                                                          .getResponseMessage(responseMessage.getCode(), ex.getParams())
                                                          .getMessage())
                                               .collect(toList()));
      } else {
        errorMessage = e.getMessage();
      }
      executionResult = ExecutionResult.FAILURE;
    }

    return anExecutionResultData().withResult(executionResult).withErrorMessage(errorMessage).build();
  }
}
