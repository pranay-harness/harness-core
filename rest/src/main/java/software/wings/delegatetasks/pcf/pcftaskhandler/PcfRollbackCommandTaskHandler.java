package software.wings.delegatetasks.pcf.pcftaskhandler;

import static java.util.stream.Collectors.toList;
import static software.wings.helpers.ext.pcf.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX;

import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PcfInstanceElement;
import software.wings.api.pcf.PcfServiceData;
import software.wings.beans.PcfConfig;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRollbackRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfDeployCommandResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Singleton
public class PcfRollbackCommandTaskHandler extends PcfCommandTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(PcfRollbackCommandTaskHandler.class);

  /**
   * This method performs Rollback operation
   * @param pcfCommandRequest
   * @param encryptedDataDetails
   * @return
   */
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  public PcfCommandExecutionResponse executeTaskInternal(
      PcfCommandRequest pcfCommandRequest, List<EncryptedDataDetail> encryptedDataDetails) {
    executionLogCallback.saveExecutionLog("--------- Starting Rollback deployment");
    List<PcfServiceData> pcfServiceDataUpdated = new ArrayList<>();
    PcfDeployCommandResponse pcfDeployCommandResponse =
        PcfDeployCommandResponse.builder().pcfInstanceElements(new ArrayList<>()).build();

    PcfCommandRollbackRequest commandRollbackRequest = (PcfCommandRollbackRequest) pcfCommandRequest;

    try {
      PcfConfig pcfConfig = pcfCommandRequest.getPcfConfig();
      encryptionService.decrypt(pcfConfig, encryptedDataDetails);
      if (CollectionUtils.isEmpty(commandRollbackRequest.getInstanceData())) {
        commandRollbackRequest.setInstanceData(new ArrayList<>());
      }

      PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder()
                                              .userName(pcfConfig.getUsername())
                                              .password(String.valueOf(pcfConfig.getPassword()))
                                              .endpointUrl(pcfConfig.getEndpointUrl())
                                              .orgName(commandRollbackRequest.getOrganization())
                                              .spaceName(commandRollbackRequest.getSpace())
                                              .timeOutIntervalInMins(commandRollbackRequest.getTimeoutIntervalInMin())
                                              .build();

      // get Upsize Instance data
      List<PcfServiceData> upsizeList =
          commandRollbackRequest.getInstanceData()
              .stream()
              .filter(pcfServiceData -> pcfServiceData.getDesiredCount() > pcfServiceData.getPreviousCount())
              .collect(toList());

      // get Downsize Instance data
      List<PcfServiceData> downSizeList =
          commandRollbackRequest.getInstanceData()
              .stream()
              .filter(pcfServiceData -> pcfServiceData.getDesiredCount() < pcfServiceData.getPreviousCount())
              .collect(toList());

      List<PcfInstanceElement> pcfInstanceElements = new ArrayList<>();
      if (ResizeStrategy.DOWNSIZE_OLD_FIRST.equals(commandRollbackRequest.getResizeStrategy())) {
        pcfCommandTaskHelper.downSizeListOfInstances(executionLogCallback, pcfDeploymentManager, pcfServiceDataUpdated,
            pcfRequestConfig, downSizeList, commandRollbackRequest.getRouteMaps());
        pcfCommandTaskHelper.upsizeListOfInstances(executionLogCallback, pcfDeploymentManager, pcfServiceDataUpdated,
            pcfRequestConfig, upsizeList, pcfInstanceElements, commandRollbackRequest.getRouteMaps());
      } else {
        pcfCommandTaskHelper.upsizeListOfInstances(executionLogCallback, pcfDeploymentManager, pcfServiceDataUpdated,
            pcfRequestConfig, upsizeList, pcfInstanceElements, commandRollbackRequest.getRouteMaps());
        pcfCommandTaskHelper.downSizeListOfInstances(executionLogCallback, pcfDeploymentManager, pcfServiceDataUpdated,
            pcfRequestConfig, downSizeList, commandRollbackRequest.getRouteMaps());
      }

      pcfDeployCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
      pcfDeployCommandResponse.setOutput(StringUtils.EMPTY);
      pcfDeployCommandResponse.setInstanceDataUpdated(pcfServiceDataUpdated);
      pcfDeployCommandResponse.getPcfInstanceElements().addAll(pcfInstanceElements);
      executionLogCallback.saveExecutionLog("\n\n--------- PCF Rollback completed successfully");
    } catch (Exception e) {
      logger.error(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "Exception in processing PCF Rollback task [{}]",
          commandRollbackRequest, e);
      executionLogCallback.saveExecutionLog("\n\n--------- PCF Rollback failed to complete successfully");
      Misc.logAllMessages(e, executionLogCallback);
      pcfDeployCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
      pcfDeployCommandResponse.setInstanceDataUpdated(pcfServiceDataUpdated);
      pcfDeployCommandResponse.setOutput(Misc.getMessage(e));
    }

    return PcfCommandExecutionResponse.builder()
        .commandExecutionStatus(pcfDeployCommandResponse.getCommandExecutionStatus())
        .errorMessage(pcfDeployCommandResponse.getOutput())
        .pcfCommandResponse(pcfDeployCommandResponse)
        .build();
  }
}
