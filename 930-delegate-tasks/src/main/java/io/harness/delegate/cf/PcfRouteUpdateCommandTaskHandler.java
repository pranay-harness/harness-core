package io.harness.delegate.cf;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.beans.pcf.CfInternalConfig;
import io.harness.delegate.beans.pcf.CfRouteUpdateRequestConfigData;
import io.harness.delegate.task.pcf.CfCommandRequest;
import io.harness.delegate.task.pcf.CfCommandResponse;
import io.harness.delegate.task.pcf.request.CfCommandRouteUpdateRequest;
import io.harness.delegate.task.pcf.response.CfCommandExecutionResponse;
import io.harness.exception.InvalidArgumentsException;
import io.harness.filesystem.FileIo;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfAppAutoscalarRequestData;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.operations.applications.ApplicationDetail;

@NoArgsConstructor
@Singleton
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(HarnessTeam.CDP)
public class PcfRouteUpdateCommandTaskHandler extends PcfCommandTaskHandler {
  /**
   * Performs RouteSwapping for Blue-Green deployment
   *
   * @param cfCommandRequest
   * @param encryptedDataDetails
   * @param logStreamingTaskClient
   * @param isInstanceSync
   * @return
   */
  @Override
  public CfCommandExecutionResponse executeTaskInternal(CfCommandRequest cfCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ILogStreamingTaskClient logStreamingTaskClient,
      boolean isInstanceSync) {
    if (!(cfCommandRequest instanceof CfCommandRouteUpdateRequest)) {
      throw new InvalidArgumentsException(
          Pair.of("cfCommandRequest", "Must be instance of CfCommandRouteUpdateRequest"));
    }
    LogCallback executionLogCallback = logStreamingTaskClient.obtainLogCallback(cfCommandRequest.getCommandName());
    CfCommandResponse cfCommandResponse = new CfCommandResponse();
    CfCommandExecutionResponse cfCommandExecutionResponse =
        CfCommandExecutionResponse.builder().pcfCommandResponse(cfCommandResponse).build();

    File workingDirectory = null;
    try {
      // This will be CF_HOME for any cli related operations
      workingDirectory = pcfCommandTaskBaseHelper.generateWorkingDirectoryForDeployment();

      executionLogCallback.saveExecutionLog(color("--------- Starting PCF Route Update\n", White, Bold));
      CfCommandRouteUpdateRequest cfCommandRouteUpdateRequest = (CfCommandRouteUpdateRequest) cfCommandRequest;
      CfInternalConfig pcfConfig = cfCommandRouteUpdateRequest.getPcfConfig();
      secretDecryptionService.decrypt(pcfConfig, encryptedDataDetails, false);

      CfRequestConfig cfRequestConfig =
          CfRequestConfig.builder()
              .userName(String.valueOf(pcfConfig.getUsername()))
              .endpointUrl(pcfConfig.getEndpointUrl())
              .password(String.valueOf(pcfConfig.getPassword()))
              .orgName(cfCommandRouteUpdateRequest.getOrganization())
              .spaceName(cfCommandRouteUpdateRequest.getSpace())
              .timeOutIntervalInMins(cfCommandRouteUpdateRequest.getTimeoutIntervalInMin())
              .cfHomeDirPath(workingDirectory.getAbsolutePath())
              .useCFCLI(cfCommandRouteUpdateRequest.isUseCfCLI())
              .cfCliPath(pcfCommandTaskBaseHelper.getCfCliPathOnDelegate(
                  cfCommandRequest.isUseCfCLI(), cfCommandRequest.getCfCliVersion()))
              .cfCliVersion(cfCommandRequest.getCfCliVersion())
              .limitPcfThreads(cfCommandRequest.isLimitPcfThreads())
              .ignorePcfConnectionContextCache(cfCommandRequest.isIgnorePcfConnectionContextCache())
              .build();

      CfRouteUpdateRequestConfigData pcfRouteUpdateConfigData =
          cfCommandRouteUpdateRequest.getPcfRouteUpdateConfigData();
      if (pcfRouteUpdateConfigData.isStandardBlueGreen()) {
        if (swapRouteExecutionNeeded(pcfRouteUpdateConfigData)) {
          // If rollback and old app was downsized, restore it
          restoreOldAppDuringRollbackIfNeeded(executionLogCallback, cfCommandRouteUpdateRequest, cfRequestConfig,
              pcfRouteUpdateConfigData, workingDirectory.getAbsolutePath());
          // Swap routes
          performRouteUpdateForStandardBlueGreen(cfCommandRouteUpdateRequest, cfRequestConfig, executionLogCallback);
          // if deploy and downsizeOld is true
          downsizeOldAppDuringDeployIfRequired(executionLogCallback, cfCommandRouteUpdateRequest, cfRequestConfig,
              pcfRouteUpdateConfigData, workingDirectory.getAbsolutePath());
        } else {
          executionLogCallback.saveExecutionLog(color("# No Route Update Required In Rollback", White, Bold));
        }
      } else {
        performRouteUpdateForSimulatedBlueGreen(cfCommandRouteUpdateRequest, cfRequestConfig, executionLogCallback);
      }

      executionLogCallback.saveExecutionLog("\n--------- PCF Route Update completed successfully");
      cfCommandResponse.setOutput(StringUtils.EMPTY);
      cfCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
    } catch (Exception e) {
      log.error("Exception in processing PCF Route Update task", e);
      executionLogCallback.saveExecutionLog("\n\n--------- PCF Route Update failed to complete successfully");
      executionLogCallback.saveExecutionLog("# Error: " + e.getMessage());
      cfCommandResponse.setOutput(e.getMessage());
      cfCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
    } finally {
      try {
        if (workingDirectory != null) {
          FileIo.deleteDirectoryAndItsContentIfExists(workingDirectory.getAbsolutePath());
        }
      } catch (IOException e) {
        log.warn("Failed to delete temp directory created for CF CLI login", e);
      }
    }

    cfCommandExecutionResponse.setCommandExecutionStatus(cfCommandResponse.getCommandExecutionStatus());
    cfCommandExecutionResponse.setErrorMessage(cfCommandResponse.getOutput());
    return cfCommandExecutionResponse;
  }

