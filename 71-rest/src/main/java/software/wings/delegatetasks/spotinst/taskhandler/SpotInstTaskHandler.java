package software.wings.delegatetasks.spotinst.taskhandler;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.exception.ExceptionUtils.getMessage;
import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateLogService;

@Slf4j
public abstract class SpotInstTaskHandler {
  @Inject protected DelegateLogService delegateLogService;

  public SpotInstTaskExecutionResponse executeTask(SpotInstTaskParameters spotInstTaskParameters) {
    ExecutionLogCallback logCallback = new ExecutionLogCallback(delegateLogService,
        spotInstTaskParameters.getAccountId(), spotInstTaskParameters.getAppId(),
        spotInstTaskParameters.getActivityId(), spotInstTaskParameters.getCommandName());
    try {
      return executeTaskInternal(spotInstTaskParameters, logCallback);
    } catch (Exception ex) {
      String message = getMessage(ex);
      logCallback.saveExecutionLog(message);
      logger.error(format("Exception: [%s] while processing spotinst task: [%s]. Workflow execution id: [%s]", message,
                       spotInstTaskParameters.getCommandType().name(), spotInstTaskParameters.getWorkflowExecutionId()),
          ex);
      return SpotInstTaskExecutionResponse.builder().commandExecutionStatus(FAILURE).errorMessage(message).build();
    }
  }

  protected abstract SpotInstTaskExecutionResponse executeTaskInternal(
      SpotInstTaskParameters spotInstTaskParameters, ExecutionLogCallback logCallback) throws Exception;
}