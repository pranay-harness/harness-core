package software.wings.delegatetasks.pcf;

import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskResponse;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfCommandTaskHandler;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
@Slf4j
public class PcfCommandTask extends AbstractDelegateRunnableTask {
  @Inject private Map<String, PcfCommandTaskHandler> commandTaskTypeToTaskHandlerMap;

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
