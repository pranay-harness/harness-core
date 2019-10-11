package software.wings.delegatetasks.pcf.pcftaskhandler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.InvalidArgumentsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.PivotalClientApiException;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRouteUpdateRequest;
import software.wings.helpers.ext.pcf.request.PcfRouteUpdateRequestConfigData;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfCommandResponse;

import java.util.List;

@NoArgsConstructor
@Singleton
@Slf4j
public class PcfRouteUpdateCommandTaskHandler extends PcfCommandTaskHandler {
  /**
   * Performs RouteSwapping for Blue-Green deployment
   * @param pcfCommandRequest
   * @param encryptedDataDetails
   * @return
   */
  public PcfCommandExecutionResponse executeTaskInternal(PcfCommandRequest pcfCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    if (!(pcfCommandRequest instanceof PcfCommandRouteUpdateRequest)) {
      throw new InvalidArgumentsException(
          Pair.of("pcfCommandRequest", "Must be instance of PcfCommandRouteUpdateRequest"));
    }
    PcfCommandResponse pcfCommandResponse = new PcfCommandResponse();
    PcfCommandExecutionResponse pcfCommandExecutionResponse =
        PcfCommandExecutionResponse.builder().pcfCommandResponse(pcfCommandResponse).build();

    try {
      executionLogCallback.saveExecutionLog("--------- Starting PCF Route Update\n");
      PcfCommandRouteUpdateRequest pcfCommandRouteUpdateRequest = (PcfCommandRouteUpdateRequest) pcfCommandRequest;
      PcfConfig pcfConfig = pcfCommandRouteUpdateRequest.getPcfConfig();
      encryptionService.decrypt(pcfConfig, encryptedDataDetails);

      PcfRequestConfig pcfRequestConfig =
          PcfRequestConfig.builder()
              .userName(pcfConfig.getUsername())
              .endpointUrl(pcfConfig.getEndpointUrl())
              .password(String.valueOf(pcfConfig.getPassword()))
              .orgName(pcfCommandRouteUpdateRequest.getOrganization())
              .spaceName(pcfCommandRouteUpdateRequest.getSpace())
              .timeOutIntervalInMins(pcfCommandRouteUpdateRequest.getTimeoutIntervalInMin())
              .build();

      PcfRouteUpdateRequestConfigData pcfRouteUpdateConfigData =
          pcfCommandRouteUpdateRequest.getPcfRouteUpdateConfigData();
      if (pcfRouteUpdateConfigData.isStandardBlueGreen()) {
        // If rollback and old app was downsized, restore it
        restoreOldAppDuringRollbackIfNeeded(
            executionLogCallback, pcfCommandRouteUpdateRequest, pcfRequestConfig, pcfRouteUpdateConfigData);
        // Swap routes
        performRouteUpdateForStandardBlueGreen(pcfCommandRouteUpdateRequest, pcfRequestConfig, executionLogCallback);
        // if deploy and downsizeOld is true
        downsizeOldAppDuringDeployIfRequired(
            executionLogCallback, pcfCommandRouteUpdateRequest, pcfRequestConfig, pcfRouteUpdateConfigData);
      } else {
        performRouteUpdateForSimulatedBlueGreen(pcfCommandRouteUpdateRequest, pcfRequestConfig, executionLogCallback);
      }

      executionLogCallback.saveExecutionLog("\n--------- PCF Route Update completed successfully");
      pcfCommandResponse.setOutput(StringUtils.EMPTY);
      pcfCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
    } catch (Exception e) {
      logger.error("Exception in processing PCF Route Update task", e);
      executionLogCallback.saveExecutionLog("\n\n--------- PCF Route Update failed to complete successfully");
      executionLogCallback.saveExecutionLog("# Error: " + e.getMessage());
      pcfCommandResponse.setOutput(e.getMessage());
      pcfCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
    }

    pcfCommandExecutionResponse.setCommandExecutionStatus(pcfCommandResponse.getCommandExecutionStatus());
    pcfCommandExecutionResponse.setErrorMessage(pcfCommandResponse.getOutput());
    return pcfCommandExecutionResponse;
  }

  @VisibleForTesting
  void restoreOldAppDuringRollbackIfNeeded(ExecutionLogCallback executionLogCallback,
      PcfCommandRouteUpdateRequest pcfCommandRouteUpdateRequest, PcfRequestConfig pcfRequestConfig,
      PcfRouteUpdateRequestConfigData pcfRouteUpdateConfigData) {
    if (pcfRouteUpdateConfigData.isRollback() && pcfRouteUpdateConfigData.isDownsizeOldApplication()) {
      resizeOldApplications(pcfCommandRouteUpdateRequest, pcfRequestConfig, executionLogCallback, true);
    }
  }

  @VisibleForTesting
  void resizeOldApplications(PcfCommandRouteUpdateRequest pcfCommandRouteUpdateRequest,
      PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback, boolean isRollback) {
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
        pcfDeploymentManager.resizeApplication(pcfRequestConfig);
      } catch (Exception e) {
        logger.error("Failed to downsize PCF application: " + appNameBeingDownsized);
        executionLogCallback.saveExecutionLog("Failed while downsizing old application: " + appNameBeingDownsized);
      }
    }
  }

  @VisibleForTesting
  void downsizeOldAppDuringDeployIfRequired(ExecutionLogCallback executionLogCallback,
      PcfCommandRouteUpdateRequest pcfCommandRouteUpdateRequest, PcfRequestConfig pcfRequestConfig,
      PcfRouteUpdateRequestConfigData pcfRouteUpdateConfigData) {
    if (!pcfRouteUpdateConfigData.isRollback() && pcfRouteUpdateConfigData.isDownsizeOldApplication()) {
      resizeOldApplications(pcfCommandRouteUpdateRequest, pcfRequestConfig, executionLogCallback, false);
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

    List<String> mapRouteForNewApp = data.isRollback() ? data.getTempRoutes() : data.getFinalRoutes();
    List<String> unmapRouteForNewApp = data.isRollback() ? data.getFinalRoutes() : data.getTempRoutes();
    pcfCommandTaskHelper.mapRouteMaps(
        data.getNewApplicatiaonName(), mapRouteForNewApp, pcfRequestConfig, executionLogCallback);
    pcfCommandTaskHelper.unmapRouteMaps(
        data.getNewApplicatiaonName(), unmapRouteForNewApp, pcfRequestConfig, executionLogCallback);

    if (isNotEmpty(data.getExistingApplicationNames())) {
      List<String> mapRouteForExistingApp = data.isRollback() ? data.getFinalRoutes() : data.getTempRoutes();
      List<String> unmapRouteForExistingApp = data.isRollback() ? data.getTempRoutes() : data.getFinalRoutes();
      for (String existingAppName : data.getExistingApplicationNames()) {
        pcfCommandTaskHelper.mapRouteMaps(
            existingAppName, mapRouteForExistingApp, pcfRequestConfig, executionLogCallback);
        pcfCommandTaskHelper.unmapRouteMaps(
            existingAppName, unmapRouteForExistingApp, pcfRequestConfig, executionLogCallback);
      }
    }
  }
}
