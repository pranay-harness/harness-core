package software.wings.delegatetasks.pcf.pcftaskhandler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.pcf.model.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Log.LogColor.White;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;
import static software.wings.beans.ResizeStrategy.DOWNSIZE_OLD_FIRST;
import static software.wings.beans.command.PcfDummyCommandUnit.Downsize;
import static software.wings.beans.command.PcfDummyCommandUnit.Upsize;
import static software.wings.beans.command.PcfDummyCommandUnit.Wrapup;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.filesystem.FileIo;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.Misc;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import software.wings.api.PcfInstanceElement;
import software.wings.api.pcf.PcfServiceData;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.PivotalClientApiException;
import software.wings.helpers.ext.pcf.request.PcfAppAutoscalarRequestData;
import software.wings.helpers.ext.pcf.request.PcfCommandDeployRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfDeployCommandResponse;

import java.io.File;
import java.io.IOException;
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
  @Override
  public PcfCommandExecutionResponse executeTaskInternal(PcfCommandRequest pcfCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    if (!(pcfCommandRequest instanceof PcfCommandDeployRequest)) {
      throw new InvalidArgumentsException(Pair.of("pcfCommandRequest", "Must be instance of PcfCommandDeployRequest"));
    }
    PcfCommandDeployRequest pcfCommandDeployRequest = (PcfCommandDeployRequest) pcfCommandRequest;
    List<PcfServiceData> pcfServiceDataUpdated = new ArrayList<>();
    PcfDeployCommandResponse pcfDeployCommandResponse =
        PcfDeployCommandResponse.builder().pcfInstanceElements(new ArrayList<>()).build();

    File workingDirectory = null;
    boolean exceptionOccured = false;
    Exception exception = null;
    PcfAppAutoscalarRequestData pcfAppAutoscalarRequestData = PcfAppAutoscalarRequestData.builder().build();
    try {
      boolean downSize = DOWNSIZE_OLD_FIRST == pcfCommandDeployRequest.getResizeStrategy();
      String commandUnitType = downSize ? Downsize : Upsize;
      executionLogCallback = pcfCommandTaskHelper.getLogCallBack(delegateLogService, pcfCommandRequest.getAccountId(),
          pcfCommandRequest.getAppId(), pcfCommandRequest.getActivityId(), commandUnitType);
      executionLogCallback.saveExecutionLog(color("\n---------- Starting PCF Resize Command\n", White, Bold));

      PcfConfig pcfConfig = pcfCommandRequest.getPcfConfig();
      encryptionService.decrypt(pcfConfig, encryptedDataDetails);

      PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder()
                                              .userName(pcfConfig.getUsername())
                                              .password(String.valueOf(pcfConfig.getPassword()))
                                              .endpointUrl(pcfConfig.getEndpointUrl())
                                              .orgName(pcfCommandDeployRequest.getOrganization())
                                              .spaceName(pcfCommandDeployRequest.getSpace())
                                              .timeOutIntervalInMins(pcfCommandDeployRequest.getTimeoutIntervalInMin())
                                              .useCFCLI(pcfCommandDeployRequest.isUseCfCLI())
                                              .limitPcfThreads(pcfCommandDeployRequest.isLimitPcfThreads())
                                              .build();

      // This will be CF_HOME for any cli related operations
      workingDirectory = pcfCommandTaskHelper.generateWorkingDirectoryForDeployment();
      if (workingDirectory == null) {
        throw new PivotalClientApiException("Failed to create working CF directory");
      }
      pcfRequestConfig.setCfHomeDirPath(workingDirectory.getAbsolutePath());

      // Init AppAutoscalarRequestData If Needed
      if (pcfCommandDeployRequest.isUseAppAutoscalar()) {
        pcfAppAutoscalarRequestData.setPcfRequestConfig(pcfRequestConfig);
        pcfAppAutoscalarRequestData.setConfigPathVar(workingDirectory.getAbsolutePath());
        pcfAppAutoscalarRequestData.setTimeoutInMins(pcfCommandDeployRequest.getTimeoutIntervalInMin());
      }

      ApplicationDetail details = pcfCommandTaskHelper.getNewlyCreatedApplication(
          pcfRequestConfig, pcfCommandDeployRequest, pcfDeploymentManager);
      // No of instances to be added to newly created application in this deploy stage
      Integer stepIncrease = pcfCommandDeployRequest.getUpdateCount() - details.getInstances();
      Integer stepDecrease = pcfCommandDeployRequest.getDownSizeCount();

      // downsize previous apps with non zero instances by same count new app was upsized
      List<PcfInstanceElement> pcfInstanceElementsForVerification = new ArrayList<>();
      if (DOWNSIZE_OLD_FIRST == pcfCommandDeployRequest.getResizeStrategy()) {
        pcfCommandTaskHelper.downsizePreviousReleases(pcfCommandDeployRequest, pcfRequestConfig, executionLogCallback,
            pcfServiceDataUpdated, stepDecrease, pcfInstanceElementsForVerification, pcfAppAutoscalarRequestData);
        unmapRoutesIfAppDownsizedToZero(pcfCommandDeployRequest, pcfRequestConfig, executionLogCallback);
        executionLogCallback.saveExecutionLog("Downsize Application Successfully Completed", INFO, SUCCESS);

        executionLogCallback = pcfCommandTaskHelper.getLogCallBack(delegateLogService, pcfCommandRequest.getAccountId(),
            pcfCommandRequest.getAppId(), pcfCommandRequest.getActivityId(), Upsize);
        performUpsize(executionLogCallback, pcfCommandDeployRequest, pcfServiceDataUpdated, pcfRequestConfig, details,
            pcfInstanceElementsForVerification, pcfAppAutoscalarRequestData);
        executionLogCallback.saveExecutionLog("Upsize Application Successfully Completed", INFO, SUCCESS);
      } else {
        performUpsize(executionLogCallback, pcfCommandDeployRequest, pcfServiceDataUpdated, pcfRequestConfig, details,
            pcfInstanceElementsForVerification, pcfAppAutoscalarRequestData);
        executionLogCallback.saveExecutionLog("Upsize Application Successfully Completed", INFO, SUCCESS);

        executionLogCallback = pcfCommandTaskHelper.getLogCallBack(delegateLogService, pcfCommandRequest.getAccountId(),
            pcfCommandRequest.getAppId(), pcfCommandRequest.getActivityId(), Downsize);
        pcfCommandTaskHelper.downsizePreviousReleases(pcfCommandDeployRequest, pcfRequestConfig, executionLogCallback,
            pcfServiceDataUpdated, stepDecrease, pcfInstanceElementsForVerification, pcfAppAutoscalarRequestData);
        unmapRoutesIfAppDownsizedToZero(pcfCommandDeployRequest, pcfRequestConfig, executionLogCallback);
        executionLogCallback.saveExecutionLog("Downsize Application Successfully Completed", INFO, SUCCESS);
      }

      // This data will be used by verification phase for analysis
      executionLogCallback = pcfCommandTaskHelper.getLogCallBack(delegateLogService, pcfCommandRequest.getAccountId(),
          pcfCommandRequest.getAppId(), pcfCommandRequest.getActivityId(), Wrapup);
      generatePcfInstancesElementsForExistingApp(
          pcfInstanceElementsForVerification, pcfRequestConfig, pcfCommandDeployRequest, executionLogCallback);

      // generate response to be sent back to Manager
      pcfDeployCommandResponse.setCommandExecutionStatus(SUCCESS);
      pcfDeployCommandResponse.setOutput(StringUtils.EMPTY);
      pcfDeployCommandResponse.setInstanceDataUpdated(pcfServiceDataUpdated);
      pcfDeployCommandResponse.getPcfInstanceElements().addAll(pcfInstanceElementsForVerification);

    } catch (PivotalClientApiException | IOException e) {
      exceptionOccured = true;
      exception = e;
      logException(executionLogCallback, pcfCommandDeployRequest, exception);
    } catch (Exception ex) {
      exceptionOccured = true;
      exception = ex;
      logException(executionLogCallback, pcfCommandDeployRequest, exception);
    } finally {
      try {
        if (workingDirectory != null) {
          executionLogCallback.saveExecutionLog("#--------- Removing any temporary files created");
          FileIo.deleteDirectoryAndItsContentIfExists(workingDirectory.getAbsolutePath());
        }
      } catch (IOException e) {
        logger.warn("Failed to delete Temp Directory created for CF CLI login", e);
      }
    }

    if (exceptionOccured) {
      pcfDeployCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
      pcfDeployCommandResponse.setOutput(ExceptionUtils.getMessage(exception));
      pcfDeployCommandResponse.setInstanceDataUpdated(pcfServiceDataUpdated);
    } else {
      executionLogCallback.saveExecutionLog("#------- PCF Resize State Successfully Completed", INFO, SUCCESS);
    }

    return PcfCommandExecutionResponse.builder()
        .commandExecutionStatus(pcfDeployCommandResponse.getCommandExecutionStatus())
        .errorMessage(pcfDeployCommandResponse.getOutput())
        .pcfCommandResponse(pcfDeployCommandResponse)
        .build();
  }

  private void logException(
      ExecutionLogCallback executionLogCallback, PcfCommandDeployRequest pcfCommandDeployRequest, Exception exception) {
    logger.error(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "Exception in processing PCF Deploy task [{}]",
        pcfCommandDeployRequest, exception);

    executionLogCallback.saveExecutionLog("\n\n--------- PCF Resize failed to complete successfully", ERROR, FAILURE);
    Misc.logAllMessages(exception, executionLogCallback);
  }

  @VisibleForTesting
  void generatePcfInstancesElementsForExistingApp(List<PcfInstanceElement> pcfInstanceElementsForVerification,
      PcfRequestConfig pcfRequestConfig, PcfCommandDeployRequest pcfCommandDeployRequest,
      ExecutionLogCallback executionLogCallback) {
    PcfAppSetupTimeDetails downsizeAppDetail = pcfCommandDeployRequest.getDownsizeAppDetail();
    if (downsizeAppDetail == null || isBlank(downsizeAppDetail.getApplicationName())) {
      return;
    }

    try {
      pcfRequestConfig.setApplicationName(downsizeAppDetail.getApplicationName());
      ApplicationDetail applicationDetail = pcfDeploymentManager.getApplicationByName(pcfRequestConfig);
      applicationDetail.getInstanceDetails().forEach(instanceDetail
          -> pcfInstanceElementsForVerification.add(PcfInstanceElement.builder()
                                                        .applicationId(applicationDetail.getId())
                                                        .displayName(applicationDetail.getName())
                                                        .instanceIndex(instanceDetail.getIndex())
                                                        .isUpsize(false)
                                                        .build()));
    } catch (Exception e) {
      executionLogCallback.saveExecutionLog(
          new StringBuilder(128)
              .append("# Failed to fetch InstanceDetails for existing Application: ")
              .append(downsizeAppDetail.getApplicationName())
              .append(", Verification may be able to use older instances to compare data")
              .toString());
    }
  }

  private void performUpsize(ExecutionLogCallback executionLogCallback, PcfCommandDeployRequest pcfCommandDeployRequest,
      List<PcfServiceData> pcfServiceDataUpdated, PcfRequestConfig pcfRequestConfig, ApplicationDetail details,
      List<PcfInstanceElement> pcfInstanceElementsForVerification, PcfAppAutoscalarRequestData appAutoscalarRequestData)
      throws PivotalClientApiException, IOException {
    pcfCommandTaskHelper.upsizeNewApplication(executionLogCallback, pcfCommandDeployRequest, pcfServiceDataUpdated,
        pcfRequestConfig, details, pcfInstanceElementsForVerification);
    configureAutoscalarIfNeeded(pcfCommandDeployRequest, details, appAutoscalarRequestData, executionLogCallback);
  }

  @VisibleForTesting
  void configureAutoscalarIfNeeded(PcfCommandDeployRequest pcfCommandDeployRequest, ApplicationDetail applicationDetail,
      PcfAppAutoscalarRequestData appAutoscalarRequestData, ExecutionLogCallback executionLogCallback)
      throws PivotalClientApiException, IOException {
    if (pcfCommandDeployRequest.isUseAppAutoscalar() && pcfCommandDeployRequest.getPcfManifestsPackage() != null
        && isNotEmpty(pcfCommandDeployRequest.getPcfManifestsPackage().getAutoscalarManifestYml())
        && pcfCommandDeployRequest.getMaxCount() <= pcfCommandDeployRequest.getUpdateCount()) {
      // This is autoscalar file inside workingDirectory
      String filePath =
          appAutoscalarRequestData.getConfigPathVar() + "/autoscalar_" + System.currentTimeMillis() + ".yml";
      pcfCommandTaskHelper.createYamlFileLocally(
          filePath, pcfCommandDeployRequest.getPcfManifestsPackage().getAutoscalarManifestYml());

      // upload autoscalar config
      appAutoscalarRequestData.setApplicationName(applicationDetail.getName());
      appAutoscalarRequestData.setApplicationGuid(applicationDetail.getId());
      appAutoscalarRequestData.setTimeoutInMins(pcfCommandDeployRequest.getTimeoutIntervalInMin());
      appAutoscalarRequestData.setAutoscalarFilePath(filePath);
      pcfDeploymentManager.performConfigureAutoscalar(appAutoscalarRequestData, executionLogCallback);
    }
  }

  private void unmapRoutesIfAppDownsizedToZero(PcfCommandDeployRequest pcfCommandDeployRequest,
      PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    if (pcfCommandDeployRequest.isStandardBlueGreen() || pcfCommandDeployRequest.getDownsizeAppDetail() == null
        || isBlank(pcfCommandDeployRequest.getDownsizeAppDetail().getApplicationName())) {
      return;
    }

    pcfRequestConfig.setApplicationName(pcfCommandDeployRequest.getDownsizeAppDetail().getApplicationName());
    ApplicationDetail applicationDetail = pcfDeploymentManager.getApplicationByName(pcfRequestConfig);

    if (applicationDetail.getInstances() == 0) {
      pcfCommandTaskHelper.unmapExistingRouteMaps(applicationDetail, pcfRequestConfig, executionLogCallback);
    }
  }
}
