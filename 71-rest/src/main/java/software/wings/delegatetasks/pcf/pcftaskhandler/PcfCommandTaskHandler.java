package software.wings.delegatetasks.pcf.pcftaskhandler;

import com.google.inject.Inject;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.pcf.PcfCommandTaskHelper;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;
public abstract class PcfCommandTaskHandler {
  @Inject protected DelegateFileManager delegateFileManager;
  @Inject protected PcfDeploymentManager pcfDeploymentManager;
  @Inject protected EncryptionService encryptionService;
  @Inject protected DelegateLogService delegateLogService;
  @Inject protected PcfCommandTaskHelper pcfCommandTaskHelper;

  public PcfCommandExecutionResponse executeTask(
      PcfCommandRequest pcfCommandRequest, List<EncryptedDataDetail> encryptedDataDetails) {
    ExecutionLogCallback executionLogCallback =
        new ExecutionLogCallback(delegateLogService, pcfCommandRequest.getAccountId(), pcfCommandRequest.getAppId(),
            pcfCommandRequest.getActivityId(), pcfCommandRequest.getCommandName());

    return executeTaskInternal(pcfCommandRequest, encryptedDataDetails, executionLogCallback);
  }

  protected abstract PcfCommandExecutionResponse executeTaskInternal(PcfCommandRequest pcfCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback);
}
