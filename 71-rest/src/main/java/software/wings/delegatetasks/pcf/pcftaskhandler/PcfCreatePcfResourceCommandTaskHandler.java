package software.wings.delegatetasks.pcf.pcftaskhandler;

import static java.util.Collections.emptyList;

import com.google.inject.Singleton;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest.PcfCommandType;
import software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfInfraMappingDataResponse;

import java.util.Arrays;
import java.util.List;

@NoArgsConstructor
@Singleton
@Slf4j
public class PcfCreatePcfResourceCommandTaskHandler extends PcfCommandTaskHandler {
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
      if (PcfCommandType.CREATE_ROUTE == pcfInfraMappingDataRequest.getPcfCommandType()) {
        String routeCreated = pcfDeploymentManager.createRouteMap(
            PcfRequestConfig.builder()
                .orgName(pcfInfraMappingDataRequest.getOrganization())
                .spaceName(pcfInfraMappingDataRequest.getSpace())
                .userName(pcfConfig.getUsername())
                .password(String.valueOf(pcfConfig.getPassword()))
                .endpointUrl(pcfConfig.getEndpointUrl())
                .timeOutIntervalInMins(pcfInfraMappingDataRequest.getTimeoutIntervalInMin())
                .build(),
            pcfInfraMappingDataRequest.getHost(), pcfInfraMappingDataRequest.getDomain(),
            pcfInfraMappingDataRequest.getPath(), pcfInfraMappingDataRequest.isTcpRoute(),
            pcfInfraMappingDataRequest.isUseRandomPort(), pcfInfraMappingDataRequest.getPort());

        pcfInfraMappingDataResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
        pcfInfraMappingDataResponse.setOutput(StringUtils.EMPTY);
        pcfInfraMappingDataResponse.setRouteMaps(Arrays.asList(routeCreated));
      }

    } catch (Exception e) {
      logger.error("Exception in processing Create Route task", e);
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
}
