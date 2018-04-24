package software.wings.service.impl;

import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.PcfConfig;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest.PcfCommandType;
import software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfInfraMappingDataResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by bzane on 2/22/17
 */
@Singleton
public class PcfHelperService {
  private static final Logger logger = LoggerFactory.getLogger(PcfHelperService.class);
  @Inject private DelegateService delegateService;
  @Inject private SecretManager secretManager;
  @Inject private EncryptionService encryptionService;

  public void validate(PcfConfig pcfConfig) {
    PcfCommandExecutionResponse pcfCommandExecutionResponse;

    try {
      pcfCommandExecutionResponse =
          delegateService.executeTask(aDelegateTask()
                                          .withTaskType(TaskType.PCF_COMMAND_TASK)
                                          .withAccountId(pcfConfig.getAccountId())
                                          .withAppId(GLOBAL_APP_ID)
                                          .withAsync(false)
                                          .withTimeout(TimeUnit.MINUTES.toMillis(2))
                                          .withParameters(new Object[] {PcfInfraMappingDataRequest.builder()
                                                                            .pcfConfig(pcfConfig)
                                                                            .pcfCommandType(PcfCommandType.VALIDATE)
                                                                            .timeoutIntervalInMin(2)
                                                                            .build(),
                                              null})
                                          .build());

    } catch (InterruptedException e) {
      pcfCommandExecutionResponse = PcfCommandExecutionResponse.builder()
                                        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                                        .errorMessage(e.getMessage())
                                        .build();
    }

    if (CommandExecutionStatus.FAILURE.equals(pcfCommandExecutionResponse.getCommandExecutionStatus())) {
      logger.warn(pcfCommandExecutionResponse.getErrorMessage());
      throw new WingsException(INVALID_REQUEST).addParam("message", pcfCommandExecutionResponse.getErrorMessage());
    }
  }
  public List<String> listOrganizations(PcfConfig pcfConfig) {
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(pcfConfig, null, null);
    PcfCommandExecutionResponse pcfCommandExecutionResponse;
    try {
      pcfCommandExecutionResponse =
          delegateService.executeTask(aDelegateTask()
                                          .withTaskType(TaskType.PCF_COMMAND_TASK)
                                          .withAccountId(pcfConfig.getAccountId())
                                          .withAppId(GLOBAL_APP_ID)
                                          .withAsync(false)
                                          .withTimeout(TimeUnit.MINUTES.toMillis(2))
                                          .withParameters(new Object[] {PcfInfraMappingDataRequest.builder()
                                                                            .pcfConfig(pcfConfig)
                                                                            .pcfCommandType(PcfCommandType.DATAFETCH)
                                                                            .timeoutIntervalInMin(2)
                                                                            .build(),
                                              encryptionDetails})
                                          .build());
    } catch (InterruptedException e) {
      pcfCommandExecutionResponse = PcfCommandExecutionResponse.builder()
                                        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                                        .errorMessage(e.getMessage())
                                        .build();
    }

    if (CommandExecutionStatus.SUCCESS.equals(pcfCommandExecutionResponse.getCommandExecutionStatus())) {
      return ((PcfInfraMappingDataResponse) pcfCommandExecutionResponse.getPcfCommandResponse()).getOrganizations();
    } else {
      logger.warn(pcfCommandExecutionResponse.getErrorMessage());
      throw new WingsException(INVALID_REQUEST).addParam("message", pcfCommandExecutionResponse.getErrorMessage());
    }
  }

  public List<String> listSpaces(PcfConfig pcfConfig, String organization) {
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(pcfConfig, null, null);
    PcfCommandExecutionResponse pcfCommandExecutionResponse;
    try {
      pcfCommandExecutionResponse =
          delegateService.executeTask(aDelegateTask()
                                          .withTaskType(TaskType.PCF_COMMAND_TASK)
                                          .withAccountId(pcfConfig.getAccountId())
                                          .withAppId(GLOBAL_APP_ID)
                                          .withAsync(false)
                                          .withTimeout(TimeUnit.MINUTES.toMillis(2))
                                          .withParameters(new Object[] {PcfInfraMappingDataRequest.builder()
                                                                            .pcfConfig(pcfConfig)
                                                                            .organization(organization)
                                                                            .pcfCommandType(PcfCommandType.DATAFETCH)
                                                                            .timeoutIntervalInMin(2)
                                                                            .build(),
                                              encryptionDetails})
                                          .build());
    } catch (InterruptedException e) {
      pcfCommandExecutionResponse = PcfCommandExecutionResponse.builder()
                                        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                                        .errorMessage(e.getMessage())
                                        .build();
    }

    if (CommandExecutionStatus.SUCCESS.equals(pcfCommandExecutionResponse.getCommandExecutionStatus())) {
      return ((PcfInfraMappingDataResponse) pcfCommandExecutionResponse.getPcfCommandResponse()).getSpaces();
    } else {
      logger.warn(pcfCommandExecutionResponse.getErrorMessage());
      throw new WingsException(INVALID_REQUEST).addParam("message", pcfCommandExecutionResponse.getErrorMessage());
    }
  }

  public List<String> listRoutes(PcfConfig pcfConfig, String organization, String space) {
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(pcfConfig, null, null);
    PcfCommandExecutionResponse pcfCommandExecutionResponse;
    try {
      pcfCommandExecutionResponse =
          delegateService.executeTask(aDelegateTask()
                                          .withTaskType(TaskType.PCF_COMMAND_TASK)
                                          .withAccountId(pcfConfig.getAccountId())
                                          .withAppId(GLOBAL_APP_ID)
                                          .withAsync(false)
                                          .withTimeout(TimeUnit.MINUTES.toMillis(2))
                                          .withParameters(new Object[] {PcfInfraMappingDataRequest.builder()
                                                                            .pcfConfig(pcfConfig)
                                                                            .organization(organization)
                                                                            .space(space)
                                                                            .pcfCommandType(PcfCommandType.DATAFETCH)
                                                                            .timeoutIntervalInMin(2)
                                                                            .build(),
                                              encryptionDetails})
                                          .build());
    } catch (InterruptedException e) {
      pcfCommandExecutionResponse = PcfCommandExecutionResponse.builder()
                                        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                                        .errorMessage(e.getMessage())
                                        .build();
    }

    if (CommandExecutionStatus.SUCCESS.equals(pcfCommandExecutionResponse.getCommandExecutionStatus())) {
      return ((PcfInfraMappingDataResponse) pcfCommandExecutionResponse.getPcfCommandResponse()).getRouteMaps();
    } else {
      logger.warn(pcfCommandExecutionResponse.getErrorMessage());
      throw new WingsException(INVALID_REQUEST).addParam("message", pcfCommandExecutionResponse.getErrorMessage());
    }
  }
}
