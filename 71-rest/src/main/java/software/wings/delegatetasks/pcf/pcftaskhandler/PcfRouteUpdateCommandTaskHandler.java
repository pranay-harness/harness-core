package software.wings.delegatetasks.pcf.pcftaskhandler;

import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidArgumentsException;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.PivotalClientApiException;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRouteUpdateRequest;
import software.wings.helpers.ext.pcf.request.PcfRouteUpdateRequestConfigData;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfCommandResponse;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@NoArgsConstructor
@Singleton
public class PcfRouteUpdateCommandTaskHandler extends PcfCommandTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(PcfRouteUpdateCommandTaskHandler.class);

  /**
   * Performs RouteSwapping for Blue-Green deployment
   * @param pcfCommandRequest
   * @param encryptedDataDetails
   * @return
   */
  public PcfCommandExecutionResponse executeTaskInternal(
      PcfCommandRequest pcfCommandRequest, List<EncryptedDataDetail> encryptedDataDetails) {
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

      if (pcfCommandRouteUpdateRequest.getPcfRouteUpdateConfigData().isStandardBlueGreen()) {
        performRouteUpdateForStandardBlueGreen(pcfCommandRouteUpdateRequest, pcfRequestConfig);
        resizeOldApplicationsIfRequired(pcfCommandRouteUpdateRequest, pcfRequestConfig, executionLogCallback);
      } else {
        performRouteUpdateForSimulatedBlueGreen(pcfCommandRouteUpdateRequest, pcfRequestConfig);
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

  private void resizeOldApplicationsIfRequired(PcfCommandRouteUpdateRequest pcfCommandRouteUpdateRequest,
      PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback) {
    PcfRouteUpdateRequestConfigData pcfRouteUpdateConfigData =
        pcfCommandRouteUpdateRequest.getPcfRouteUpdateConfigData();

    if (!pcfRouteUpdateConfigData.isDownsizeOldApplication()
        || EmptyPredicate.isEmpty(pcfRouteUpdateConfigData.getExistingApplicationNames())) {
      return;
    }

    if (pcfRouteUpdateConfigData.isRollback()) {
      executionLogCallback.saveExecutionLog("\n# Resizing Old Apps to original count as a part of Rollback");
    } else {
      executionLogCallback.saveExecutionLog("\n# Resizing Old Apps to 0 count as configured");
    }

    String appNameBeingDownsized = null;
    for (PcfAppSetupTimeDetails appDetail : pcfRouteUpdateConfigData.getExistingApplicationDetails()) {
      try {
        appNameBeingDownsized = appDetail.getApplicationName();
        int count = pcfRouteUpdateConfigData.isRollback() ? appDetail.getInitialInstanceCount() : 0;

        pcfRequestConfig.setApplicationName(appNameBeingDownsized);
        pcfRequestConfig.setDesiredCount(count);
        executionLogCallback.saveExecutionLog(new StringBuilder()
                                                  .append("Resizing Application: {")
                                                  .append(appNameBeingDownsized)
                                                  .append("} to count: ")
                                                  .append(count)
                                                  .toString());
        pcfDeploymentManager.resizeApplication(pcfRequestConfig);
      } catch (Exception e) {
        logger.error("Failed to downsize PCF application: " + appNameBeingDownsized);
        executionLogCallback.saveExecutionLog("Failed while downsizing old application: " + appNameBeingDownsized);
      }
    }
  }

  private void performRouteUpdateForSimulatedBlueGreen(PcfCommandRouteUpdateRequest pcfCommandRouteUpdateRequest,
      PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
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
      PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    PcfRouteUpdateRequestConfigData data = pcfCommandRouteUpdateRequest.getPcfRouteUpdateConfigData();

    List<String> mapRouteForNewApp = data.isRollback() ? data.getTempRoutes() : data.getFinalRoutes();
    List<String> unmapRouteForNewApp = data.isRollback() ? data.getFinalRoutes() : data.getTempRoutes();
    pcfCommandTaskHelper.mapRouteMaps(
        data.getNewApplicatiaonName(), mapRouteForNewApp, pcfRequestConfig, executionLogCallback);
    pcfCommandTaskHelper.unmapRouteMaps(
        data.getNewApplicatiaonName(), unmapRouteForNewApp, pcfRequestConfig, executionLogCallback);

    if (EmptyPredicate.isNotEmpty(data.getExistingApplicationNames())) {
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
