package software.wings.delegatetasks.pcf.pcftaskhandler;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.pcf.model.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;
import static software.wings.beans.command.PcfDummyCommandUnit.Downsize;
import static software.wings.beans.command.PcfDummyCommandUnit.Upsize;
import static software.wings.beans.command.PcfDummyCommandUnit.Wrapup;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.filesystem.FileIo;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.Misc;
import io.harness.pcf.PivotalClientApiException;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.api.PcfInstanceElement;
import software.wings.api.pcf.PcfServiceData;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.request.PcfAppAutoscalarRequestData;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRollbackRequest;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfDeployCommandResponse;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.operations.applications.ApplicationDetail;

@NoArgsConstructor
@Singleton
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(HarnessTeam.CDP)
public class PcfRollbackCommandTaskHandler extends PcfCommandTaskHandler {
  @Override
  public PcfCommandExecutionResponse executeTaskInternal(PcfCommandRequest pcfCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback,
      boolean isInstanceSync) {
    if (!(pcfCommandRequest instanceof PcfCommandRollbackRequest)) {
      throw new InvalidArgumentsException(
          Pair.of("pcfCommandRequest", "Must be instance of PcfCommandRollbackRequest"));
    }

    executionLogCallback = pcfCommandTaskHelper.getLogCallBack(delegateLogService, pcfCommandRequest.getAccountId(),
        pcfCommandRequest.getAppId(), pcfCommandRequest.getActivityId(), Upsize);
    executionLogCallback.saveExecutionLog(color("--------- Starting Rollback deployment", White, Bold));
    List<PcfServiceData> pcfServiceDataUpdated = new ArrayList<>();
    PcfDeployCommandResponse pcfDeployCommandResponse =
        PcfDeployCommandResponse.builder().pcfInstanceElements(new ArrayList<>()).build();

    PcfCommandRollbackRequest commandRollbackRequest = (PcfCommandRollbackRequest) pcfCommandRequest;

    File workingDirectory = null;
    Exception exception = null;
    try {
      // This will be CF_HOME for any cli related operations
      workingDirectory = pcfCommandTaskHelper.generateWorkingDirectoryForDeployment();

      PcfConfig pcfConfig = pcfCommandRequest.getPcfConfig();
      encryptionService.decrypt(pcfConfig, encryptedDataDetails, false);
      if (CollectionUtils.isEmpty(commandRollbackRequest.getInstanceData())) {
        commandRollbackRequest.setInstanceData(new ArrayList<>());
      }

      PcfRequestConfig pcfRequestConfig =
          PcfRequestConfig.builder()
              .userName(String.valueOf(pcfConfig.getUsername()))
              .password(String.valueOf(pcfConfig.getPassword()))
              .endpointUrl(pcfConfig.getEndpointUrl())
              .orgName(commandRollbackRequest.getOrganization())
              .spaceName(commandRollbackRequest.getSpace())
              .timeOutIntervalInMins(commandRollbackRequest.getTimeoutIntervalInMin() == null
                      ? 10
                      : commandRollbackRequest.getTimeoutIntervalInMin())
              .cfHomeDirPath(workingDirectory.getAbsolutePath())
              .useCFCLI(commandRollbackRequest.isUseCfCLI())
              .cfCliPath(pcfCommandTaskHelper.getCfCliPathOnDelegate(
                  pcfCommandRequest.isUseCfCLI(), pcfCommandRequest.getCfCliVersion()))
              .cfCliVersion(pcfCommandRequest.getCfCliVersion())
              .limitPcfThreads(commandRollbackRequest.isLimitPcfThreads())
              .ignorePcfConnectionContextCache(commandRollbackRequest.isIgnorePcfConnectionContextCache())
              .build();

      // Will be used if app autoscalar is configured
      PcfAppAutoscalarRequestData autoscalarRequestData =
          PcfAppAutoscalarRequestData.builder()
              .pcfRequestConfig(pcfRequestConfig)
              .configPathVar(workingDirectory.getAbsolutePath())
              .timeoutInMins(commandRollbackRequest.getTimeoutIntervalInMin() != null
                      ? commandRollbackRequest.getTimeoutIntervalInMin()
                      : 10)
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
      // During rollback, always upsize old ones
      pcfCommandTaskHelper.upsizeListOfInstances(executionLogCallback, pcfDeploymentManager, pcfServiceDataUpdated,
          pcfRequestConfig, upsizeList, pcfInstanceElements);
      restoreRoutesForOldApplication(commandRollbackRequest, pcfRequestConfig, executionLogCallback);
      // Enable autoscalar for older app, if it was disabled during deploy
      enableAutoscalarIfNeeded(upsizeList, autoscalarRequestData, executionLogCallback);
      executionLogCallback.saveExecutionLog("#---------- Upsize Application Successfully Completed", INFO, SUCCESS);

      executionLogCallback = pcfCommandTaskHelper.getLogCallBack(delegateLogService, pcfCommandRequest.getAccountId(),
          pcfCommandRequest.getAppId(), pcfCommandRequest.getActivityId(), Downsize);
      pcfCommandTaskHelper.downSizeListOfInstances(executionLogCallback, pcfServiceDataUpdated, pcfRequestConfig,
          downSizeList, commandRollbackRequest, autoscalarRequestData);
      unmapRoutesFromNewAppAfterDownsize(executionLogCallback, commandRollbackRequest, pcfRequestConfig);

      pcfDeployCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
      pcfDeployCommandResponse.setOutput(StringUtils.EMPTY);
      pcfDeployCommandResponse.setInstanceDataUpdated(pcfServiceDataUpdated);
      pcfDeployCommandResponse.getPcfInstanceElements().addAll(pcfInstanceElements);
      executionLogCallback.saveExecutionLog("\n\n--------- PCF Rollback completed successfully", INFO, SUCCESS);
    } catch (IOException | PivotalClientApiException e) {
      exception = e;
      logExceptionMessage(executionLogCallback, commandRollbackRequest, exception);
    } catch (Exception ex) {
      exception = ex;
      logExceptionMessage(executionLogCallback, commandRollbackRequest, exception);
    } finally {
      executionLogCallback = pcfCommandTaskHelper.getLogCallBack(delegateLogService, pcfCommandRequest.getAccountId(),
          pcfCommandRequest.getAppId(), pcfCommandRequest.getActivityId(), Wrapup);
      executionLogCallback.saveExecutionLog("#------- Deleting Temporary Files");
      if (workingDirectory != null) {
        try {
          FileIo.deleteDirectoryAndItsContentIfExists(workingDirectory.getAbsolutePath());
          executionLogCallback.saveExecutionLog("Temporary Files Successfully deleted", INFO, SUCCESS);
        } catch (IOException e) {
          log.warn("Failed to delete temp cf home folder", e);
        }
      }
    }

    if (exception != null) {
      pcfDeployCommandResponse.setCommandExecutionStatus(FAILURE);
      pcfDeployCommandResponse.setInstanceDataUpdated(pcfServiceDataUpdated);
      pcfDeployCommandResponse.setOutput(ExceptionUtils.getMessage(exception));
    }

    return PcfCommandExecutionResponse.builder()
        .commandExecutionStatus(pcfDeployCommandResponse.getCommandExecutionStatus())
        .errorMessage(pcfDeployCommandResponse.getOutput())
        .pcfCommandResponse(pcfDeployCommandResponse)
        .build();
  }