  // This tells if routeUpdate needs to happen in Rollback.
  // If its rollback, and routeUpdate was not executed, no need to do anything
  @VisibleForTesting
  boolean swapRouteExecutionNeeded(CfRouteUpdateRequestConfigData pcfRouteUpdateConfigData) {
    boolean executionNeeded;
    if (pcfRouteUpdateConfigData == null) {
      executionNeeded = false;
    } else if (!pcfRouteUpdateConfigData.isRollback()) {
      executionNeeded = true;
    } else {
      executionNeeded = !pcfRouteUpdateConfigData.isSkipRollback();
    }

    return executionNeeded;
  }

  @VisibleForTesting
  void restoreOldAppDuringRollbackIfNeeded(LogCallback executionLogCallback,
      CfCommandRouteUpdateRequest cfCommandRouteUpdateRequest, CfRequestConfig cfRequestConfig,
      CfRouteUpdateRequestConfigData pcfRouteUpdateConfigData, String configVarPath) {
    if (pcfRouteUpdateConfigData.isRollback() && pcfRouteUpdateConfigData.isDownsizeOldApplication()) {
      resizeOldApplications(cfCommandRouteUpdateRequest, cfRequestConfig, executionLogCallback, true, configVarPath);
    }
  }

  @VisibleForTesting
  void resizeOldApplications(CfCommandRouteUpdateRequest cfCommandRouteUpdateRequest, CfRequestConfig cfRequestConfig,
      LogCallback executionLogCallback, boolean isRollback, String configVarPath) {
    CfRouteUpdateRequestConfigData pcfRouteUpdateConfigData = cfCommandRouteUpdateRequest.getPcfRouteUpdateConfigData();

    String msg =
        isRollback ? "\n# Restoring Old Apps to original count" : "\n# Resizing Old Apps to 0 count as configured";
    executionLogCallback.saveExecutionLog(msg);
    String appNameBeingDownsized = null;

    List<CfAppSetupTimeDetails> existingApplicationDetails = pcfRouteUpdateConfigData.getExistingApplicationDetails();
    if (isNotEmpty(existingApplicationDetails)) {
      try {
        CfAppSetupTimeDetails existingAppDetails = existingApplicationDetails.get(0);
        appNameBeingDownsized = existingAppDetails.getApplicationName();
        int count = isRollback ? existingAppDetails.getInitialInstanceCount() : 0;

        cfRequestConfig.setApplicationName(appNameBeingDownsized);
        cfRequestConfig.setDesiredCount(count);
        executionLogCallback.saveExecutionLog(new StringBuilder()
                                                  .append("Resizing Application: {")
                                                  .append(appNameBeingDownsized)
                                                  .append("} to Count: ")
                                                  .append(count)
                                                  .toString());

        // If downsizing, disable auto-scalar
        CfAppAutoscalarRequestData appAutoscalarRequestData = null;
        if (cfCommandRouteUpdateRequest.isUseAppAutoscalar()) {
          ApplicationDetail applicationDetail = pcfDeploymentManager.getApplicationByName(cfRequestConfig);
          appAutoscalarRequestData = CfAppAutoscalarRequestData.builder()
                                         .applicationGuid(applicationDetail.getId())
                                         .applicationName(applicationDetail.getName())
                                         .cfRequestConfig(cfRequestConfig)
                                         .configPathVar(configVarPath)
                                         .timeoutInMins(cfCommandRouteUpdateRequest.getTimeoutIntervalInMin())
                                         .build();

          // Before downsizing, disable autoscalar if its enabled.
          if (!isRollback) {
            appAutoscalarRequestData.setExpectedEnabled(true);
            pcfCommandTaskBaseHelper.disableAutoscalar(appAutoscalarRequestData, executionLogCallback);
          }
        }

        // resize app (upsize in swap rollback, downsize in swap state)
        if (!isRollback) {
          pcfDeploymentManager.resizeApplication(cfRequestConfig);
        } else {
          pcfDeploymentManager.upsizeApplicationWithSteadyStateCheck(cfRequestConfig, executionLogCallback);
        }

        // After resize, enable autoscalar if it was attached.
        if (isRollback && cfCommandRouteUpdateRequest.isUseAppAutoscalar()) {
          appAutoscalarRequestData.setExpectedEnabled(false);
          pcfDeploymentManager.changeAutoscalarState(appAutoscalarRequestData, executionLogCallback, true);
        }
      } catch (Exception e) {
        log.error("Failed to downsize PCF application: " + appNameBeingDownsized, e);
        executionLogCallback.saveExecutionLog("Failed while downsizing old application: " + appNameBeingDownsized);
      }
    }
  }

