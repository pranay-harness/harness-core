package software.wings.delegatetasks.pcf.pcftaskhandler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.ActivityBasedLogSanitizer;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.LogSanitizer;
import software.wings.delegatetasks.pcf.PcfCommandTaskHelper;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.service.intfc.security.EncryptionService;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public abstract class PcfCommandTaskHandler {
  @Inject protected DelegateFileManager delegateFileManager;
  @Inject protected PcfDeploymentManager pcfDeploymentManager;
  @Inject protected EncryptionService encryptionService;
  @Inject protected DelegateLogService delegateLogService;
  @Inject protected PcfCommandTaskHelper pcfCommandTaskHelper;

  public PcfCommandExecutionResponse executeTask(
      PcfCommandRequest pcfCommandRequest, List<EncryptedDataDetail> encryptedDataDetails, boolean isInstanceSync) {
    Optional<LogSanitizer> logSanitizer =
        getLogSanitizer(pcfCommandRequest.getActivityId(), encryptedDataDetails, isInstanceSync);
    try {
      logSanitizer.ifPresent(delegateLogService::registerLogSanitizer);
      ExecutionLogCallback executionLogCallback =
          new ExecutionLogCallback(delegateLogService, pcfCommandRequest.getAccountId(), pcfCommandRequest.getAppId(),
              pcfCommandRequest.getActivityId(), pcfCommandRequest.getCommandName());

      return executeTaskInternal(pcfCommandRequest, encryptedDataDetails, executionLogCallback, isInstanceSync);
    } finally {
      logSanitizer.ifPresent(delegateLogService::unregisterLogSanitizer);
    }
  }

  protected abstract PcfCommandExecutionResponse executeTaskInternal(PcfCommandRequest pcfCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback,
      boolean isInstanceSync);

  private Optional<LogSanitizer> getLogSanitizer(
      String activityId, List<EncryptedDataDetail> encryptedDataDetails, boolean isInstanceSync) {
    Set<String> secrets = new HashSet<>();
    if (isNotEmpty(encryptedDataDetails)) {
      for (EncryptedDataDetail encryptedDataDetail : encryptedDataDetails) {
        secrets.add(String.valueOf(encryptionService.getDecryptedValue(encryptedDataDetail, isInstanceSync)));
      }
    }
    return isNotEmpty(secrets) ? Optional.of(new ActivityBasedLogSanitizer(activityId, secrets)) : Optional.empty();
  }
}
