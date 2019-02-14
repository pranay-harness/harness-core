package software.wings.delegatetasks.k8s.taskhandler;

import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.utils.Misc;

public abstract class K8sTaskHandler {
  @Inject protected DelegateLogService delegateLogService;
  private static final Logger logger = LoggerFactory.getLogger(K8sTaskHandler.class);

  public K8sTaskExecutionResponse executeTask(
      K8sTaskParameters k8STaskParameters, K8sDelegateTaskParams k8SDelegateTaskParams) {
    try {
      return executeTaskInternal(k8STaskParameters, k8SDelegateTaskParams);
    } catch (Exception ex) {
      logger.error(format("Exception in processing K8s task [%s]",
                       k8STaskParameters.getCommandName() + ":" + k8STaskParameters.getCommandType()),
          ex);
      return K8sTaskExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(Misc.getMessage(ex))
          .build();
    }
  }

  protected abstract K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8STaskParameters, K8sDelegateTaskParams k8SDelegateTaskParams) throws Exception;
}
