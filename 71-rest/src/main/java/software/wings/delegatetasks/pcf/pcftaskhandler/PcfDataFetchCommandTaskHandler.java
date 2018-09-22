package software.wings.delegatetasks.pcf.pcftaskhandler;

import static java.util.Collections.emptyList;

import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.PivotalClientApiException;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfInfraMappingDataResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.Misc;

import java.util.List;

@NoArgsConstructor
@Singleton
public class PcfDataFetchCommandTaskHandler extends PcfCommandTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(PcfDataFetchCommandTaskHandler.class);

  /**
   * Fetches Organization, Spaces, RouteMap data
   */
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  public PcfCommandExecutionResponse executeTaskInternal(
      PcfCommandRequest pcfCommandRequest, List<EncryptedDataDetail> encryptedDataDetails) {
    PcfInfraMappingDataRequest pcfInfraMappingDataRequest = (PcfInfraMappingDataRequest) pcfCommandRequest;
    PcfConfig pcfConfig = pcfInfraMappingDataRequest.getPcfConfig();
    encryptionService.decrypt(pcfConfig, encryptedDataDetails);

    PcfCommandExecutionResponse pcfCommandExecutionResponse = PcfCommandExecutionResponse.builder().build();
    PcfInfraMappingDataResponse pcfInfraMappingDataResponse = PcfInfraMappingDataResponse.builder().build();
    pcfCommandExecutionResponse.setPcfCommandResponse(pcfInfraMappingDataResponse);

    try {
      if (StringUtils.isBlank(pcfInfraMappingDataRequest.getOrganization())) {
        getOrgs(pcfDeploymentManager, pcfInfraMappingDataRequest, pcfInfraMappingDataResponse, pcfConfig);
      } else if (StringUtils.isBlank(pcfInfraMappingDataRequest.getSpace())) {
        getSpaces(pcfDeploymentManager, pcfInfraMappingDataRequest, pcfInfraMappingDataResponse, pcfConfig);
      } else {
        getRoutes(pcfDeploymentManager, pcfInfraMappingDataRequest, pcfInfraMappingDataResponse, pcfConfig);
      }

      pcfInfraMappingDataResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
      pcfInfraMappingDataResponse.setOutput(StringUtils.EMPTY);
    } catch (Exception e) {
      logger.error("Exception in processing PCF DataFetch task", e);
      pcfInfraMappingDataResponse.setOrganizations(emptyList());
      pcfInfraMappingDataResponse.setSpaces(emptyList());
      pcfInfraMappingDataResponse.setRouteMaps(emptyList());
      pcfInfraMappingDataResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
      pcfInfraMappingDataResponse.setOutput(Misc.getMessage(e));
    }

    pcfCommandExecutionResponse.setCommandExecutionStatus(pcfInfraMappingDataResponse.getCommandExecutionStatus());
    pcfCommandExecutionResponse.setErrorMessage(pcfInfraMappingDataResponse.getOutput());
    return pcfCommandExecutionResponse;
  }

  private void getRoutes(PcfDeploymentManager pcfDeploymentManager,
      PcfInfraMappingDataRequest pcfInfraMappingDataRequest, PcfInfraMappingDataResponse pcfInfraMappingDataResponse,
      PcfConfig pcfConfig) throws PivotalClientApiException {
    List<String> routes = pcfDeploymentManager.getRouteMaps(
        PcfRequestConfig.builder()
            .orgName(pcfInfraMappingDataRequest.getOrganization())
            .spaceName(pcfInfraMappingDataRequest.getSpace())
            .userName(pcfConfig.getUsername())
            .password(String.valueOf(pcfConfig.getPassword()))
            .endpointUrl(pcfConfig.getEndpointUrl())
            .timeOutIntervalInMins(pcfInfraMappingDataRequest.getTimeoutIntervalInMin())
            .build());

    pcfInfraMappingDataResponse.setRouteMaps(routes);
  }

  private void getSpaces(PcfDeploymentManager pcfDeploymentManager,
      PcfInfraMappingDataRequest pcfInfraMappingDataRequest, PcfInfraMappingDataResponse pcfInfraMappingDataResponse,
      PcfConfig pcfConfig) throws PivotalClientApiException {
    List<String> spaces = pcfDeploymentManager.getSpacesForOrganization(
        PcfRequestConfig.builder()
            .orgName(pcfInfraMappingDataRequest.getOrganization())
            .spaceName(pcfInfraMappingDataRequest.getSpace())
            .userName(pcfConfig.getUsername())
            .password(String.valueOf(pcfConfig.getPassword()))
            .endpointUrl(pcfConfig.getEndpointUrl())
            .timeOutIntervalInMins(pcfInfraMappingDataRequest.getTimeoutIntervalInMin())
            .build());

    pcfInfraMappingDataResponse.setSpaces(spaces);
  }

  private void getOrgs(PcfDeploymentManager pcfDeploymentManager, PcfInfraMappingDataRequest pcfInfraMappingDataRequest,
      PcfInfraMappingDataResponse pcfInfraMappingDataResponse, PcfConfig pcfConfig) throws PivotalClientApiException {
    List<String> orgs = pcfDeploymentManager.getOrganizations(
        PcfRequestConfig.builder()
            .orgName(pcfInfraMappingDataRequest.getOrganization())
            .userName(pcfConfig.getUsername())
            .password(String.valueOf(pcfConfig.getPassword()))
            .endpointUrl(pcfConfig.getEndpointUrl())
            .timeOutIntervalInMins(pcfInfraMappingDataRequest.getTimeoutIntervalInMin())
            .build());

    pcfInfraMappingDataResponse.setOrganizations(orgs);
  }
}
