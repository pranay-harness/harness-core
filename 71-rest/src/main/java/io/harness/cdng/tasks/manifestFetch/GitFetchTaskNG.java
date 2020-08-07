package io.harness.cdng.tasks.manifestFetch;

import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.logging.LogLevel.WARN;
import static software.wings.beans.command.K8sDummyCommandUnit.FetchFiles;

import com.google.inject.Inject;

import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.tasks.manifestFetch.beans.GitFetchFilesConfig;
import io.harness.cdng.tasks.manifestFetch.beans.GitFetchRequest;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.delegate.task.TaskParameters;
import io.harness.logging.CommandExecutionStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.GitConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.GitFetchFilesTaskHelper;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;

import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public class GitFetchTaskNG extends AbstractDelegateRunnableTask {
  @Inject private GitService gitService;
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService delegateLogService;
  @Inject private GitFetchFilesTaskHelper gitFetchFilesTaskHelper;

  public static final int GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT = 10;

  public GitFetchTaskNG(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public GitCommandExecutionResponse run(TaskParameters parameters) {
    GitFetchRequest gitFetchRequest = (GitFetchRequest) parameters;

    logger.info("Running GitFetchFilesTask for activityId {}", gitFetchRequest.getActivityId());
    String executionLogName = EmptyPredicate.isEmpty(gitFetchRequest.getExecutionLogName())
        ? FetchFiles
        : gitFetchRequest.getExecutionLogName();

    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(delegateLogService,
        gitFetchRequest.getAccountId(), gitFetchRequest.getAppId(), gitFetchRequest.getActivityId(), executionLogName);

    Map<String, GitFetchFilesResult> filesFromMultipleRepo = new HashMap<>();
    List<GitFetchFilesConfig> gitFetchFilesConfigs = gitFetchRequest.getGitFetchFilesConfigs();

    for (GitFetchFilesConfig gitFetchFilesConfig : gitFetchFilesConfigs) {
      GitFetchFilesResult gitFetchFilesResult;

      try {
        gitFetchFilesResult = fetchFilesFromRepo(gitFetchFilesConfig, executionLogCallback);
      } catch (Exception ex) {
        String exceptionMsg = ex.getMessage();

        // Values.yaml in service spec is optional.
        if (ex.getCause() instanceof NoSuchFileException && gitFetchFilesConfig.isSucceedIfFileNotFound()) {
          logger.info("file not found. " + exceptionMsg, ex);
          executionLogCallback.saveExecutionLog(exceptionMsg, WARN);
          continue;
        }

        String msg = "Exception in processing GitFetchFilesTask. " + exceptionMsg;
        logger.error(msg, ex);
        executionLogCallback.saveExecutionLog(msg, ERROR, CommandExecutionStatus.FAILURE);
        return GitCommandExecutionResponse.builder()
            .errorMessage(exceptionMsg)
            .gitCommandStatus(GitCommandExecutionResponse.GitCommandStatus.FAILURE)
            .build();
      }

      filesFromMultipleRepo.put(gitFetchFilesConfig.getIdentifier(), gitFetchFilesResult);
    }

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);

    return GitCommandExecutionResponse.builder()
        .gitCommandResult(
            GitFetchFilesFromMultipleRepoResult.builder().filesFromMultipleRepo(filesFromMultipleRepo).build())
        .gitCommandStatus(GitCommandExecutionResponse.GitCommandStatus.SUCCESS)
        .build();
  }

  private GitFetchFilesResult fetchFilesFromRepo(
      GitFetchFilesConfig gitFetchFilesConfig, ExecutionLogCallback executionLogCallback) {
    GitStore gitStore = gitFetchFilesConfig.getGitStore();
    GitConfig gitConfig = gitFetchFilesConfig.getGitConfig();

    encryptionService.decrypt(gitConfig, gitFetchFilesConfig.getEncryptedDataDetails());

    executionLogCallback.saveExecutionLog("Git connector Url: " + gitConfig.getRepoUrl());
    executionLogCallback.saveExecutionLog(gitStore.getGitFetchType() + " : " + gitStore.getBranch());

    List<String> filePathsToFetch = null;
    if (EmptyPredicate.isNotEmpty(gitFetchFilesConfig.getPaths())) {
      filePathsToFetch = gitFetchFilesConfig.getPaths();
      executionLogCallback.saveExecutionLog("\nFetching following Files :");
      gitFetchFilesTaskHelper.printFileNamesInExecutionLogs(filePathsToFetch, executionLogCallback);
    }

    GitFetchFilesResult gitFetchFilesResult = gitService.fetchFilesByPath(gitConfig, gitStore.getConnectorIdentifier(),
        gitStore.getBranch(), gitStore.getBranch(), filePathsToFetch, FetchType.BRANCH == gitStore.getGitFetchType());

    gitFetchFilesTaskHelper.printFileNamesInExecutionLogs(gitFetchFilesResult, executionLogCallback);

    return gitFetchFilesResult;
  }

  @Override
  public GitCommandExecutionResponse run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }
}
