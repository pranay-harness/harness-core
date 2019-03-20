package software.wings.delegatetasks.aws.ecs.ecstaskhandler;

import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.ecs.request.EcsCommandRequest;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;

public abstract class EcsCommandTaskHandler {
  @Inject protected DelegateFileManager delegateFileManager;
  @Inject protected EncryptionService encryptionService;
  @Inject protected DelegateLogService delegateLogService;

  public EcsCommandExecutionResponse executeTask(
      EcsCommandRequest ecsCommandRequest, List<EncryptedDataDetail> encryptedDataDetails) {
    ExecutionLogCallback executionLogCallback =
        new ExecutionLogCallback(delegateLogService, ecsCommandRequest.getAccountId(), ecsCommandRequest.getAppId(),
            ecsCommandRequest.getActivityId(), ecsCommandRequest.getCommandName());

    try {
      return executeTaskInternal(ecsCommandRequest, encryptedDataDetails, executionLogCallback);
    } catch (Exception e) {
      executionLogCallback.saveExecutionLog(
          "Failed while executing EcsCommandTask: " + ecsCommandRequest.getEcsCommandType(), LogLevel.ERROR);
      executionLogCallback.saveExecutionLog(e.getMessage(), LogLevel.ERROR);
      throw new WingsException(ErrorCode.GENERAL_ERROR, e.getMessage(), WingsException.USER)
          .addParam("message", e.getMessage());
    }
  }

  protected abstract EcsCommandExecutionResponse executeTaskInternal(EcsCommandRequest ecsCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback);
}
