package software.wings.delegatetasks.helm;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.command.LogCallback;
import software.wings.beans.command.NoopExecutionCallback;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.HarnessException;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.helm.HelmCommandExecutionResponse;
import software.wings.helpers.ext.helm.HelmDeployService;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmCommandRequest.HelmCommandType;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmReleaseHistoryCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.helpers.ext.helm.response.HelmCommandResponse;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by anubhaw on 3/22/18.
 */
public class HelmCommandTask extends AbstractDelegateRunnableTask {
  @Inject private DelegateLogService delegateLogService;
  @Inject private HelmDeployService helmDeployService;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private HelmCommandHelper helmCommandHelper;

  private static final Logger logger = LoggerFactory.getLogger(HelmCommandTask.class);

  public HelmCommandTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public HelmCommandExecutionResponse run(Object[] parameters) {
    HelmCommandRequest helmCommandRequest = (HelmCommandRequest) parameters[0];
    HelmCommandResponse commandResponse;

    LogCallback executionLogCallback = getExecutionLogCallback(helmCommandRequest);
    helmCommandRequest.setExecutionLogCallback(executionLogCallback);

    try {
      executionLogCallback.saveExecutionLog("Creating KubeConfig", LogLevel.INFO, CommandExecutionStatus.RUNNING);
      String configLocation = containerDeploymentDelegateHelper.createAndGetKubeConfigLocation(
          helmCommandRequest.getContainerServiceParams());
      helmCommandRequest.setKubeConfigLocation(configLocation);
      executionLogCallback.saveExecutionLog(
          "Setting KubeConfig\nKUBECONFIG_PATH=" + configLocation, LogLevel.INFO, CommandExecutionStatus.RUNNING);

      ensureHelmCliAndTillerInstalled(helmCommandRequest);
      addPublicRepo(helmCommandRequest);

      executionLogCallback.saveExecutionLog(
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
      logger.error(format("Exception in processing helm task [%s]", helmCommandRequest.toString()), ex);
      return HelmCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(Misc.getMessage(ex))
          .build();
    }

    executionLogCallback.saveExecutionLog("Command finished with status " + commandResponse.getCommandExecutionStatus(),
        LogLevel.INFO, commandResponse.getCommandExecutionStatus());

    return HelmCommandExecutionResponse.builder()
        .commandExecutionStatus(commandResponse.getCommandExecutionStatus())
        .helmCommandResponse(commandResponse)
        .errorMessage(commandResponse.getOutput())
        .build();
  }

  protected LogCallback getExecutionLogCallback(HelmCommandRequest helmCommandRequest) {
    return isAsync()
        ? new ExecutionLogCallback(delegateLogService, helmCommandRequest.getAccountId(), helmCommandRequest.getAppId(),
              helmCommandRequest.getActivityId(), helmCommandRequest.getCommandName())
        : new NoopExecutionCallback();
  }

  private void ensureHelmCliAndTillerInstalled(HelmCommandRequest helmCommandRequest) throws Exception {
    LogCallback executionLogCallback = helmCommandRequest.getExecutionLogCallback();

    executionLogCallback.saveExecutionLog(
        "Finding helm client and server version", LogLevel.INFO, CommandExecutionStatus.RUNNING);
    HelmCommandResponse helmCommandResponse = helmDeployService.ensureHelmCliAndTillerInstalled(helmCommandRequest);
    executionLogCallback.saveExecutionLog(helmCommandResponse.getOutput());
  }

  private void addPublicRepo(HelmCommandRequest helmCommandRequest)
      throws InterruptedException, IOException, TimeoutException {
    LogCallback executionLogCallback = helmCommandRequest.getExecutionLogCallback();

    if (helmCommandRequest.getHelmCommandType() != HelmCommandType.INSTALL) {
      return;
    }

    if (helmCommandRequest.getChartSpecification() != null
        && isNotEmpty(helmCommandRequest.getChartSpecification().getChartUrl())
        && isNotEmpty(helmCommandRequest.getRepoName())) {
      executionLogCallback.saveExecutionLog(
          "Adding helm repository " + helmCommandRequest.getChartSpecification().getChartUrl(), LogLevel.INFO,
          CommandExecutionStatus.RUNNING);
      HelmCommandResponse helmCommandResponse = helmDeployService.addPublicRepo(helmCommandRequest);
      executionLogCallback.saveExecutionLog(helmCommandResponse.getOutput());
    }
  }
}
