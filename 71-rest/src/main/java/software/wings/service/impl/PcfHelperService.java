package software.wings.service.impl;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.PcfConfig;
import software.wings.beans.TaskType;
import software.wings.beans.infrastructure.instance.info.PcfInstanceInfo;
import software.wings.helpers.ext.pcf.PcfAppNotFoundException;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest.PcfCommandType;
import software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest;
import software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest.ActionType;
import software.wings.helpers.ext.pcf.request.PcfInstanceSyncRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfInfraMappingDataResponse;
import software.wings.helpers.ext.pcf.response.PcfInstanceSyncResponse;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.SecretManager;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by bzane on 2/22/17
 */
@Singleton
@Slf4j
public class PcfHelperService {
  @Inject private DelegateService delegateService;
  @Inject private SecretManager secretManager;

  public void validate(PcfConfig pcfConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    PcfCommandExecutionResponse pcfCommandExecutionResponse;

    try {
      pcfCommandExecutionResponse = delegateService.executeTask(
          DelegateTask.builder()
              .accountId(pcfConfig.getAccountId())
              .appId(GLOBAL_APP_ID)
              .data(TaskData.builder()
                        .async(false)
                        .taskType(TaskType.PCF_COMMAND_TASK.name())
                        .parameters(new Object[] {PcfInfraMappingDataRequest.builder()
                                                      .pcfConfig(pcfConfig)
                                                      .pcfCommandType(PcfCommandType.VALIDATE)
                                                      .timeoutIntervalInMin(2)
                                                      .build(),
                            encryptedDataDetails})
                        .timeout(TimeUnit.MINUTES.toMillis(2))
                        .build())
              .build());

    } catch (InterruptedException e) {
      pcfCommandExecutionResponse = PcfCommandExecutionResponse.builder()
                                        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                                        .errorMessage(ExceptionUtils.getMessage(e))
                                        .build();
    }

    if (CommandExecutionStatus.FAILURE == pcfCommandExecutionResponse.getCommandExecutionStatus()) {
      logger.warn(pcfCommandExecutionResponse.getErrorMessage());
      throw new InvalidRequestException(pcfCommandExecutionResponse.getErrorMessage());
    }
  }

  public List<PcfInstanceInfo> getApplicationDetails(String pcfApplicationName, String organization, String space,
      PcfConfig pcfConfig, PcfCommandExecutionResponse pcfCommandExecutionPerpTaskResponse)
      throws PcfAppNotFoundException {
    PcfCommandExecutionResponse pcfCommandExecutionResponse;

    try {
      List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(pcfConfig, null, null);

      if (pcfCommandExecutionPerpTaskResponse == null) {
        pcfCommandExecutionResponse =
            executeTaskOnDelegate(pcfApplicationName, organization, space, pcfConfig, encryptionDetails);
      } else {
        pcfCommandExecutionResponse = pcfCommandExecutionPerpTaskResponse;
      }

      PcfInstanceSyncResponse pcfInstanceSyncResponse =
          validatePcfInstanceSyncResponse(pcfApplicationName, organization, space, pcfCommandExecutionResponse);

      // creates the response based on the count of instances it has got.
      if (CollectionUtils.isNotEmpty(pcfInstanceSyncResponse.getInstanceIndices())) {
        return getPcfInstanceInfoList(pcfInstanceSyncResponse);
      }

    } catch (InterruptedException e) {
      throw new WingsException(ErrorCode.GENERAL_ERROR).addParam("message", "Failed to fetch app details for PCF");
    }

    return Collections.EMPTY_LIST;
  }

