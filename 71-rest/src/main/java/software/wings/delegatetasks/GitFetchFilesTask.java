package software.wings.delegatetasks;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.Log.LogLevel.WARN;
import static software.wings.beans.command.K8sDummyCommandUnit.FetchFiles;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.GitFileConfig;
import software.wings.beans.Log;
import software.wings.beans.Log.LogColor;
import software.wings.beans.Log.LogWeight;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;

import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class GitFetchFilesTask extends AbstractDelegateRunnableTask {
  @Inject private GitService gitService;
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService delegateLogService;

  public static final int GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT = 10;

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

    Map<String, GitFetchFilesResult> filesFromMultipleRepo = new HashMap<>();

    for (Entry<String, GitFetchFilesConfig> entry : taskParams.getGitFetchFilesConfigMap().entrySet()) {
      executionLogCallback.saveExecutionLog(
          Log.color("\nFetching values files from git for " + entry.getKey(), LogColor.White, LogWeight.Bold));

      GitFetchFilesConfig gitFetchFileConfig = entry.getValue();
      String k8ValuesLocation = entry.getKey();
      GitFetchFilesResult gitFetchFilesResult;

      try {
        gitFetchFilesResult = fetchFilesFromRepo(gitFetchFileConfig.getGitFileConfig(),
            gitFetchFileConfig.getGitConfig(), gitFetchFileConfig.getEncryptedDataDetails(), executionLogCallback);
      } catch (Exception ex) {
        String exceptionMsg = ExceptionUtils.getMessage(ex);

        // Values.yaml in service spec is optional.
        if (K8sValuesLocation.Service.toString().equals(k8ValuesLocation)
            && ex.getCause() instanceof NoSuchFileException) {
          logger.info(exceptionMsg, ex);
          executionLogCallback.saveExecutionLog(exceptionMsg, WARN);
          continue;
        }

        String msg = "Exception in processing GitFetchFilesTask. " + ExceptionUtils.getMessage(ex);
        logger.error(msg, ex);
        executionLogCallback.saveExecutionLog(msg, ERROR, CommandExecutionStatus.FAILURE);
        return GitCommandExecutionResponse.builder()
            .errorMessage(msg)
            .gitCommandStatus(GitCommandStatus.FAILURE)
            .build();
      }

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
