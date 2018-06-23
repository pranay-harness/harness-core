package software.wings.delegatetasks.pcf;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfCommandTaskHandler;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PcfCommandTask extends AbstractDelegateRunnableTask {
  @Inject private Map<String, PcfCommandTaskHandler> commandTaskTypeToTaskHandlerMap;

  private static final Logger logger = LoggerFactory.getLogger(PcfCommandTask.class);

  public PcfCommandTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public PcfCommandExecutionResponse run(Object[] parameters) {
    PcfCommandRequest pcfCommandRequest = (PcfCommandRequest) parameters[0];
    try {
      return commandTaskTypeToTaskHandlerMap.get(pcfCommandRequest.getPcfCommandType().name())
          .executeTask(pcfCommandRequest, (List) parameters[1]);
    } catch (Exception ex) {
      logger.error("Exception in processing PCF task [{}]", pcfCommandRequest, ex);
      return PcfCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(Misc.getMessage(ex))
          .build();
    }
  }
}
