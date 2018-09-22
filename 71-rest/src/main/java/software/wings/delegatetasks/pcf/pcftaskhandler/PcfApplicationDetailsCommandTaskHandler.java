package software.wings.delegatetasks.pcf.pcftaskhandler;

import static java.util.stream.Collectors.toList;

import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfInstanceSyncRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfInstanceSyncResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.Misc;

import java.util.List;

@NoArgsConstructor
@Singleton
public class PcfApplicationDetailsCommandTaskHandler extends PcfCommandTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(PcfApplicationDetailsCommandTaskHandler.class);

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  public PcfCommandExecutionResponse executeTaskInternal(
      PcfCommandRequest pcfCommandRequest, List<EncryptedDataDetail> encryptedDataDetails) {
    PcfCommandExecutionResponse pcfCommandExecutionResponse = PcfCommandExecutionResponse.builder().build();
    PcfInstanceSyncResponse pcfInstanceSyncResponse = PcfInstanceSyncResponse.builder().build();
    pcfCommandExecutionResponse.setPcfCommandResponse(pcfInstanceSyncResponse);
    try {
      PcfConfig pcfConfig = pcfCommandRequest.getPcfConfig();
      encryptionService.decrypt(pcfConfig, encryptedDataDetails);

      PcfInstanceSyncRequest pcfInstanceSyncRequest = (PcfInstanceSyncRequest) pcfCommandRequest;
      PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder()
                                              .timeOutIntervalInMins(5)
                                              .applicationName(pcfInstanceSyncRequest.getPcfApplicationName())
                                              .userName(pcfConfig.getUsername())
                                              .password(String.valueOf(pcfConfig.getPassword()))
                                              .endpointUrl(pcfConfig.getEndpointUrl())
                                              .orgName(pcfCommandRequest.getOrganization())
                                              .spaceName(pcfCommandRequest.getSpace())
                                              .build();

      ApplicationDetail applicationDetail = pcfDeploymentManager.getApplicationByName(pcfRequestConfig);

      pcfInstanceSyncResponse.setGuid(applicationDetail.getId());
      pcfInstanceSyncResponse.setName(applicationDetail.getName());
      pcfInstanceSyncResponse.setOrganization(pcfCommandRequest.getOrganization());
      pcfInstanceSyncResponse.setSpace(pcfCommandRequest.getSpace());
      if (CollectionUtils.isNotEmpty(applicationDetail.getInstanceDetails())) {
        pcfInstanceSyncResponse.setInstanceIndices(applicationDetail.getInstanceDetails()
                                                       .stream()
                                                       .map(instanceDetail -> instanceDetail.getIndex())
                                                       .collect(toList()));
      }

      pcfInstanceSyncResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
      pcfInstanceSyncResponse.setOutput(StringUtils.EMPTY);
    } catch (Exception e) {
      pcfInstanceSyncResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
      pcfInstanceSyncResponse.setOutput(Misc.getMessage(e));
    }

    pcfCommandExecutionResponse.setErrorMessage(pcfInstanceSyncResponse.getOutput());
    pcfCommandExecutionResponse.setCommandExecutionStatus(pcfInstanceSyncResponse.getCommandExecutionStatus());
    pcfCommandExecutionResponse.setPcfCommandResponse(pcfInstanceSyncResponse);

    return pcfCommandExecutionResponse;
  }
}
