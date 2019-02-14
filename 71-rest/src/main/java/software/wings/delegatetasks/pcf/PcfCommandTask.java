package software.wings.delegatetasks.pcf;

import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.protocol.TaskParameters;
import io.harness.exception.ExceptionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfCommandTaskHandler;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PcfCommandTask extends AbstractDelegateRunnableTask {
  @Inject private Map<String, PcfCommandTaskHandler> commandTaskTypeToTaskHandlerMap;

  private static final Logger logger = LoggerFactory.getLogger(PcfCommandTask.class);

  public PcfCommandTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public PcfCommandExecutionResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public PcfCommandExecutionResponse run(Object[] parameters) {
    PcfCommandRequest pcfCommandRequest = (PcfCommandRequest) parameters[0];
    try {
      return commandTaskTypeToTaskHandlerMap.get(pcfCommandRequest.getPcfCommandType().name())
          .executeTask(pcfCommandRequest, (List) parameters[1]);
    } catch (Exception ex) {
      logger.error(format("Exception in processing PCF task [%s]", pcfCommandRequest.toString()), ex);
      return PcfCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(ex))
          .build();
    }
  }
}
