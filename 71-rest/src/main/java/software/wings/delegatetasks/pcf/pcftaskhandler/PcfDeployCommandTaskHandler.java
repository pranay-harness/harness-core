package software.wings.delegatetasks.pcf.pcftaskhandler;

import static io.harness.pcf.model.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX;

import com.google.inject.Singleton;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import software.wings.api.PcfInstanceElement;
import software.wings.api.pcf.PcfServiceData;
import software.wings.beans.PcfConfig;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.request.PcfCommandDeployRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfDeployCommandResponse;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Singleton
@Slf4j
public class PcfDeployCommandTaskHandler extends PcfCommandTaskHandler {
  /**
   * This method is responsible for upsizing new application instances and downsizing previous application instances.
   * @param pcfCommandRequest
   * @param encryptedDataDetails
   * @return
   */
  public PcfCommandExecutionResponse executeTaskInternal(PcfCommandRequest pcfCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    if (!(pcfCommandRequest instanceof PcfCommandDeployRequest)) {
      throw new InvalidArgumentsException(Pair.of("pcfCommandRequest", "Must be instance of PcfCommandDeployRequest"));
    }
    PcfCommandDeployRequest pcfCommandDeployRequest = (PcfCommandDeployRequest) pcfCommandRequest;
    List<PcfServiceData> pcfServiceDataUpdated = new ArrayList<>();
    PcfDeployCommandResponse pcfDeployCommandResponse =
        PcfDeployCommandResponse.builder().pcfInstanceElements(new ArrayList<>()).build();
    try {
      executionLogCallback.saveExecutionLog("\n---------- Starting PCF Resize Command\n");

      PcfConfig pcfConfig = pcfCommandRequest.getPcfConfig();
      encryptionService.decrypt(pcfConfig, encryptedDataDetails);

      PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder()
                                              .userName(pcfConfig.getUsername())
                                              .password(String.valueOf(pcfConfig.getPassword()))
                                              .endpointUrl(pcfConfig.getEndpointUrl())
                                              .orgName(pcfCommandDeployRequest.getOrganization())
                                              .spaceName(pcfCommandDeployRequest.getSpace())
                                              .timeOutIntervalInMins(pcfCommandDeployRequest.getTimeoutIntervalInMin())
                                              .build();

      ApplicationDetail details = pcfCommandTaskHelper.getNewlyCreatedApplication(
          pcfRequestConfig, pcfCommandDeployRequest, pcfDeploymentManager);
      // No of instances to be added to newly created application in this deploy stage
      Integer stepIncrease = pcfCommandDeployRequest.getUpdateCount() - details.getInstances();
      Integer stepDecrease = pcfCommandDeployRequest.getDownSizeCount();

      // downsize previous apps with non zero instances by same count new app was upsized
      List<PcfInstanceElement> pcfInstanceElementsForVerification = new ArrayList<>();
      if (ResizeStrategy.DOWNSIZE_OLD_FIRST.equals(pcfCommandDeployRequest.getResizeStrategy())) {
        pcfCommandTaskHelper.downsizePreviousReleases(pcfCommandDeployRequest, pcfRequestConfig, executionLogCallback,
            pcfServiceDataUpdated, stepDecrease, pcfInstanceElementsForVerification);

        pcfCommandTaskHelper.upsizeNewApplication(executionLogCallback, pcfDeploymentManager, pcfCommandDeployRequest,
            pcfServiceDataUpdated, pcfRequestConfig, details, stepIncrease, pcfInstanceElementsForVerification);
      } else {
        pcfCommandTaskHelper.upsizeNewApplication(executionLogCallback, pcfDeploymentManager, pcfCommandDeployRequest,
            pcfServiceDataUpdated, pcfRequestConfig, details, stepIncrease, pcfInstanceElementsForVerification);

        pcfCommandTaskHelper.downsizePreviousReleases(pcfCommandDeployRequest, pcfRequestConfig, executionLogCallback,
            pcfServiceDataUpdated, stepDecrease, pcfInstanceElementsForVerification);
      }
      // generate response to be sent back to Manager
      pcfDeployCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
      pcfDeployCommandResponse.setOutput(StringUtils.EMPTY);
      pcfDeployCommandResponse.setInstanceDataUpdated(pcfServiceDataUpdated);
      pcfDeployCommandResponse.getPcfInstanceElements().addAll(pcfInstanceElementsForVerification);
      executionLogCallback.saveExecutionLog("\n\n--------- PCF Resize completed successfully");
    } catch (Exception e) {
      logger.error(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "Exception in processing PCF Deploy task [{}]",
          pcfCommandDeployRequest, e);
      executionLogCallback.saveExecutionLog("\n\n--------- PCF Resize failed to complete successfully");
      Misc.logAllMessages(e, executionLogCallback);
      pcfDeployCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
      pcfDeployCommandResponse.setOutput(ExceptionUtils.getMessage(e));
      pcfDeployCommandResponse.setInstanceDataUpdated(pcfServiceDataUpdated);
    }

    return PcfCommandExecutionResponse.builder()
        .commandExecutionStatus(pcfDeployCommandResponse.getCommandExecutionStatus())
        .errorMessage(pcfDeployCommandResponse.getOutput())
        .pcfCommandResponse(pcfDeployCommandResponse)
        .build();
  }
}