  private PcfCommandExecutionResponse executeTaskOnDelegate(String pcfApplicationName, String organization,
      String space, PcfConfig pcfConfig, List<EncryptedDataDetail> encryptionDetails) throws InterruptedException {
    PcfCommandExecutionResponse pcfCommandExecutionResponse;
    pcfCommandExecutionResponse = delegateService.executeTask(
        DelegateTask.builder()
            .accountId(pcfConfig.getAccountId())
            .appId(GLOBAL_APP_ID)
            .data(TaskData.builder()
                      .async(false)
                      .taskType(TaskType.PCF_COMMAND_TASK.name())
                      .parameters(new Object[] {PcfInstanceSyncRequest.builder()
                                                    .pcfConfig(pcfConfig)
                                                    .pcfApplicationName(pcfApplicationName)
                                                    .organization(organization)
                                                    .space(space)
                                                    .pcfCommandType(PcfCommandType.APP_DETAILS)
                                                    .timeoutIntervalInMin(5)
                                                    .build(),
                          encryptionDetails})
                      .timeout(TimeUnit.MINUTES.toMillis(5))
                      .build())
            .build());
    return pcfCommandExecutionResponse;
  }

  public PcfInstanceSyncResponse validatePcfInstanceSyncResponse(String pcfApplicationName, String organization,
      String space, PcfCommandExecutionResponse pcfCommandExecutionResponse) throws PcfAppNotFoundException {
    PcfInstanceSyncResponse pcfInstanceSyncResponse =
        (PcfInstanceSyncResponse) pcfCommandExecutionResponse.getPcfCommandResponse();

    // checks the status code and error messages.
    if (CommandExecutionStatus.FAILURE == pcfCommandExecutionResponse.getCommandExecutionStatus()) {
      logger.warn("Failed to fetch PCF application details for Instance Sync, check delegate logs"
          + pcfCommandExecutionResponse.getPcfCommandResponse().getOutput());
      if (pcfCommandExecutionResponse.getErrorMessage().contains(pcfApplicationName + " does not exist")
          || pcfCommandExecutionResponse.getErrorMessage().contains(organization + " does not exist")
          || pcfCommandExecutionResponse.getErrorMessage().contains(space + " does not exist")) {
        throw new PcfAppNotFoundException(pcfCommandExecutionResponse.getErrorMessage());
      } else {
        String errMsg = new StringBuilder(128)
                            .append("Failed to fetch app details for PCF APP: ")
                            .append(pcfApplicationName)
                            .append(" with Error: ")
                            .append(pcfInstanceSyncResponse.getOutput())
                            .toString();
        throw new WingsException(ErrorCode.GENERAL_ERROR, errMsg).addParam("message", errMsg);
      }
    }
    return pcfInstanceSyncResponse;
  }

  @NotNull
  private List<PcfInstanceInfo> getPcfInstanceInfoList(PcfInstanceSyncResponse pcfInstanceSyncResponse) {
    return pcfInstanceSyncResponse.getInstanceIndices()
        .stream()
        .map(index
            -> PcfInstanceInfo.builder()
                   .instanceIndex(index)
                   .pcfApplicationName(pcfInstanceSyncResponse.getName())
                   .pcfApplicationGuid(pcfInstanceSyncResponse.getGuid())
                   .organization(pcfInstanceSyncResponse.getOrganization())
                   .space(pcfInstanceSyncResponse.getSpace())
                   .id(pcfInstanceSyncResponse.getGuid() + ":" + index)
                   .build())
        .collect(Collectors.toList());
  }

  public List<String> listOrganizations(PcfConfig pcfConfig) {
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(pcfConfig, null, null);
    PcfCommandExecutionResponse pcfCommandExecutionResponse;
    try {
      pcfCommandExecutionResponse = delegateService.executeTask(
          DelegateTask.builder()
              .accountId(pcfConfig.getAccountId())
              .appId(GLOBAL_APP_ID)
              .data(TaskData.builder()
                        .async(false)
                        .taskType(TaskType.PCF_COMMAND_TASK.name())
                        .parameters(new Object[] {PcfInfraMappingDataRequest.builder()
                                                      .pcfConfig(pcfConfig)
                                                      .pcfCommandType(PcfCommandType.DATAFETCH)
                                                      .actionType(ActionType.FETCH_ORG)
                                                      .timeoutIntervalInMin(2)
                                                      .build(),
                            encryptionDetails})
                        .timeout(TimeUnit.MINUTES.toMillis(2))
                        .build())
              .build());
    } catch (InterruptedException e) {
      pcfCommandExecutionResponse = PcfCommandExecutionResponse.builder()
                                        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                                        .errorMessage(ExceptionUtils.getMessage(e))
                                        .build();
    }

    if (CommandExecutionStatus.SUCCESS == pcfCommandExecutionResponse.getCommandExecutionStatus()) {
      return ((PcfInfraMappingDataResponse) pcfCommandExecutionResponse.getPcfCommandResponse()).getOrganizations();
    } else {
      logger.warn(pcfCommandExecutionResponse.getErrorMessage());
      throw new InvalidRequestException(pcfCommandExecutionResponse.getErrorMessage());
    }
  }

