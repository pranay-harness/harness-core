package software.wings.delegatetasks.k8s.taskhandler;

import static java.lang.String.format;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.k8s.K8sCommandTaskParams;
import software.wings.helpers.ext.k8s.request.K8sCommandRequest;
import software.wings.helpers.ext.k8s.response.K8sCommandExecutionResponse;
import software.wings.utils.Misc;

public abstract class K8sCommandTaskHandler {
  @Inject protected DelegateLogService delegateLogService;
  private static final Logger logger = LoggerFactory.getLogger(K8sCommandTaskHandler.class);

  protected ExecutionLogCallback executionLogCallback;

  public K8sCommandExecutionResponse executeTask(
      K8sCommandRequest k8sCommandRequest, K8sCommandTaskParams k8sCommandTaskParams) {
    executionLogCallback = new ExecutionLogCallback(delegateLogService, k8sCommandRequest.getAccountId(),
        k8sCommandRequest.getAppId(), k8sCommandRequest.getActivityId(), k8sCommandRequest.getCommandName());

    try {
      return executeTaskInternal(k8sCommandRequest, k8sCommandTaskParams);
    } catch (Exception ex) {
      logger.error(format("Exception in processing K8s task [%s]",
                       k8sCommandRequest.getCommandName() + ":" + k8sCommandRequest.getCommandType()),
          ex);
      return K8sCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(Misc.getMessage(ex))
          .build();
    }
  }

  protected abstract K8sCommandExecutionResponse executeTaskInternal(
      K8sCommandRequest k8sCommandRequest, K8sCommandTaskParams k8sCommandTaskParams) throws Exception;
}
