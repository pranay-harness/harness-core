package software.wings.delegatetasks.pcf.pcftaskhandler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;

import com.google.inject.Singleton;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.PivotalClientApiException;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfInfraMappingDataResponse;

import java.util.List;

@NoArgsConstructor
@Singleton
@Slf4j
public class PcfDataFetchCommandTaskHandler extends PcfCommandTaskHandler {
  /**
   * Fetches Organization, Spaces, RouteMap data
   */
  @Override
  public PcfCommandExecutionResponse executeTaskInternal(PcfCommandRequest pcfCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    if (!(pcfCommandRequest instanceof PcfInfraMappingDataRequest)) {
      throw new InvalidArgumentsException(
          Pair.of("pcfCommandRequest", "Must be instance of PcfInfraMappingDataRequest"));
    }
    PcfInfraMappingDataRequest pcfInfraMappingDataRequest = (PcfInfraMappingDataRequest) pcfCommandRequest;
    PcfConfig pcfConfig = pcfInfraMappingDataRequest.getPcfConfig();
    encryptionService.decrypt(pcfConfig, encryptedDataDetails);

    PcfCommandExecutionResponse pcfCommandExecutionResponse = PcfCommandExecutionResponse.builder().build();
    PcfInfraMappingDataResponse pcfInfraMappingDataResponse = PcfInfraMappingDataResponse.builder().build();
    pcfCommandExecutionResponse.setPcfCommandResponse(pcfInfraMappingDataResponse);

    try {
      switch (pcfInfraMappingDataRequest.getActionType()) {
        case FETCH_ORG:
          getOrgs(pcfDeploymentManager, pcfInfraMappingDataRequest, pcfInfraMappingDataResponse, pcfConfig);
          break;

        case FETCH_SPACE:
          getSpaces(pcfDeploymentManager, pcfInfraMappingDataRequest, pcfInfraMappingDataResponse, pcfConfig);
          break;

        case FETCH_ROUTE:
          getRoutes(pcfDeploymentManager, pcfInfraMappingDataRequest, pcfInfraMappingDataResponse, pcfConfig);
          break;

        case RUNNING_COUNT:
          getRunningCount(pcfDeploymentManager, pcfInfraMappingDataRequest, pcfInfraMappingDataResponse, pcfConfig);
          break;

        default:
          throw new WingsException(
              ErrorCode.INVALID_ARGUMENT, "Invalid ActionType: " + pcfInfraMappingDataRequest.getActionType())
              .addParam("message", "Invalid ActionType: " + pcfInfraMappingDataRequest.getActionType());
      }

      pcfInfraMappingDataResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
      pcfInfraMappingDataResponse.setOutput(StringUtils.EMPTY);
    } catch (Exception e) {
      logger.error("Exception in processing PCF DataFetch task", e);
      pcfInfraMappingDataResponse.setOrganizations(emptyList());
      pcfInfraMappingDataResponse.setSpaces(emptyList());
      pcfInfraMappingDataResponse.setRouteMaps(emptyList());
      pcfInfraMappingDataResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
      pcfInfraMappingDataResponse.setOutput(ExceptionUtils.getMessage(e));
    }

    pcfCommandExecutionResponse.setCommandExecutionStatus(pcfInfraMappingDataResponse.getCommandExecutionStatus());
    pcfCommandExecutionResponse.setErrorMessage(pcfInfraMappingDataResponse.getOutput());
    return pcfCommandExecutionResponse;
  }

  private void getRunningCount(PcfDeploymentManager pcfDeploymentManager,
      PcfInfraMappingDataRequest pcfInfraMappingDataRequest, PcfInfraMappingDataResponse pcfInfraMappingDataResponse,
      PcfConfig pcfConfig) throws PivotalClientApiException {
    Integer count = Integer.valueOf(0);

    List<ApplicationSummary> applicationSummaries = pcfDeploymentManager.getPreviousReleases(
        PcfRequestConfig.builder()
            .endpointUrl(pcfConfig.getEndpointUrl())
            .orgName(pcfInfraMappingDataRequest.getOrganization())
            .spaceName(pcfInfraMappingDataRequest.getSpace())
            .userName(pcfConfig.getUsername())
            .password(String.valueOf(pcfConfig.getPassword()))
            .timeOutIntervalInMins(pcfInfraMappingDataRequest.getTimeoutIntervalInMin())
            .build(),
        pcfInfraMappingDataRequest.getApplicationNamePrefix());

    applicationSummaries = applicationSummaries.stream()
                               .filter(applicationSummary
                                   -> applicationSummary.getRunningInstances() > 0
                                       && !"STOPPED".equals(applicationSummary.getRequestedState()))
                               .collect(toList());

    applicationSummaries =
        applicationSummaries.stream()
            .sorted(comparingInt(
                applicationSummary -> pcfCommandTaskHelper.getRevisionFromReleaseName(applicationSummary.getName())))
            .collect(toList());

    if (isNotEmpty(applicationSummaries)) {
      count = applicationSummaries.get(applicationSummaries.size() - 1).getRunningInstances();
    }

    pcfInfraMappingDataResponse.setRunningInstanceCount(count);
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
