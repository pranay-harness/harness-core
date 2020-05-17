package software.wings.delegatetasks.helm;

import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.HarnessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.command.HelmDummyCommandUnit;
import software.wings.beans.command.LogCallback;
import software.wings.beans.command.NoopExecutionCallback;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.helm.HelmCommandExecutionResponse;
import software.wings.helpers.ext.helm.HelmDeployService;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmReleaseHistoryCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.helpers.ext.helm.response.HelmCommandResponse;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by anubhaw on 3/22/18.
 */
@Slf4j
public class HelmCommandTask extends AbstractDelegateRunnableTask {
  @Inject private DelegateLogService delegateLogService;
  @Inject private HelmDeployService helmDeployService;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private HelmCommandHelper helmCommandHelper;

  public HelmCommandTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public HelmCommandExecutionResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public HelmCommandExecutionResponse run(Object[] parameters) {
    HelmCommandRequest helmCommandRequest = (HelmCommandRequest) parameters[0];
    HelmCommandResponse commandResponse;

    try {
      init(helmCommandRequest, getExecutionLogCallback(helmCommandRequest, HelmDummyCommandUnit.Init));

      helmCommandRequest.setExecutionLogCallback(
          getExecutionLogCallback(helmCommandRequest, HelmDummyCommandUnit.Prepare));

      helmCommandRequest.getExecutionLogCallback().saveExecutionLog(
          helmCommandHelper.getDeploymentMessage(helmCommandRequest), LogLevel.INFO, CommandExecutionStatus.RUNNING);

      switch (helmCommandRequest.getHelmCommandType()) {
        case INSTALL:
          commandResponse = helmDeployService.deploy((HelmInstallCommandRequest) helmCommandRequest);
          break;
        case ROLLBACK:
          commandResponse = helmDeployService.rollback((HelmRollbackCommandRequest) helmCommandRequest);
          break;
        case RELEASE_HISTORY:
          commandResponse = helmDeployService.releaseHistory((HelmReleaseHistoryCommandRequest) helmCommandRequest);
          break;
        default:
          throw new HarnessException("Operation not supported");
      }
    } catch (Exception ex) {
      String errorMsg = ExceptionUtils.getMessage(ex);
      helmCommandRequest.getExecutionLogCallback().saveExecutionLog(
          errorMsg + "\nFailed", LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      logger.error(
          format("Exception in processing helm task [%s] and error is %s", helmCommandRequest.toString(), errorMsg));
      return HelmCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(errorMsg)
          .build();
    }

    helmCommandRequest.getExecutionLogCallback().saveExecutionLog(
        "Command finished with status " + commandResponse.getCommandExecutionStatus(), LogLevel.INFO,
        commandResponse.getCommandExecutionStatus());
    logger.info(commandResponse.getOutput());

    HelmCommandExecutionResponse helmCommandExecutionResponse =
        HelmCommandExecutionResponse.builder()
            .commandExecutionStatus(commandResponse.getCommandExecutionStatus())
            .helmCommandResponse(commandResponse)
            .build();

    if (CommandExecutionStatus.SUCCESS != commandResponse.getCommandExecutionStatus()) {
      helmCommandExecutionResponse.setErrorMessage(commandResponse.getOutput());
    }

    return helmCommandExecutionResponse;
  }

  private void init(HelmCommandRequest helmCommandRequest, LogCallback executionLogCallback) throws Exception {
    helmCommandRequest.setExecutionLogCallback(executionLogCallback);
    executionLogCallback.saveExecutionLog("Creating KubeConfig", LogLevel.INFO, CommandExecutionStatus.RUNNING);
    String configLocation = containerDeploymentDelegateHelper.createAndGetKubeConfigLocation(
        helmCommandRequest.getContainerServiceParams());
    helmCommandRequest.setKubeConfigLocation(configLocation);
    executionLogCallback.saveExecutionLog(
        "Setting KubeConfig\nKUBECONFIG_PATH=" + configLocation, LogLevel.INFO, CommandExecutionStatus.RUNNING);

    ensureHelmInstalled(helmCommandRequest);
    executionLogCallback.saveExecutionLog("\nDone.", LogLevel.INFO, CommandExecutionStatus.SUCCESS);
  }

  protected LogCallback getExecutionLogCallback(HelmCommandRequest helmCommandRequest, String name) {
    return isAsync() ? new ExecutionLogCallback(delegateLogService, helmCommandRequest.getAccountId(),
                           helmCommandRequest.getAppId(), helmCommandRequest.getActivityId(), name)
                     : new NoopExecutionCallback();
  }

  private void ensureHelmInstalled(HelmCommandRequest helmCommandRequest) {
    LogCallback executionLogCallback = helmCommandRequest.getExecutionLogCallback();

    executionLogCallback.saveExecutionLog("Finding helm version", LogLevel.INFO, CommandExecutionStatus.RUNNING);

    HelmCommandResponse helmCommandResponse = helmDeployService.ensureHelmInstalled(helmCommandRequest);
    logger.info(helmCommandResponse.getOutput());
    executionLogCallback.saveExecutionLog(helmCommandResponse.getOutput());
  }
}
