package software.wings.delegatetasks;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.GitConfig;
import software.wings.beans.yaml.GitCommand.GitCommandType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.beans.yaml.GitCommitAndPushResult;
import software.wings.beans.yaml.GitCommitRequest;
import software.wings.beans.yaml.GitDiffRequest;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by anubhaw on 10/26/17.
 */
public class GitCommandTask extends AbstractDelegateRunnableTask {
  @Inject private GitClient gitClient;
  @Inject private EncryptionService encryptionService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  public GitCommandTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public GitCommandExecutionResponse run(Object[] parameters) {
    GitCommandType gitCommandType = (GitCommandType) parameters[0];
    GitConfig gitConfig = (GitConfig) parameters[1];
    List<EncryptedDataDetail> encryptionDetails = (List<EncryptedDataDetail>) parameters[2];
    encryptionService.decrypt(gitConfig, encryptionDetails);

    try {
      switch (gitCommandType) {
        case COMMIT_AND_PUSH:
          GitCommitRequest gitCommitRequest = (GitCommitRequest) parameters[3];
          logger.info("COMMIT_AND_PUSH: [{}]", gitCommitRequest);
          GitCommitAndPushResult gitCommitAndPushResult = gitClient.commitAndPush(gitConfig, gitCommitRequest);
          return GitCommandExecutionResponse.builder()
              .gitCommandRequest(gitCommitRequest)
              .gitCommandResult(gitCommitAndPushResult)
              .gitCommandStatus(GitCommandStatus.SUCCESS)
              .build();
        case DIFF:
          GitDiffRequest gitDiffRequest = (GitDiffRequest) parameters[3];
          logger.info("DIFF: [{}]", gitDiffRequest);
          GitDiffResult gitDiffResult = gitClient.diff(gitConfig, gitDiffRequest.getLastProcessedCommitId());
          return GitCommandExecutionResponse.builder()
              .gitCommandRequest(gitDiffRequest)
              .gitCommandResult(gitDiffResult)
              .gitCommandStatus(GitCommandStatus.SUCCESS)
              .build();
        case VALIDATE:
          String errorMessage = gitClient.validate(gitConfig);
          if (errorMessage == null) {
            return GitCommandExecutionResponse.builder().gitCommandStatus(GitCommandStatus.SUCCESS).build();
          } else {
            return GitCommandExecutionResponse.builder()
                .gitCommandStatus(GitCommandStatus.FAILURE)
                .errorMessage(errorMessage)
                .build();
          }
        default:
          return GitCommandExecutionResponse.builder()
              .gitCommandStatus(GitCommandStatus.FAILURE)
              .errorMessage("Git Operation not supported")
              .build();
      }
    } catch (Exception ex) {
      logger.error("Exception in processing GitTask", ex);
      return GitCommandExecutionResponse.builder()
          .gitCommandStatus(GitCommandStatus.FAILURE)
          .errorMessage(ex.getMessage())
          .build();
    }
  }
}
