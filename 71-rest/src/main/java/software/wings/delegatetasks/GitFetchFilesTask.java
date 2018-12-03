package software.wings.delegatetasks;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.K8sDummyCommandUnit.FetchFiles;

import com.google.inject.Inject;

import io.harness.delegate.task.protocol.TaskParameters;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.GitFileConfig;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class GitFetchFilesTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(GitFetchFilesTask.class);

  @Inject private GitService gitService;
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService delegateLogService;

  public GitFetchFilesTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public GitCommandExecutionResponse run(TaskParameters parameters) {
    GitFetchFilesTaskParams taskParams = (GitFetchFilesTaskParams) parameters;

    logger.info(format("Running GitFetchFilesTask for account %s, app %s, activityId %s", taskParams.getAccountId(),
        taskParams.getAppId(), taskParams.getActivityId()));

    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(
        delegateLogService, taskParams.getAccountId(), taskParams.getAppId(), taskParams.getActivityId(), FetchFiles);
    executionLogCallback.saveExecutionLog("Fetching files from git\n");

    try {
      encryptionService.decrypt(taskParams.getGitConfig(), taskParams.getEncryptedDataDetails());

      GitFileConfig gitFileConfig = taskParams.getGitFileConfig();

      executionLogCallback.saveExecutionLog("Git connector Url: " + taskParams.getGitConfig().getRepoUrl());
      if (gitFileConfig.isUseBranch()) {
        executionLogCallback.saveExecutionLog("Branch: " + gitFileConfig.getBranch());
      } else {
        executionLogCallback.saveExecutionLog("CommitId: " + gitFileConfig.getCommitId());
      }
      executionLogCallback.saveExecutionLog("\nFetching " + gitFileConfig.getFilePath());

      GitFetchFilesResult gitFetchFilesResult = gitService.fetchFilesByPath(taskParams.getGitConfig(),
          gitFileConfig.getConnectorId(), gitFileConfig.getCommitId(), gitFileConfig.getBranch(),
          asList(gitFileConfig.getFilePath()), gitFileConfig.isUseBranch());
      executionLogCallback.saveExecutionLog("Successfully fetched " + gitFileConfig.getFilePath());

      if (taskParams.isFinalState()) {
        executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      }

      return GitCommandExecutionResponse.builder()
          .gitCommandResult(gitFetchFilesResult)
          .gitCommandStatus(GitCommandStatus.SUCCESS)
          .build();
    } catch (Exception ex) {
      logger.error("Exception in processing GitFetchFilesTask", ex);
      executionLogCallback.saveExecutionLog(ex.getMessage(), ERROR, CommandExecutionStatus.FAILURE);
      return GitCommandExecutionResponse.builder()
          .errorMessage(ex.getMessage())
          .gitCommandStatus(GitCommandStatus.FAILURE)
          .build();
    }
  }

  @Override
  public GitCommandExecutionResponse run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }
}
