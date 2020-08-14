package io.harness.cdng.git.tasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.GIT_CONNECTION_ERROR;
import static io.harness.eraro.ErrorCode.GIT_DIFF_COMMIT_NOT_IN_ORDER;
import static software.wings.beans.yaml.YamlConstants.GIT_YAML_LOG_PREFIX;

import com.google.inject.Inject;

import io.harness.cdng.gitclient.GitClientNG;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.git.GitCommand.GitCommandType;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandExecutionResponse.GitCommandStatus;
import io.harness.delegate.beans.git.GitCommandParams;
import io.harness.delegate.beans.git.GitCommandRequest;
import io.harness.delegate.beans.git.GitCommitAndPushRequest;
import io.harness.delegate.beans.git.GitCommitAndPushResult;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public class NGGitCommandTask extends AbstractDelegateRunnableTask {
  @Inject private GitClientNG gitClient;
  @Inject private SecretDecryptionService decryptionService;

  public NGGitCommandTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public ResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public ResponseData run(TaskParameters parameters) {
    GitCommandParams gitCommandParams = (GitCommandParams) parameters;
    GitConfigDTO gitConfig = gitCommandParams.getGitConfig();
    List<EncryptedDataDetail> encryptionDetails = gitCommandParams.getEncryptionDetails();
    decryptionService.decrypt(gitConfig.getGitAuth(), encryptionDetails);
    GitCommandType gitCommandType = gitCommandParams.getGitCommandType();
    GitCommandRequest gitCommandRequest = gitCommandParams.getGitCommandRequest();

    try {
      switch (gitCommandType) {
        case VALIDATE:
          return handleValidateTask(gitConfig);
        case COMMIT_AND_PUSH:
          return handleCommitAndPush(gitCommandParams, gitConfig);
        default:
          return GitCommandExecutionResponse.builder()
              .gitCommandStatus(GitCommandStatus.FAILURE)
              .gitCommandRequest(gitCommandRequest)
              .errorMessage(GIT_YAML_LOG_PREFIX + "Git Operation not supported")
              .build();
      }
    } catch (Exception ex) {
      return GitCommandExecutionResponse.builder()
          .gitCommandRequest(gitCommandRequest)
          .errorMessage(ex.getMessage())
          .errorCode(getErrorCode(ex))
          .gitCommandStatus(GitCommandStatus.FAILURE)
          .build();
    }
  }

  private ResponseData handleCommitAndPush(GitCommandParams gitCommandParams, GitConfigDTO gitConfig) {
    GitCommitAndPushRequest gitCommitRequest = (GitCommitAndPushRequest) gitCommandParams.getGitCommandRequest();
    logger.info(GIT_YAML_LOG_PREFIX + "COMMIT_AND_PUSH: [{}]", gitCommitRequest);
    GitCommitAndPushResult gitCommitAndPushResult =
        gitClient.commitAndPush(gitConfig, gitCommitRequest, getAccountId(), null);
    gitCommitAndPushResult.setYamlGitConfig(gitCommitRequest.getYamlGitConfigs());

    return GitCommandExecutionResponse.builder()
        .gitCommandRequest(gitCommitRequest)
        .gitCommandResult(gitCommitAndPushResult)
        .gitCommandStatus(GitCommandStatus.SUCCESS)
        .build();
  }

  private ResponseData handleValidateTask(GitConfigDTO gitConfig) {
    logger.info("Processing Git command: VALIDATE");
    String errorMessage = gitClient.validate(gitConfig);
    if (isEmpty(errorMessage)) {
      return GitCommandExecutionResponse.builder().gitCommandStatus(GitCommandStatus.SUCCESS).build();
    } else {
      return GitCommandExecutionResponse.builder()
          .gitCommandStatus(GitCommandStatus.FAILURE)
          .errorMessage(errorMessage)
          .build();
    }
  }

  private ErrorCode getErrorCode(Exception ex) {
    if (ex instanceof WingsException) {
      final WingsException we = (WingsException) ex;
      if (GIT_CONNECTION_ERROR == we.getCode()) {
        return GIT_CONNECTION_ERROR;
      } else if (GIT_DIFF_COMMIT_NOT_IN_ORDER == we.getCode()) {
        return GIT_DIFF_COMMIT_NOT_IN_ORDER;
      }
    }
    return null;
  }
}
