package software.wings.delegatetasks.pcf.pcftaskhandler;

import com.google.inject.Singleton;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@NoArgsConstructor
@Singleton
public class PcfValidationCommandTaskHandler extends PcfCommandTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(PcfValidationCommandTaskHandler.class);

  /**
   * Performs validation of PCF config while adding PCF cloud provider
   * @param pcfCommandRequest
   * @return
   */
  public PcfCommandExecutionResponse executeTaskInternal(PcfCommandRequest pcfCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    if (!(pcfCommandRequest instanceof PcfInfraMappingDataRequest)) {
      throw new InvalidArgumentsException(
          Pair.of("pcfCommandRequest", "Must be instance of PcfInfraMappingDataRequest"));
    }
    PcfInfraMappingDataRequest pcfInfraMappingDataRequest = (PcfInfraMappingDataRequest) pcfCommandRequest;
    PcfConfig pcfConfig = pcfInfraMappingDataRequest.getPcfConfig();
    PcfCommandExecutionResponse pcfCommandExecutionResponse = PcfCommandExecutionResponse.builder().build();
    try {
      pcfDeploymentManager.getOrganizations(
          PcfRequestConfig.builder()
              .orgName(pcfInfraMappingDataRequest.getOrganization())
              .userName(pcfConfig.getUsername())
              .password(String.valueOf(pcfConfig.getPassword()))
              .endpointUrl(pcfConfig.getEndpointUrl())
              .timeOutIntervalInMins(pcfInfraMappingDataRequest.getTimeoutIntervalInMin())
              .build());

      pcfCommandExecutionResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);

    } catch (Exception e) {
      logger.error("Exception in processing PCF validation task for Account {} ",
          pcfInfraMappingDataRequest.getPcfConfig().getAccountId(), e);
      pcfCommandExecutionResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
      pcfCommandExecutionResponse.setErrorMessage(ExceptionUtils.getMessage(e));
    }

    return pcfCommandExecutionResponse;
  }
}
