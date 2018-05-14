package software.wings.delegatetasks.cloudformation.cloudformationtaskhandler;

import com.google.inject.Inject;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;

public abstract class CloudFormationCommandTaskHandler {
  @Inject protected DelegateFileManager delegateFileManager;
  @Inject protected EncryptionService encryptionService;
  @Inject protected AwsHelperService awsHelperService;
  @Inject private DelegateLogService delegateLogService;

  protected ExecutionLogCallback executionLogCallback;

  // ten minutes default timeout for polling stack operations
  static final int DEFAULT_TIMEOUT_MS = 10 * 60 * 1000;

  public CloudFormationCommandExecutionResponse execute(
      CloudFormationCommandRequest request, List<EncryptedDataDetail> details) {
    executionLogCallback = new ExecutionLogCallback(delegateLogService, request.getAccountId(), request.getAppId(),
        request.getActivityId(), request.getCommandName());
    return executeInternal(request, details);
  }

  protected abstract CloudFormationCommandExecutionResponse executeInternal(
      CloudFormationCommandRequest request, List<EncryptedDataDetail> details);
}