  @VisibleForTesting
  void downsizeOldAppDuringDeployIfRequired(LogCallback executionLogCallback,
      CfCommandRouteUpdateRequest cfCommandRouteUpdateRequest, CfRequestConfig cfRequestConfig,
      CfRouteUpdateRequestConfigData pcfRouteUpdateConfigData, String configVarPath) {
    if (!pcfRouteUpdateConfigData.isRollback() && pcfRouteUpdateConfigData.isDownsizeOldApplication()) {
      resizeOldApplications(cfCommandRouteUpdateRequest, cfRequestConfig, executionLogCallback, false, configVarPath);
    }
  }

  private void performRouteUpdateForSimulatedBlueGreen(CfCommandRouteUpdateRequest cfCommandRouteUpdateRequest,
      CfRequestConfig cfRequestConfig, LogCallback executionLogCallback) throws PivotalClientApiException {
    CfRouteUpdateRequestConfigData data = cfCommandRouteUpdateRequest.getPcfRouteUpdateConfigData();

    for (String appName : data.getExistingApplicationNames()) {
      if (data.isMapRoutesOperation()) {
        pcfCommandTaskBaseHelper.mapRouteMaps(appName, data.getFinalRoutes(), cfRequestConfig, executionLogCallback);
      } else {
        pcfCommandTaskBaseHelper.unmapRouteMaps(appName, data.getFinalRoutes(), cfRequestConfig, executionLogCallback);
      }
    }
  }

