package io.harness.delegate.task.git;

import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.logging.LogLevel.WARN;

import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.git.NGGitService;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.git.model.FetchFilesResult;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.DummyLogCallbackImpl;
import io.harness.logging.LogCallback;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class GitFetchTaskNG extends AbstractDelegateRunnableTask {
  @Inject private NGGitService ngGitService;
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private GitFetchFilesTaskHelper gitFetchFilesTaskHelper;

  public static final int GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT = 10;

  public GitFetchTaskNG(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public GitFetchResponse run(TaskParameters parameters) {
    GitFetchRequest gitFetchRequest = (GitFetchRequest) parameters;

    log.info("Running GitFetchFilesTask for activityId {}", gitFetchRequest.getActivityId());

    LogCallback executionLogCallback = getLogCallBack();

    Map<String, FetchFilesResult> filesFromMultipleRepo = new HashMap<>();
    List<GitFetchFilesConfig> gitFetchFilesConfigs = gitFetchRequest.getGitFetchFilesConfigs();

    for (GitFetchFilesConfig gitFetchFilesConfig : gitFetchFilesConfigs) {
      FetchFilesResult gitFetchFilesResult;

      try {
        gitFetchFilesResult =
            fetchFilesFromRepo(gitFetchFilesConfig, executionLogCallback, gitFetchRequest.getAccountId());
      } catch (Exception ex) {
        String exceptionMsg = ex.getMessage();

        // Values.yaml in service spec is optional.
        if (ex.getCause() instanceof NoSuchFileException && gitFetchFilesConfig.isSucceedIfFileNotFound()) {
          log.info("file not found. " + exceptionMsg, ex);
          executionLogCallback.saveExecutionLog(exceptionMsg, WARN);
          continue;
        }

        String msg = "Exception in processing GitFetchFilesTask. " + exceptionMsg;
        log.error(msg, ex);
        executionLogCallback.saveExecutionLog(msg, ERROR, CommandExecutionStatus.FAILURE);
        return GitFetchResponse.builder().errorMessage(exceptionMsg).taskStatus(TaskStatus.FAILURE).build();
      }

      filesFromMultipleRepo.put(gitFetchFilesConfig.getIdentifier(), gitFetchFilesResult);
    }

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);

    return GitFetchResponse.builder()
        .taskStatus(TaskStatus.SUCCESS)
        .filesFromMultipleRepo(filesFromMultipleRepo)
        .build();
  }

  private LogCallback getLogCallBack() {
    // TODO VS/Anshul: update when logcallback is available, use `FetchFiles` as commandUnit Name
    return new DummyLogCallbackImpl();
  }

  private FetchFilesResult fetchFilesFromRepo(
      GitFetchFilesConfig gitFetchFilesConfig, LogCallback executionLogCallback, String accountId) {
    GitStoreDelegateConfig gitStoreDelegateConfig = gitFetchFilesConfig.getGitStoreDelegateConfig();
    GitConfigDTO gitConfigDTO = gitStoreDelegateConfig.getGitConfigDTO();

    secretDecryptionService.decrypt(gitConfigDTO.getGitAuth(), gitStoreDelegateConfig.getEncryptedDataDetails());

    executionLogCallback.saveExecutionLog("Git connector Url: " + gitConfigDTO.getUrl());
    String fetchTypeInfo = gitStoreDelegateConfig.getFetchType() == FetchType.BRANCH
        ? "Branch: " + gitStoreDelegateConfig.getBranch()
        : "CommitId: " + gitStoreDelegateConfig.getCommitId();

    executionLogCallback.saveExecutionLog(fetchTypeInfo);

    List<String> filePathsToFetch = null;
    if (EmptyPredicate.isNotEmpty(gitFetchFilesConfig.getGitStoreDelegateConfig().getPaths())) {
      filePathsToFetch = gitFetchFilesConfig.getGitStoreDelegateConfig().getPaths();
      executionLogCallback.saveExecutionLog("\nFetching following Files :");
      gitFetchFilesTaskHelper.printFileNamesInExecutionLogs(filePathsToFetch, executionLogCallback);
    }

    FetchFilesResult gitFetchFilesResult = ngGitService.fetchFilesByPath(gitStoreDelegateConfig, accountId);

    gitFetchFilesTaskHelper.printFileNamesInExecutionLogs(executionLogCallback, gitFetchFilesResult.getFiles());

    return gitFetchFilesResult;
  }

  @Override
  public GitFetchResponse run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }
}