  public String createRoute(PcfConfig pcfConfig, String organization, String space, String host, String domain,
      String path, boolean tcpRoute, boolean useRandomPort, Integer port) {
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(pcfConfig, null, null);
    PcfCommandExecutionResponse pcfCommandExecutionResponse;
    try {
      pcfCommandExecutionResponse = delegateService.executeTask(
          DelegateTask.builder()
              .accountId(pcfConfig.getAccountId())
              .appId(GLOBAL_APP_ID)
              .data(TaskData.builder()
                        .async(false)
                        .taskType(TaskType.PCF_COMMAND_TASK.name())
                        .parameters(new Object[] {PcfInfraMappingDataRequest.builder()
                                                      .pcfConfig(pcfConfig)
                                                      .pcfCommandType(PcfCommandType.CREATE_ROUTE)
                                                      .organization(organization)
                                                      .space(space)
                                                      .host(host)
                                                      .domain(domain)
                                                      .path(path)
                                                      .tcpRoute(tcpRoute)
                                                      .useRandomPort(useRandomPort)
                                                      .port(port)
                                                      .timeoutIntervalInMin(2)
                                                      .build(),
                            encryptionDetails})
                        .timeout(TimeUnit.MINUTES.toMillis(2))
                        .build())
              .build());
    } catch (InterruptedException e) {
      pcfCommandExecutionResponse = PcfCommandExecutionResponse.builder()
                                        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                                        .errorMessage(ExceptionUtils.getMessage(e))
                                        .build();
    }

    if (CommandExecutionStatus.SUCCESS == pcfCommandExecutionResponse.getCommandExecutionStatus()) {
      return ((PcfInfraMappingDataResponse) pcfCommandExecutionResponse.getPcfCommandResponse()).getRouteMaps().get(0);
    } else {
      logger.warn(pcfCommandExecutionResponse.getErrorMessage());
      throw new InvalidRequestException(pcfCommandExecutionResponse.getErrorMessage());
    }
  }

  public List<String> listSpaces(PcfConfig pcfConfig, String organization) {
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(pcfConfig, null, null);
    PcfCommandExecutionResponse pcfCommandExecutionResponse;
    try {
      pcfCommandExecutionResponse = delegateService.executeTask(
          DelegateTask.builder()
              .accountId(pcfConfig.getAccountId())
              .appId(GLOBAL_APP_ID)
              .data(TaskData.builder()
                        .async(false)
                        .taskType(TaskType.PCF_COMMAND_TASK.name())
                        .parameters(new Object[] {PcfInfraMappingDataRequest.builder()
                                                      .pcfConfig(pcfConfig)
                                                      .organization(organization)
                                                      .pcfCommandType(PcfCommandType.DATAFETCH)
                                                      .actionType(ActionType.FETCH_SPACE)
                                                      .timeoutIntervalInMin(2)
                                                      .build(),
                            encryptionDetails})
                        .timeout(TimeUnit.MINUTES.toMillis(2))
                        .build())
              .build());
    } catch (InterruptedException e) {
      pcfCommandExecutionResponse = PcfCommandExecutionResponse.builder()
                                        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                                        .errorMessage(ExceptionUtils.getMessage(e))
                                        .build();
    }

    if (CommandExecutionStatus.SUCCESS == pcfCommandExecutionResponse.getCommandExecutionStatus()) {
      return ((PcfInfraMappingDataResponse) pcfCommandExecutionResponse.getPcfCommandResponse()).getSpaces();
    } else {
      logger.warn(pcfCommandExecutionResponse.getErrorMessage());
      throw new InvalidRequestException(pcfCommandExecutionResponse.getErrorMessage());
    }
  }