  private void performRouteUpdateForStandardBlueGreen(CfCommandRouteUpdateRequest cfCommandRouteUpdateRequest,
      CfRequestConfig cfRequestConfig, LogCallback executionLogCallback) throws PivotalClientApiException {
    CfRouteUpdateRequestConfigData data = cfCommandRouteUpdateRequest.getPcfRouteUpdateConfigData();

    if (!data.isRollback()) {
      updateRoutesForNewApplication(cfRequestConfig, executionLogCallback, data);
      updateRoutesForExistingApplication(cfRequestConfig, executionLogCallback, data);
    } else {
      updateRoutesForExistingApplication(cfRequestConfig, executionLogCallback, data);
      updateRoutesForNewApplication(cfRequestConfig, executionLogCallback, data);
    }
  }

  private void updateRoutesForExistingApplication(CfRequestConfig cfRequestConfig, LogCallback executionLogCallback,
      CfRouteUpdateRequestConfigData data) throws PivotalClientApiException {
    if (isNotEmpty(data.getExistingApplicationNames())) {
      List<String> mapRouteForExistingApp = data.isRollback() ? data.getFinalRoutes() : data.getTempRoutes();
      List<String> unmapRouteForExistingApp = data.isRollback() ? data.getTempRoutes() : data.getFinalRoutes();
      for (String existingAppName : data.getExistingApplicationNames()) {
        pcfCommandTaskBaseHelper.mapRouteMaps(
            existingAppName, mapRouteForExistingApp, cfRequestConfig, executionLogCallback);
        pcfCommandTaskBaseHelper.unmapRouteMaps(
            existingAppName, unmapRouteForExistingApp, cfRequestConfig, executionLogCallback);
        updateEnvVariableForApplication(cfRequestConfig, executionLogCallback, existingAppName, data.isRollback());
      }
    }
  }

  private void updateEnvVariableForApplication(CfRequestConfig cfRequestConfig, LogCallback executionLogCallback,
      String appName, boolean isActiveApplication) throws PivotalClientApiException {
    cfRequestConfig.setApplicationName(appName);
    pcfDeploymentManager.setEnvironmentVariableForAppStatus(cfRequestConfig, isActiveApplication, executionLogCallback);
  }

  private void updateRoutesForNewApplication(CfRequestConfig cfRequestConfig, LogCallback executionLogCallback,
      CfRouteUpdateRequestConfigData data) throws PivotalClientApiException {
    List<String> mapRouteForNewApp = data.isRollback() ? data.getTempRoutes() : data.getFinalRoutes();
    List<String> unmapRouteForNewApp = data.isRollback() ? data.getFinalRoutes() : data.getTempRoutes();
    pcfCommandTaskBaseHelper.mapRouteMaps(
        data.getNewApplicatiaonName(), mapRouteForNewApp, cfRequestConfig, executionLogCallback);
    pcfCommandTaskBaseHelper.unmapRouteMaps(
        data.getNewApplicatiaonName(), unmapRouteForNewApp, cfRequestConfig, executionLogCallback);
    // mark new app as ACTIVE if not rollback, STAGE if rollback
    updateEnvVariableForApplication(
        cfRequestConfig, executionLogCallback, data.getNewApplicatiaonName(), !data.isRollback());
  }
}
