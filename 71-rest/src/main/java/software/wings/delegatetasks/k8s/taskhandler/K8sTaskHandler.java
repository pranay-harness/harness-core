package software.wings.delegatetasks.k8s.taskhandler;

import com.google.inject.Inject;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
@Slf4j
public abstract class K8sTaskHandler {
  @Inject protected DelegateLogService delegateLogService;

  public K8sTaskExecutionResponse executeTask(
      K8sTaskParameters k8STaskParameters, K8sDelegateTaskParams k8SDelegateTaskParams) {
    try {
      return executeTaskInternal(k8STaskParameters, k8SDelegateTaskParams);
    } catch (Exception ex) {
      logger.error("Exception in processing K8s task [{}]",
          k8STaskParameters.getCommandName() + ":" + k8STaskParameters.getCommandType(), ex);
      return K8sTaskExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(ex))
          .build();
    }
  }

  protected abstract K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8STaskParameters, K8sDelegateTaskParams k8SDelegateTaskParams) throws Exception;
}
