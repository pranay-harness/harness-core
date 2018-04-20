package software.wings.delegatetasks;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.exception.HarnessException;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.helm.HelmCommandExecutionResponse;
import software.wings.helpers.ext.helm.HelmDeployService;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmReleaseHistoryCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.helpers.ext.helm.response.HelmCommandResponse;
import software.wings.waitnotify.NotifyResponseData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by anubhaw on 3/22/18.
 */
public class HelmCommandTask extends AbstractDelegateRunnableTask {
  @Inject private DelegateLogService delegateLogService;
  @Inject private HelmDeployService helmDeployService;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;

  private static final Logger logger = LoggerFactory.getLogger(HelmCommandTask.class);

  public HelmCommandTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public HelmCommandExecutionResponse run(Object[] parameters) {
    HelmCommandRequest helmCommandRequest = (HelmCommandRequest) parameters[0];
    HelmCommandResponse commandResponse;

    ExecutionLogCallback executionLogCallback =
        new ExecutionLogCallback(delegateLogService, helmCommandRequest.getAccountId(), helmCommandRequest.getAppId(),
            helmCommandRequest.getActivityId(), helmCommandRequest.getCommandName());

    try {
      String configLocation = containerDeploymentDelegateHelper.createAndGetKubeConfigLocation(
          helmCommandRequest.getContainerServiceParams());
      helmCommandRequest.setKubeConfigLocation(configLocation);
      HelmCommandResponse helmCommandResponse = helmDeployService.ensureHelmCliAndTillerInstalled(helmCommandRequest);

      if (isAsync()) {
        executionLogCallback.saveExecutionLog(helmCommandResponse.getOutput());
        executionLogCallback.saveExecutionLog(
            "Started executing helm command", LogLevel.INFO, CommandExecutionStatus.RUNNING);
      }
      switch (helmCommandRequest.getHelmCommandType()) {
        case INSTALL:
          commandResponse =
              helmDeployService.deploy((HelmInstallCommandRequest) helmCommandRequest, executionLogCallback);
          break;
        case ROLLBACK:
          commandResponse =
              helmDeployService.rollback((HelmRollbackCommandRequest) helmCommandRequest, executionLogCallback);
          break;
        case RELEASE_HISTORY:
          commandResponse = helmDeployService.releaseHistory((HelmReleaseHistoryCommandRequest) helmCommandRequest);
          break;
        default:
          throw new HarnessException("Operation not supported");
      }
    } catch (Exception ex) {
      logger.error("Exception in processing helm task [{}]", helmCommandRequest, ex);
      return HelmCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ex.getMessage())
          .build();
    }

    if (isAsync()) {
      executionLogCallback.saveExecutionLog(
          "Command finished with status " + commandResponse.getCommandExecutionStatus(), LogLevel.INFO,
          commandResponse.getCommandExecutionStatus());
    }

    return HelmCommandExecutionResponse.builder()
        .commandExecutionStatus(commandResponse.getCommandExecutionStatus())
        .helmCommandResponse(commandResponse)
        .errorMessage(commandResponse.getOutput())
        .build();
  }
}
