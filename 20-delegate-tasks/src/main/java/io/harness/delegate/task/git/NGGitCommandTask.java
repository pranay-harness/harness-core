package io.harness.delegate.task.git;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.GIT_CONNECTION_ERROR;
import static io.harness.eraro.ErrorCode.GIT_DIFF_COMMIT_NOT_IN_ORDER;
import static io.harness.eraro.ErrorCode.GIT_UNSEEN_REMOTE_HEAD_COMMIT;
import static io.harness.git.Constants.GIT_YAML_LOG_PREFIX;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandExecutionResponse.GitCommandStatus;
import io.harness.delegate.beans.git.GitCommandParams;
import io.harness.delegate.beans.git.GitCommandType;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.git.NGGitService;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.git.model.CommitAndPushRequest;
import io.harness.git.model.CommitAndPushResult;
import io.harness.git.model.GitBaseRequest;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class NGGitCommandTask extends AbstractDelegateRunnableTask {
  @Inject private SecretDecryptionService decryptionService;
  @Inject private NGGitService gitService;

  public NGGitCommandTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    GitCommandParams gitCommandParams = (GitCommandParams) parameters;
    GitConfigDTO gitConfig = gitCommandParams.getGitConfig();
    List<EncryptedDataDetail> encryptionDetails = gitCommandParams.getEncryptionDetails();
    decryptionService.decrypt(gitConfig.getGitAuth(), encryptionDetails);
    GitCommandType gitCommandType = gitCommandParams.getGitCommandType();
    GitBaseRequest gitCommandRequest = gitCommandParams.getGitCommandRequest();

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

  private DelegateResponseData handleCommitAndPush(GitCommandParams gitCommandParams, GitConfigDTO gitConfig) {
    CommitAndPushRequest gitCommitRequest = (CommitAndPushRequest) gitCommandParams.getGitCommandRequest();
    log.info(GIT_YAML_LOG_PREFIX + "COMMIT_AND_PUSH: [{}]", gitCommitRequest);
    CommitAndPushResult gitCommitAndPushResult = gitService.commitAndPush(gitConfig, gitCommitRequest, getAccountId());

    return GitCommandExecutionResponse.builder()
        .gitCommandRequest(gitCommitRequest)
        .gitCommandResult(gitCommitAndPushResult)
        .gitCommandStatus(GitCommandStatus.SUCCESS)
        .build();
  }

  private DelegateResponseData handleValidateTask(GitConfigDTO gitConfig) {
    log.info("Processing Git command: VALIDATE");
    String errorMessage = gitService.validate(gitConfig, getAccountId());
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
      switch (we.getCode()) {
        case GIT_CONNECTION_ERROR:
          return GIT_CONNECTION_ERROR;
        case GIT_DIFF_COMMIT_NOT_IN_ORDER:
          return GIT_DIFF_COMMIT_NOT_IN_ORDER;
        case GIT_UNSEEN_REMOTE_HEAD_COMMIT:
          return GIT_UNSEEN_REMOTE_HEAD_COMMIT;
        default:
          return null;
      }
    }
    return null;
  }
}
