package io.harness.delegate.task.helm;

import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.waitForDirectoryToBeAccessibleOutOfProcess;

import static java.lang.String.format;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGDelegateLogCallback;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.exception.TaskNGDataException;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.ManifestDelegateConfigHelper;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.k8s.K8sGlobalConfigService;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import com.google.inject.Inject;
import java.nio.file.Paths;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class HelmCommandTaskNG extends AbstractDelegateRunnableTask {
  @Inject private HelmDeployServiceNG helmDeployServiceNG;
  @Inject private ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;
  @Inject private K8sGlobalConfigService k8sGlobalConfigService;
  @Inject private ManifestDelegateConfigHelper manifestDelegateConfigHelper;

  private static final String WORKING_DIR_BASE = "./repository/helm/";
  public static final String MANIFEST_FILES_DIR = "manifest-files";

  public static final String FetchFiles = "Fetch Files";
  public static final String Init = "Initialize";
  public static final String Prepare = "Prepare";
  public static final String InstallUpgrade = "Install / Upgrade";
  public static final String WaitForSteadyState = "Wait For Steady State";
  public static final String WrapUp = "Wrap Up";
  public static final String Rollback = "Rollback";

  public HelmCommandTaskNG(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public boolean isSupportingErrorFramework() {
    return true;
  }

  @Override
  public HelmCmdExecResponseNG run(TaskParameters parameters) {
    helmDeployServiceNG.setLogStreamingClient(this.getLogStreamingTaskClient());
    HelmCommandRequestNG helmCommandRequestNG = (HelmCommandRequestNG) parameters;
    HelmCommandResponseNG helmCommandResponseNG;

    String workingDirectory = Paths.get(WORKING_DIR_BASE, convertBase64UuidToCanonicalForm(generateUuid()))
                                  .normalize()
                                  .toAbsolutePath()
                                  .toString();

    try {
      createDirectoryIfDoesNotExist(workingDirectory);
      waitForDirectoryToBeAccessibleOutOfProcess(workingDirectory, 10);

      helmCommandRequestNG.setWorkingDir(workingDirectory);

      decryptRequestDTOs(helmCommandRequestNG);

      init(helmCommandRequestNG,
          getLogCallback(getLogStreamingTaskClient(), Init, helmCommandRequestNG.isShouldOpenFetchFilesLogStream(),
              helmCommandRequestNG.getCommandUnitsProgress()));

      helmCommandRequestNG.setLogCallback(getLogCallback(getLogStreamingTaskClient(), Prepare,
          helmCommandRequestNG.isShouldOpenFetchFilesLogStream(), helmCommandRequestNG.getCommandUnitsProgress()));

      helmCommandRequestNG.getLogCallback().saveExecutionLog(
          getDeploymentMessage(helmCommandRequestNG), LogLevel.INFO, CommandExecutionStatus.RUNNING);

      switch (helmCommandRequestNG.getHelmCommandType()) {
        case INSTALL:
          helmCommandResponseNG = helmDeployServiceNG.deploy((HelmInstallCommandRequestNG) helmCommandRequestNG);
          break;
        case ROLLBACK:
          helmCommandResponseNG = helmDeployServiceNG.rollback((HelmRollbackCommandRequestNG) helmCommandRequestNG);
          break;
        case RELEASE_HISTORY:
          helmCommandResponseNG =
              helmDeployServiceNG.releaseHistory((HelmReleaseHistoryCommandRequestNG) helmCommandRequestNG);
          break;
        default:
          throw new UnsupportedOperationException("Operation not supported");
      }
    } catch (Exception ex) {
      String errorMsg = ex.getMessage();
      helmCommandRequestNG.getLogCallback().saveExecutionLog(
          errorMsg + "\n Overall deployment Failed", LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      log.error(format("Exception in processing helm task [%s]", helmCommandRequestNG.toString()), ex);
      throw new TaskNGDataException(UnitProgressDataMapper.toUnitProgressData(helmCommandRequestNG.getCommandUnitsProgress()), ex);
    }

    helmCommandRequestNG.getLogCallback().saveExecutionLog(
        "Command finished with status " + helmCommandResponseNG.getCommandExecutionStatus(), LogLevel.INFO,
        helmCommandResponseNG.getCommandExecutionStatus());
    log.info(helmCommandResponseNG.getOutput());

    HelmCmdExecResponseNG helmCommandExecutionResponse =
        HelmCmdExecResponseNG.builder()
            .commandExecutionStatus(helmCommandResponseNG.getCommandExecutionStatus())
            .helmCommandResponse(helmCommandResponseNG)
            .commandUnitsProgress(
                UnitProgressDataMapper.toUnitProgressData(helmCommandRequestNG.getCommandUnitsProgress()))
            .build();

    if (CommandExecutionStatus.SUCCESS != helmCommandResponseNG.getCommandExecutionStatus()) {
      helmCommandExecutionResponse.setErrorMessage(helmCommandResponseNG.getOutput());
    }

    return helmCommandExecutionResponse;
  }

  public void decryptRequestDTOs(HelmCommandRequestNG commandRequestNG) {
    manifestDelegateConfigHelper.decryptManifestDelegateConfig(commandRequestNG.getManifestDelegateConfig());
    containerDeploymentDelegateBaseHelper.decryptK8sInfraDelegateConfig(commandRequestNG.getK8sInfraDelegateConfig());
  }

  private void init(HelmCommandRequestNG commandRequestNG, LogCallback logCallback) {
    commandRequestNG.setLogCallback(logCallback);
    logCallback.saveExecutionLog("Creating KubeConfig", LogLevel.INFO, CommandExecutionStatus.RUNNING);
    String configLocation = containerDeploymentDelegateBaseHelper.createKubeConfig(
        containerDeploymentDelegateBaseHelper.createKubernetesConfig(commandRequestNG.getK8sInfraDelegateConfig()));
    commandRequestNG.setKubeConfigLocation(configLocation);
    commandRequestNG.setOcPath(k8sGlobalConfigService.getOcPath());
    logCallback.saveExecutionLog(
        "Setting KubeConfig\nKUBECONFIG_PATH=" + configLocation, LogLevel.INFO, CommandExecutionStatus.RUNNING);

    ensureHelmInstalled(commandRequestNG);
    logCallback.saveExecutionLog("\nDone.", LogLevel.INFO, CommandExecutionStatus.SUCCESS);
  }

  private void ensureHelmInstalled(HelmCommandRequestNG helmCommandRequest) {
    LogCallback logCallback = helmCommandRequest.getLogCallback();

    logCallback.saveExecutionLog("Finding helm version", LogLevel.INFO, CommandExecutionStatus.RUNNING);

    HelmCommandResponseNG helmCommandResponse = helmDeployServiceNG.ensureHelmInstalled(helmCommandRequest);
    log.info(helmCommandResponse.getOutput());
    logCallback.saveExecutionLog(helmCommandResponse.getOutput());
  }

  public LogCallback getLogCallback(ILogStreamingTaskClient logStreamingTaskClient, String commandUnitName,
      boolean shouldOpenStream, CommandUnitsProgress commandUnitsProgress) {
    return new NGDelegateLogCallback(logStreamingTaskClient, commandUnitName, shouldOpenStream, commandUnitsProgress);
  }

  String getDeploymentMessage(HelmCommandRequestNG helmCommandRequest) {
    switch (helmCommandRequest.getHelmCommandType()) {
      case INSTALL:
        return "Installing";
      case ROLLBACK:
        return "Rolling back";
      case RELEASE_HISTORY:
        return "Getting release history";
      default:
        return "Unsupported operation";
    }
  }
}