  public List<String> listRoutes(PcfConfig pcfConfig, String organization, String space) {
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(pcfConfig, null, null);
    PcfCommandExecutionResponse pcfCommandExecutionResponse;
    try {
      pcfCommandExecutionResponse = delegateService.executeTask(
          DelegateTask.builder()
              .accountId(pcfConfig.getAccountId())
              .appId(GLOBAL_APP_ID)
              .data(TaskData.builder()
                        .async(false)
                        .taskType(TaskType.PCF_COMMAND_TASK.name())
                        .parameters(new Object[] {PcfInfraMappingDataRequest.builder()
                                                      .pcfConfig(pcfConfig)
                                                      .organization(organization)
                                                      .space(space)
                                                      .pcfCommandType(PcfCommandType.DATAFETCH)
                                                      .actionType(ActionType.FETCH_ROUTE)
                                                      .timeoutIntervalInMin(2)
                                                      .build(),
                            encryptionDetails})
                        .timeout(TimeUnit.MINUTES.toMillis(2))
                        .build())
              .build());
    } catch (InterruptedException e) {
      pcfCommandExecutionResponse = PcfCommandExecutionResponse.builder()
                                        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                                        .errorMessage(ExceptionUtils.getMessage(e))
                                        .build();
    }

    if (CommandExecutionStatus.SUCCESS == pcfCommandExecutionResponse.getCommandExecutionStatus()) {
      return ((PcfInfraMappingDataResponse) pcfCommandExecutionResponse.getPcfCommandResponse()).getRouteMaps();
    } else {
      logger.warn(pcfCommandExecutionResponse.getErrorMessage());
      throw new InvalidRequestException(pcfCommandExecutionResponse.getErrorMessage());
    }
  }

  public Integer getRunningInstanceCount(PcfConfig pcfConfig, String organization, String space, String appPrefix) {
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(pcfConfig, null, null);
    PcfCommandExecutionResponse pcfCommandExecutionResponse;
    try {
      pcfCommandExecutionResponse = delegateService.executeTask(
          DelegateTask.builder()
              .accountId(pcfConfig.getAccountId())
              .appId(GLOBAL_APP_ID)
              .data(TaskData.builder()
                        .async(false)
                        .taskType(TaskType.PCF_COMMAND_TASK.name())
                        .parameters(new Object[] {PcfInfraMappingDataRequest.builder()
                                                      .pcfConfig(pcfConfig)
                                                      .organization(organization)
                                                      .space(space)
                                                      .actionType(ActionType.RUNNING_COUNT)
                                                      .pcfCommandType(PcfCommandType.DATAFETCH)
                                                      .applicationNamePrefix(appPrefix)
                                                      .timeoutIntervalInMin(2)
                                                      .build(),
                            encryptionDetails})
                        .timeout(TimeUnit.MINUTES.toMillis(2))
                        .build())
              .build());
    } catch (InterruptedException e) {
      pcfCommandExecutionResponse = PcfCommandExecutionResponse.builder()
                                        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                                        .errorMessage(ExceptionUtils.getMessage(e))
                                        .build();
    }

    if (CommandExecutionStatus.SUCCESS == pcfCommandExecutionResponse.getCommandExecutionStatus()) {
      PcfInfraMappingDataResponse pcfInfraMappingDataResponse =
          (PcfInfraMappingDataResponse) pcfCommandExecutionResponse.getPcfCommandResponse();
      return pcfInfraMappingDataResponse.getRunningInstanceCount();
    } else {
      logger.warn(pcfCommandExecutionResponse.getErrorMessage());
      throw new InvalidRequestException(pcfCommandExecutionResponse.getErrorMessage());
    }
  }
}
