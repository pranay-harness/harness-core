package software.wings.delegatetasks;

import static io.harness.exception.WingsException.USER_ADMIN;
import static software.wings.beans.yaml.YamlConstants.GIT_YAML_LOG_PREFIX;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.GitRepositoryType;
import software.wings.beans.yaml.GitCommand.GitCommandType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandExecutionResponseBuilder;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.beans.yaml.GitCommitAndPushResult;
import software.wings.beans.yaml.GitCommitRequest;
import software.wings.beans.yaml.GitDiffRequest;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFetchFilesRequest;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by anubhaw on 10/26/17.
 */
public class GitCommandTask extends AbstractDelegateRunnableTask {
  @Inject private GitClient gitClient;
  @Inject private EncryptionService encryptionService;

  private static final Logger logger = LoggerFactory.getLogger(GitCommandTask.class);

  public GitCommandTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public GitCommandExecutionResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public GitCommandExecutionResponse run(Object[] parameters) {
    GitCommandType gitCommandType = (GitCommandType) parameters[0];
    GitConfig gitConfig = (GitConfig) parameters[1];
    gitConfig.setGitRepoType(GitRepositoryType.YAML); // TODO:: find better place. possibly manager can set this

    try {
      List<EncryptedDataDetail> encryptionDetails = (List<EncryptedDataDetail>) parameters[2];

      // Decrypt git config
      decryptGitConfig(gitConfig, encryptionDetails);

      switch (gitCommandType) {
        case COMMIT_AND_PUSH:
          GitCommitRequest gitCommitRequest = (GitCommitRequest) parameters[3];
          logger.info(GIT_YAML_LOG_PREFIX + "COMMIT_AND_PUSH: [{}]", gitCommitRequest);
          GitCommitAndPushResult gitCommitAndPushResult = gitClient.commitAndPush(gitConfig, gitCommitRequest);
          gitCommitAndPushResult.setYamlGitConfig(gitCommitRequest.getYamlGitConfig());

          return GitCommandExecutionResponse.builder()
              .gitCommandRequest(gitCommitRequest)
              .gitCommandResult(gitCommitAndPushResult)
              .gitCommandStatus(GitCommandStatus.SUCCESS)
              .build();
        case DIFF:
          GitDiffRequest gitDiffRequest = (GitDiffRequest) parameters[3];
          logger.info(GIT_YAML_LOG_PREFIX + "DIFF: [{}]", gitDiffRequest);
          GitDiffResult gitDiffResult = gitClient.diff(gitConfig, gitDiffRequest.getLastProcessedCommitId());
          gitDiffResult.setYamlGitConfig(gitDiffRequest.getYamlGitConfig());

          return GitCommandExecutionResponse.builder()
              .gitCommandRequest(gitDiffRequest)
              .gitCommandResult(gitDiffResult)
              .gitCommandStatus(GitCommandStatus.SUCCESS)
              .build();
        case VALIDATE:
          logger.info(GIT_YAML_LOG_PREFIX + " Processing Git command: VALIDATE");
          String errorMessage = gitClient.validate(gitConfig, true);
          if (errorMessage == null) {
            return GitCommandExecutionResponse.builder().gitCommandStatus(GitCommandStatus.SUCCESS).build();
          } else {
            return GitCommandExecutionResponse.builder()
                .gitCommandStatus(GitCommandStatus.FAILURE)
                .errorMessage(errorMessage)
                .build();
          }
        case FETCH_FILES:
          GitFetchFilesRequest gitFetchFilesRequest = (GitFetchFilesRequest) parameters[3];
          return getFilesFromGitUsingPath(gitFetchFilesRequest, gitConfig);
        default:
          return GitCommandExecutionResponse.builder()
              .gitCommandStatus(GitCommandStatus.FAILURE)
              .errorMessage(GIT_YAML_LOG_PREFIX + "Git Operation not supported")
              .build();
      }
    } catch (Exception ex) {
      logger.error(GIT_YAML_LOG_PREFIX + "Exception in processing GitTask", ex);
      GitCommandExecutionResponseBuilder builder = GitCommandExecutionResponse.builder()
                                                       .gitCommandStatus(GitCommandStatus.FAILURE)
                                                       .errorMessage(ex.getMessage());

      if (ex instanceof WingsException
          && ((WingsException) ex).getParams().containsKey(ErrorCode.GIT_CONNECTION_ERROR.name())) {
        builder.errorCode(ErrorCode.GIT_CONNECTION_ERROR);
      }
      return builder.build();
    }
  }

  private GitCommandExecutionResponse getFilesFromGitUsingPath(GitFetchFilesRequest gitRequest, GitConfig gitConfig) {
    try {
      GitFetchFilesResult gitResult = gitClient.fetchFilesByPath(gitConfig, gitRequest);
      return GitCommandExecutionResponse.builder()
          .gitCommandRequest(gitRequest)
          .gitCommandResult(gitResult)
          .gitCommandStatus(GitCommandStatus.SUCCESS)
          .build();
    } catch (Exception e) {
      return GitCommandExecutionResponse.builder()
          .gitCommandRequest(gitRequest)
          .errorMessage(e.getMessage())
          .gitCommandStatus(GitCommandStatus.FAILURE)
          .build();
    }
  }

  private void decryptGitConfig(GitConfig gitConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      encryptionService.decrypt(gitConfig, encryptionDetails);
    } catch (Exception ex) {
      logger.error(GIT_YAML_LOG_PREFIX + "Exception in processing GitTask, decryption of git config failed: ", ex);
      throw new WingsException("Decryption of git config failed: " + ex.getMessage(), USER_ADMIN)
          .addParam(ErrorCode.GIT_CONNECTION_ERROR.name(), ErrorCode.GIT_CONNECTION_ERROR);
    }
  }
}
