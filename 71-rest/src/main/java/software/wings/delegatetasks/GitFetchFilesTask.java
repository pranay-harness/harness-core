package software.wings.delegatetasks;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.K8sDummyCommandUnit.FetchFiles;

import com.google.inject.Inject;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.protocol.TaskParameters;
import io.harness.exception.ExceptionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.GitFileConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
    executionLogCallback.saveExecutionLog("Fetching files from git");

    try {
      Map<String, GitFetchFilesResult> filesFromMultipleRepo = new HashMap<>();

      for (Entry<String, GitFetchFilesConfig> entry : taskParams.getGitFetchFilesConfigMap().entrySet()) {
        GitFetchFilesConfig gitFetchFileConfig = entry.getValue();

        GitFetchFilesResult gitFetchFilesResult = fetchFilesFromRepo(gitFetchFileConfig.getGitFileConfig(),
            gitFetchFileConfig.getGitConfig(), gitFetchFileConfig.getEncryptedDataDetails(), executionLogCallback);

        filesFromMultipleRepo.put(entry.getKey(), gitFetchFilesResult);
      }

      if (taskParams.isFinalState()) {
        executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      }

      return GitCommandExecutionResponse.builder()
          .gitCommandResult(
              GitFetchFilesFromMultipleRepoResult.builder().filesFromMultipleRepo(filesFromMultipleRepo).build())
          .gitCommandStatus(GitCommandStatus.SUCCESS)
          .build();
    } catch (Exception ex) {
      String msg = "Exception in processing GitFetchFilesTask. " + ExceptionUtils.getMessage(ex);
      logger.error(msg, ex);
      executionLogCallback.saveExecutionLog(msg, ERROR, CommandExecutionStatus.FAILURE);
      return GitCommandExecutionResponse.builder().errorMessage(msg).gitCommandStatus(GitCommandStatus.FAILURE).build();
    }
  }

  private GitFetchFilesResult fetchFilesFromRepo(GitFileConfig gitFileConfig, GitConfig gitConfig,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    encryptionService.decrypt(gitConfig, encryptedDataDetails);

    executionLogCallback.saveExecutionLog("\nGit connector Url: " + gitConfig.getRepoUrl());
    if (gitFileConfig.isUseBranch()) {
      executionLogCallback.saveExecutionLog("Branch: " + gitFileConfig.getBranch());
    } else {
      executionLogCallback.saveExecutionLog("CommitId: " + gitFileConfig.getCommitId());
    }
    executionLogCallback.saveExecutionLog("\nFetching " + gitFileConfig.getFilePath());

    String filePath = isBlank(gitFileConfig.getFilePath()) ? "" : gitFileConfig.getFilePath();
    GitFetchFilesResult gitFetchFilesResult = gitService.fetchFilesByPath(gitConfig, gitFileConfig.getConnectorId(),
        gitFileConfig.getCommitId(), gitFileConfig.getBranch(), asList(filePath), gitFileConfig.isUseBranch());
    executionLogCallback.saveExecutionLog("Successfully fetched " + gitFileConfig.getFilePath());

    return gitFetchFilesResult;
  }

  @Override
  public GitCommandExecutionResponse run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }
}
