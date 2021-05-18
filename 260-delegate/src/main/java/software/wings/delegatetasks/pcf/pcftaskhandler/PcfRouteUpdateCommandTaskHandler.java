package software.wings.delegatetasks.pcf.pcftaskhandler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidArgumentsException;
import io.harness.filesystem.FileIo;
import io.harness.logging.CommandExecutionStatus;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.PcfAppAutoscalarRequestData;
import io.harness.pcf.model.PcfRequestConfig;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRouteUpdateRequest;
import software.wings.helpers.ext.pcf.request.PcfRouteUpdateRequestConfigData;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfCommandResponse;

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
   * @param pcfCommandRequest
   * @param encryptedDataDetails
   * @param isInstanceSync
   * @return
   */
  @Override
  public PcfCommandExecutionResponse executeTaskInternal(PcfCommandRequest pcfCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback,
      boolean isInstanceSync) {
    if (!(pcfCommandRequest instanceof PcfCommandRouteUpdateRequest)) {
      throw new InvalidArgumentsException(
          Pair.of("pcfCommandRequest", "Must be instance of PcfCommandRouteUpdateRequest"));
    }
    PcfCommandResponse pcfCommandResponse = new PcfCommandResponse();
    PcfCommandExecutionResponse pcfCommandExecutionResponse =
        PcfCommandExecutionResponse.builder().pcfCommandResponse(pcfCommandResponse).build();

    File workingDirectory = null;
    try {
      // This will be CF_HOME for any cli related operations
      workingDirectory = pcfCommandTaskHelper.generateWorkingDirectoryForDeployment();

      executionLogCallback.saveExecutionLog(color("--------- Starting PCF Route Update\n", White, Bold));
      PcfCommandRouteUpdateRequest pcfCommandRouteUpdateRequest = (PcfCommandRouteUpdateRequest) pcfCommandRequest;
      PcfConfig pcfConfig = pcfCommandRouteUpdateRequest.getPcfConfig();
      encryptionService.decrypt(pcfConfig, encryptedDataDetails, false);

      PcfRequestConfig pcfRequestConfig =
          PcfRequestConfig.builder()
              .userName(String.valueOf(pcfConfig.getUsername()))
              .endpointUrl(pcfConfig.getEndpointUrl())
              .password(String.valueOf(pcfConfig.getPassword()))
              .orgName(pcfCommandRouteUpdateRequest.getOrganization())
              .spaceName(pcfCommandRouteUpdateRequest.getSpace())
              .timeOutIntervalInMins(pcfCommandRouteUpdateRequest.getTimeoutIntervalInMin())
              .cfHomeDirPath(workingDirectory.getAbsolutePath())
              .useCFCLI(pcfCommandRouteUpdateRequest.isUseCfCLI())
              .cfCliPath(pcfCommandTaskHelper.getCfCliPathOnDelegate(
                  pcfCommandRequest.isUseCfCLI(), pcfCommandRequest.getCfCliVersion()))
              .cfCliVersion(pcfCommandRequest.getCfCliVersion())
              .limitPcfThreads(pcfCommandRequest.isLimitPcfThreads())
              .ignorePcfConnectionContextCache(pcfCommandRequest.isIgnorePcfConnectionContextCache())
              .build();

      PcfRouteUpdateRequestConfigData pcfRouteUpdateConfigData =
          pcfCommandRouteUpdateRequest.getPcfRouteUpdateConfigData();
      if (pcfRouteUpdateConfigData.isStandardBlueGreen()) {
        if (swapRouteExecutionNeeded(pcfRouteUpdateConfigData)) {
          // If rollback and old app was downsized, restore it
          restoreOldAppDuringRollbackIfNeeded(executionLogCallback, pcfCommandRouteUpdateRequest, pcfRequestConfig,
              pcfRouteUpdateConfigData, workingDirectory.getAbsolutePath());
          // Swap routes
          performRouteUpdateForStandardBlueGreen(pcfCommandRouteUpdateRequest, pcfRequestConfig, executionLogCallback);
          // if deploy and downsizeOld is true
          downsizeOldAppDuringDeployIfRequired(executionLogCallback, pcfCommandRouteUpdateRequest, pcfRequestConfig,
              pcfRouteUpdateConfigData, workingDirectory.getAbsolutePath());
        } else {
          executionLogCallback.saveExecutionLog(color("# No Route Update Required In Rollback", White, Bold));
        }
      } else {
        performRouteUpdateForSimulatedBlueGreen(pcfCommandRouteUpdateRequest, pcfRequestConfig, executionLogCallback);
      }

      executionLogCallback.saveExecutionLog("\n--------- PCF Route Update completed successfully");
      pcfCommandResponse.setOutput(StringUtils.EMPTY);
      pcfCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
    } catch (Exception e) {
      log.error("Exception in processing PCF Route Update task", e);
      executionLogCallback.saveExecutionLog("\n\n--------- PCF Route Update failed to complete successfully");
      executionLogCallback.saveExecutionLog("# Error: " + e.getMessage());
      pcfCommandResponse.setOutput(e.getMessage());
      pcfCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
    } finally {
      try {
        if (workingDirectory != null) {
          FileIo.deleteDirectoryAndItsContentIfExists(workingDirectory.getAbsolutePath());
        }
      } catch (IOException e) {
        log.warn("Failed to delete temp directory created for CF CLI login", e);
      }
    }

    pcfCommandExecutionResponse.setCommandExecutionStatus(pcfCommandResponse.getCommandExecutionStatus());
    pcfCommandExecutionResponse.setErrorMessage(pcfCommandResponse.getOutput());
    return pcfCommandExecutionResponse;
  }

  // This tells if routeUpdate needs to happen in Rollback.
  // If its rollback, and routeUpdate was not executed, no need to do anything
  @VisibleForTesting
  boolean swapRouteExecutionNeeded(PcfRouteUpdateRequestConfigData pcfRouteUpdateConfigData) {
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
  void restoreOldAppDuringRollbackIfNeeded(ExecutionLogCallback executionLogCallback,
      PcfCommandRouteUpdateRequest pcfCommandRouteUpdateRequest, PcfRequestConfig pcfRequestConfig,
      PcfRouteUpdateRequestConfigData pcfRouteUpdateConfigData, String configVarPath) {
    if (pcfRouteUpdateConfigData.isRollback() && pcfRouteUpdateConfigData.isDownsizeOldApplication()) {
      resizeOldApplications(pcfCommandRouteUpdateRequest, pcfRequestConfig, executionLogCallback, true, configVarPath);
    }
  }

  @VisibleForTesting
  void resizeOldApplications(PcfCommandRouteUpdateRequest pcfCommandRouteUpdateRequest,
      PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback, boolean isRollback,
      String configVarPath) {
    PcfRouteUpdateRequestConfigData pcfRouteUpdateConfigData =
        pcfCommandRouteUpdateRequest.getPcfRouteUpdateConfigData();

    String msg =
        isRollback ? "\n# Restoring Old Apps to original count" : "\n# Resizing Old Apps to 0 count as configured";
    executionLogCallback.saveExecutionLog(msg);
    String appNameBeingDownsized = null;

    List<PcfAppSetupTimeDetails> existingApplicationDetails = pcfRouteUpdateConfigData.getExistingApplicationDetails();
    if (isNotEmpty(existingApplicationDetails)) {
      try {
        PcfAppSetupTimeDetails existingAppDetails = existingApplicationDetails.get(0);
        appNameBeingDownsized = existingAppDetails.getApplicationName();
        int count = isRollback ? existingAppDetails.getInitialInstanceCount() : 0;

        pcfRequestConfig.setApplicationName(appNameBeingDownsized);
        pcfRequestConfig.setDesiredCount(count);
        executionLogCallback.saveExecutionLog(new StringBuilder()
                                                  .append("Resizing Application: {")
                                                  .append(appNameBeingDownsized)
                                                  .append("} to Count: ")
                                                  .append(count)
                                                  .toString());

        // If downsizing, disable auto-scalar
        PcfAppAutoscalarRequestData appAutoscalarRequestData = null;
        if (pcfCommandRouteUpdateRequest.isUseAppAutoscalar()) {
          ApplicationDetail applicationDetail = pcfDeploymentManager.getApplicationByName(pcfRequestConfig);
          appAutoscalarRequestData = PcfAppAutoscalarRequestData.builder()
                                         .applicationGuid(applicationDetail.getId())
                                         .applicationName(applicationDetail.getName())
                                         .pcfRequestConfig(pcfRequestConfig)
                                         .configPathVar(configVarPath)
                                         .timeoutInMins(pcfCommandRouteUpdateRequest.getTimeoutIntervalInMin())
                                         .build();

          // Before downsizing, disable autoscalar if its enabled.
          if (!isRollback && count == 0) {
            appAutoscalarRequestData.setExpectedEnabled(true);
            pcfCommandTaskHelper.disableAutoscalar(appAutoscalarRequestData, executionLogCallback);
          }
        }

        // resize app (upsize in swap rollback, downsize in swap state)
        if (!isRollback) {
          pcfDeploymentManager.resizeApplication(pcfRequestConfig);
        } else {
          pcfDeploymentManager.upsizeApplicationWithSteadyStateCheck(pcfRequestConfig, executionLogCallback);
        }

        // After resize, enable autoscalar if it was attached.
        if (isRollback && pcfCommandRouteUpdateRequest.isUseAppAutoscalar()) {
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
  void downsizeOldAppDuringDeployIfRequired(ExecutionLogCallback executionLogCallback,
      PcfCommandRouteUpdateRequest pcfCommandRouteUpdateRequest, PcfRequestConfig pcfRequestConfig,
      PcfRouteUpdateRequestConfigData pcfRouteUpdateConfigData, String configVarPath) {
    if (!pcfRouteUpdateConfigData.isRollback() && pcfRouteUpdateConfigData.isDownsizeOldApplication()) {
      resizeOldApplications(pcfCommandRouteUpdateRequest, pcfRequestConfig, executionLogCallback, false, configVarPath);
    }
  }

  private void performRouteUpdateForSimulatedBlueGreen(PcfCommandRouteUpdateRequest pcfCommandRouteUpdateRequest,
      PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    PcfRouteUpdateRequestConfigData data = pcfCommandRouteUpdateRequest.getPcfRouteUpdateConfigData();

    for (String appName : data.getExistingApplicationNames()) {
      if (data.isMapRoutesOperation()) {
        pcfCommandTaskHelper.mapRouteMaps(appName, data.getFinalRoutes(), pcfRequestConfig, executionLogCallback);
      } else {
        pcfCommandTaskHelper.unmapRouteMaps(appName, data.getFinalRoutes(), pcfRequestConfig, executionLogCallback);
      }
    }
  }

  private void performRouteUpdateForStandardBlueGreen(PcfCommandRouteUpdateRequest pcfCommandRouteUpdateRequest,
      PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    PcfRouteUpdateRequestConfigData data = pcfCommandRouteUpdateRequest.getPcfRouteUpdateConfigData();

    if (!data.isRollback()) {
      updateRoutesForNewApplication(pcfRequestConfig, executionLogCallback, data);
      updateRoutesForExistingApplication(pcfRequestConfig, executionLogCallback, data);
    } else {
      updateRoutesForExistingApplication(pcfRequestConfig, executionLogCallback, data);
      updateRoutesForNewApplication(pcfRequestConfig, executionLogCallback, data);
    }
  }

  private void updateRoutesForExistingApplication(PcfRequestConfig pcfRequestConfig,
      ExecutionLogCallback executionLogCallback, PcfRouteUpdateRequestConfigData data)
      throws PivotalClientApiException {
    if (isNotEmpty(data.getExistingApplicationNames())) {
      List<String> mapRouteForExistingApp = data.isRollback() ? data.getFinalRoutes() : data.getTempRoutes();
      List<String> unmapRouteForExistingApp = data.isRollback() ? data.getTempRoutes() : data.getFinalRoutes();
      for (String existingAppName : data.getExistingApplicationNames()) {
        pcfCommandTaskHelper.mapRouteMaps(
            existingAppName, mapRouteForExistingApp, pcfRequestConfig, executionLogCallback);
        pcfCommandTaskHelper.unmapRouteMaps(
            existingAppName, unmapRouteForExistingApp, pcfRequestConfig, executionLogCallback);
        updateEnvVariableForApplication(pcfRequestConfig, executionLogCallback, existingAppName, data.isRollback());
      }
    }
  }

  private void updateEnvVariableForApplication(PcfRequestConfig pcfRequestConfig,
      ExecutionLogCallback executionLogCallback, String appName, boolean isActiveApplication)
      throws PivotalClientApiException {
    pcfRequestConfig.setApplicationName(appName);
    pcfDeploymentManager.setEnvironmentVariableForAppStatus(
        pcfRequestConfig, isActiveApplication, executionLogCallback);
  }

  private void updateRoutesForNewApplication(PcfRequestConfig pcfRequestConfig,
      ExecutionLogCallback executionLogCallback, PcfRouteUpdateRequestConfigData data)
      throws PivotalClientApiException {
    List<String> mapRouteForNewApp = data.isRollback() ? data.getTempRoutes() : data.getFinalRoutes();
    List<String> unmapRouteForNewApp = data.isRollback() ? data.getFinalRoutes() : data.getTempRoutes();
    pcfCommandTaskHelper.mapRouteMaps(
        data.getNewApplicatiaonName(), mapRouteForNewApp, pcfRequestConfig, executionLogCallback);
    pcfCommandTaskHelper.unmapRouteMaps(
        data.getNewApplicatiaonName(), unmapRouteForNewApp, pcfRequestConfig, executionLogCallback);
    // mark new app as ACTIVE if not rollback, STAGE if rollback
    updateEnvVariableForApplication(
        pcfRequestConfig, executionLogCallback, data.getNewApplicatiaonName(), !data.isRollback());
  }
}
