package software.wings.delegatetasks.spotinst.taskhandler;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.exception.ExceptionUtils.getMessage;
import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.spotinst.SpotInstHelperServiceDelegate;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.aws.delegate.AwsElbHelperServiceDelegate;

@Slf4j
public abstract class SpotInstTaskHandler {
  @Inject protected DelegateLogService delegateLogService;
  @Inject protected SpotInstHelperServiceDelegate spotInstHelperServiceDelegate;
  @Inject protected AwsElbHelperServiceDelegate awsElbHelperServiceDelegate;

  public SpotInstTaskExecutionResponse executeTask(
      SpotInstTaskParameters spotInstTaskParameters, SpotInstConfig spotInstConfig, AwsConfig awsConfig) {
    ExecutionLogCallback logCallback = new ExecutionLogCallback(delegateLogService,
        spotInstTaskParameters.getAccountId(), spotInstTaskParameters.getAppId(),
        spotInstTaskParameters.getActivityId(), spotInstTaskParameters.getCommandName());
    try {
      return executeTaskInternal(spotInstTaskParameters, logCallback, spotInstConfig, awsConfig);
    } catch (Exception ex) {
      String message = getMessage(ex);
      logCallback.saveExecutionLog(message);
      logger.error(format("Exception: [%s] while processing spotinst task: [%s]. Workflow execution id: [%s]", message,
                       spotInstTaskParameters.getCommandType().name(), spotInstTaskParameters.getWorkflowExecutionId()),
          ex);
      return SpotInstTaskExecutionResponse.builder().commandExecutionStatus(FAILURE).errorMessage(message).build();
    }
  }

  protected abstract SpotInstTaskExecutionResponse executeTaskInternal(SpotInstTaskParameters spotInstTaskParameters,
      ExecutionLogCallback logCallback, SpotInstConfig spotInstConfig, AwsConfig awsConfig) throws Exception;
}