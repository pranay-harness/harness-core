package software.wings.delegatetasks.pcf.pcftaskhandler;

import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.data.structure.EmptyPredicate;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.PivotalClientApiException;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRouteUpdateRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfCommandResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.Misc;

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
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  public PcfCommandExecutionResponse executeTaskInternal(
      PcfCommandRequest pcfCommandRequest, List<EncryptedDataDetail> encryptedDataDetails) {
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

      if (pcfCommandRouteUpdateRequest.isMapRoutesOperation()) {
        mapRouteMaps(pcfCommandRouteUpdateRequest, pcfRequestConfig);
      } else {
        unmapRouteMaps(pcfCommandRouteUpdateRequest, pcfRequestConfig);
      }
      executionLogCallback.saveExecutionLog("\n--------- PCF Route Update completed successfully");
      pcfCommandResponse.setOutput(StringUtils.EMPTY);
      pcfCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
    } catch (Exception e) {
      logger.error("Exception in processing PCF Route Update task", e);
      executionLogCallback.saveExecutionLog("\n\n--------- PCF Route Update failed to complete successfully");
      Misc.logAllMessages(e, executionLogCallback);
      pcfCommandResponse.setOutput(Misc.getMessage(e));
      pcfCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
    }

    pcfCommandExecutionResponse.setCommandExecutionStatus(pcfCommandResponse.getCommandExecutionStatus());
    pcfCommandExecutionResponse.setErrorMessage(pcfCommandResponse.getOutput());
    return pcfCommandExecutionResponse;
  }

  private void mapRouteMaps(PcfCommandRouteUpdateRequest pcfCommandRouteUpdateRequest,
      PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    if (CollectionUtils.isNotEmpty(pcfCommandRouteUpdateRequest.getAppsToBeUpdated())) {
      for (String applicationName : pcfCommandRouteUpdateRequest.getAppsToBeUpdated()) {
        executionLogCallback.saveExecutionLog("\n# Adding Routs");
        executionLogCallback.saveExecutionLog("APPLICATION: " + applicationName);
        executionLogCallback.saveExecutionLog(
            "ROUTES: \n[" + getRouteString(pcfCommandRouteUpdateRequest.getRouteMaps()));
        // map
        pcfRequestConfig.setApplicationName(applicationName);
        pcfDeploymentManager.mapRouteMapForApplication(pcfRequestConfig, pcfCommandRouteUpdateRequest.getRouteMaps());
      }
    }
  }

  private void unmapRouteMaps(PcfCommandRouteUpdateRequest pcfCommandRouteUpdateRequest,
      PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    if (CollectionUtils.isNotEmpty(pcfCommandRouteUpdateRequest.getAppsToBeUpdated())) {
      for (String applicationName : pcfCommandRouteUpdateRequest.getAppsToBeUpdated()) {
        executionLogCallback.saveExecutionLog("\n# Unmapping Routes");
        executionLogCallback.saveExecutionLog("APPLICATION: " + applicationName);
        executionLogCallback.saveExecutionLog(
            "ROUTES: \n[" + getRouteString(pcfCommandRouteUpdateRequest.getRouteMaps()));
        // unmap
        pcfRequestConfig.setApplicationName(applicationName);
        pcfDeploymentManager.unmapRouteMapForApplication(pcfRequestConfig, pcfCommandRouteUpdateRequest.getRouteMaps());
      }
      executionLogCallback.saveExecutionLog("# Unmapping Routes was successfully completed");
    }
  }

  private String getRouteString(List<String> routeMaps) {
    if (EmptyPredicate.isEmpty(routeMaps)) {
      return StringUtils.EMPTY;
    }

    StringBuilder builder = new StringBuilder();
    routeMaps.forEach(routeMap -> builder.append("\n").append(routeMap));
    builder.append("\n]");
    return builder.toString();
  }
}
