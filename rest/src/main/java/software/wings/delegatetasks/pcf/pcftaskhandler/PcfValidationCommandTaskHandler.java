package software.wings.delegatetasks.pcf.pcftaskhandler;

import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.Misc;

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
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  public PcfCommandExecutionResponse executeTaskInternal(
      PcfCommandRequest pcfCommandRequest, List<EncryptedDataDetail> encryptedDataDetails) {
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
      pcfCommandExecutionResponse.setErrorMessage(Misc.getMessage(e));
    }

    return pcfCommandExecutionResponse;
  }
}
