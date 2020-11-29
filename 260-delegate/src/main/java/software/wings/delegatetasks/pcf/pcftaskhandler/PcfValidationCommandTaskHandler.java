package software.wings.delegatetasks.pcf.pcftaskhandler;

import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;

import com.google.inject.Singleton;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@NoArgsConstructor
@Singleton
@Slf4j
public class PcfValidationCommandTaskHandler extends PcfCommandTaskHandler {
  /**
   * Performs validation of PCF config while adding PCF cloud provider
   *
   * @param pcfCommandRequest
   * @param isInstanceSync
   * @return
   */
  @Override
  public PcfCommandExecutionResponse executeTaskInternal(PcfCommandRequest pcfCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback,
      boolean isInstanceSync) {
    if (!(pcfCommandRequest instanceof PcfInfraMappingDataRequest)) {
      throw new InvalidArgumentsException(
          Pair.of("pcfCommandRequest", "Must be instance of PcfInfraMappingDataRequest"));
    }
    PcfInfraMappingDataRequest pcfInfraMappingDataRequest = (PcfInfraMappingDataRequest) pcfCommandRequest;
    PcfConfig pcfConfig = pcfInfraMappingDataRequest.getPcfConfig();
    encryptionService.decrypt(pcfConfig, encryptedDataDetails, false);

    PcfCommandExecutionResponse pcfCommandExecutionResponse = PcfCommandExecutionResponse.builder().build();
    try {
      pcfDeploymentManager.getOrganizations(
          PcfRequestConfig.builder()
              .orgName(pcfInfraMappingDataRequest.getOrganization())
              .userName(String.valueOf(pcfConfig.getUsername()))
              .password(String.valueOf(pcfConfig.getPassword()))
              .endpointUrl(pcfConfig.getEndpointUrl())
              .limitPcfThreads(pcfInfraMappingDataRequest.isLimitPcfThreads())
              .ignorePcfConnectionContextCache(pcfInfraMappingDataRequest.isIgnorePcfConnectionContextCache())
              .timeOutIntervalInMins(pcfInfraMappingDataRequest.getTimeoutIntervalInMin())
              .build());

      pcfCommandExecutionResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);

    } catch (Exception e) {
      log.error("Exception in processing PCF validation task for Account {} ",
          pcfInfraMappingDataRequest.getPcfConfig().getAccountId(), e);
      pcfCommandExecutionResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
      pcfCommandExecutionResponse.setErrorMessage(ExceptionUtils.getMessage(e));
    }

    return pcfCommandExecutionResponse;
  }
}