  private void logExceptionMessage(ExecutionLogCallback executionLogCallback,
      PcfCommandRollbackRequest commandRollbackRequest, Exception exception) {
    log.error(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "Exception in processing PCF Rollback task [{}]",
        commandRollbackRequest, exception);
    executionLogCallback.saveExecutionLog("\n\n--------- PCF Rollback failed to complete successfully", ERROR, FAILURE);
    Misc.logAllMessages(exception, executionLogCallback);
  }

  @VisibleForTesting
  void enableAutoscalarIfNeeded(List<PcfServiceData> upsizeList, PcfAppAutoscalarRequestData autoscalarRequestData,
      ExecutionLogCallback logCallback) throws PivotalClientApiException {
    for (PcfServiceData pcfServiceData : upsizeList) {
      if (!pcfServiceData.isDisableAutoscalarPerformed()) {
        continue;
      }

      autoscalarRequestData.setApplicationName(pcfServiceData.getName());
      autoscalarRequestData.setApplicationGuid(pcfServiceData.getId());
      autoscalarRequestData.setExpectedEnabled(false);
      pcfDeploymentManager.changeAutoscalarState(autoscalarRequestData, logCallback, true);
    }
  }

  /**
   * This is for non BG deployment.
   * Older app will be mapped to routes it was originally mapped to.
   * In deploy state, once older app is downsized to 0, we remove routeMaps,
   * this step will restore them.
   */
  @VisibleForTesting
  void restoreRoutesForOldApplication(PcfCommandRollbackRequest commandRollbackRequest,
      PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    if (commandRollbackRequest.isStandardBlueGreenWorkflow()
        || EmptyPredicate.isEmpty(commandRollbackRequest.getAppsToBeDownSized())) {
      return;
    }

    PcfAppSetupTimeDetails pcfAppSetupTimeDetails = commandRollbackRequest.getAppsToBeDownSized().get(0);

    if (pcfAppSetupTimeDetails != null) {
      pcfRequestConfig.setApplicationName(pcfAppSetupTimeDetails.getApplicationName());
      ApplicationDetail applicationDetail = pcfDeploymentManager.getApplicationByName(pcfRequestConfig);

      if (EmptyPredicate.isEmpty(pcfAppSetupTimeDetails.getUrls())) {
        return;
      }

      if (EmptyPredicate.isEmpty(applicationDetail.getUrls())
          || !pcfAppSetupTimeDetails.getUrls().containsAll(applicationDetail.getUrls())) {
        pcfCommandTaskHelper.mapRouteMaps(pcfAppSetupTimeDetails.getApplicationName(), pcfAppSetupTimeDetails.getUrls(),
            pcfRequestConfig, executionLogCallback);
      }
    }
  }

  @VisibleForTesting
  void unmapRoutesFromNewAppAfterDownsize(ExecutionLogCallback executionLogCallback,
      PcfCommandRollbackRequest commandRollbackRequest, PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException {
    if (commandRollbackRequest.isStandardBlueGreenWorkflow()
        || commandRollbackRequest.getNewApplicationDetails() == null
        || isBlank(commandRollbackRequest.getNewApplicationDetails().getApplicationName())) {
      return;
    }

    pcfRequestConfig.setApplicationName(commandRollbackRequest.getNewApplicationDetails().getApplicationName());
    ApplicationDetail appDetail = pcfDeploymentManager.getApplicationByName(pcfRequestConfig);

    if (appDetail.getInstances() == 0) {
      pcfCommandTaskHelper.unmapExistingRouteMaps(appDetail, pcfRequestConfig, executionLogCallback);
    }
  }
}
