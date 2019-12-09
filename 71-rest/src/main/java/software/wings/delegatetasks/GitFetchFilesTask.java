package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Log.LogColor.Gray;
import static software.wings.beans.Log.LogColor.White;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.Log.LogLevel.WARN;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;
import static software.wings.beans.command.K8sDummyCommandUnit.FetchFiles;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.GitFileConfig;
import software.wings.beans.Log.LogColor;
import software.wings.beans.Log.LogWeight;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;

import java.nio.file.NoSuchFileException;
import java.util.Collections;
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

    logger.info("Running GitFetchFilesTask for account {}, app {}, activityId {}", taskParams.getAccountId(),
        taskParams.getAppId(), taskParams.getActivityId());

    String executionLogName = isEmpty(taskParams.getExecutionLogName()) ? FetchFiles : taskParams.getExecutionLogName();

    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(delegateLogService, taskParams.getAccountId(),
        taskParams.getAppId(), taskParams.getActivityId(), executionLogName);

    AppManifestKind appManifestKind = taskParams.getAppManifestKind();
    Map<String, GitFetchFilesResult> filesFromMultipleRepo = new HashMap<>();

    for (Entry<String, GitFetchFilesConfig> entry : taskParams.getGitFetchFilesConfigMap().entrySet()) {
      executionLogCallback.saveExecutionLog(
          color(format("%nFetching %s files from git for %s", getFileTypeMessage(appManifestKind), entry.getKey()),
              LogColor.White, LogWeight.Bold));

      GitFetchFilesConfig gitFetchFileConfig = entry.getValue();
      String k8ValuesLocation = entry.getKey();
      GitFetchFilesResult gitFetchFilesResult;

      try {
        gitFetchFilesResult = fetchFilesFromRepo(gitFetchFileConfig.getGitFileConfig(),
            gitFetchFileConfig.getGitConfig(), gitFetchFileConfig.getEncryptedDataDetails(), executionLogCallback);
      } catch (Exception ex) {
        String exceptionMsg = ex.getMessage();

        // Values.yaml in service spec is optional.
        if (AppManifestKind.VALUES.equals(appManifestKind)
            && K8sValuesLocation.Service.toString().equals(k8ValuesLocation)
            && ex.getCause() instanceof NoSuchFileException) {
          logger.info("Values.yaml file not found. " + exceptionMsg, ex);
          executionLogCallback.saveExecutionLog(exceptionMsg, WARN);
          continue;
        }

        String msg = "Exception in processing GitFetchFilesTask. " + exceptionMsg;
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

    executionLogCallback.saveExecutionLog("Git connector Url: " + gitConfig.getRepoUrl());
    if (gitFileConfig.isUseBranch()) {
      executionLogCallback.saveExecutionLog("Branch: " + gitFileConfig.getBranch());
    } else {
      executionLogCallback.saveExecutionLog("CommitId: " + gitFileConfig.getCommitId());
    }

    List<String> filePathsToFetch = null;
    if (EmptyPredicate.isNotEmpty(gitFileConfig.getFilePathList())) {
      filePathsToFetch = gitFileConfig.getFilePathList();
      executionLogCallback.saveExecutionLog("\nFetching following Files :");
      printFileNamesInExecutionLogs(filePathsToFetch, executionLogCallback);
    } else {
      executionLogCallback.saveExecutionLog("\nFetching " + gitFileConfig.getFilePath());
      String filePath = isBlank(gitFileConfig.getFilePath()) ? "" : gitFileConfig.getFilePath();
      filePathsToFetch = Collections.singletonList(filePath);
    }

    GitFetchFilesResult gitFetchFilesResult = gitService.fetchFilesByPath(gitConfig, gitFileConfig.getConnectorId(),
        gitFileConfig.getCommitId(), gitFileConfig.getBranch(), filePathsToFetch, gitFileConfig.isUseBranch());
    printFileNamesInExecutionLogs(gitFetchFilesResult, executionLogCallback);

    return gitFetchFilesResult;
  }

  @Override
  public GitCommandExecutionResponse run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  private String getFileTypeMessage(AppManifestKind appManifestKind) {
    if (appManifestKind == null) {
      return "";
    }

    switch (appManifestKind) {
      case VALUES:
        return "Values";

      case PCF_OVERRIDE:
      case K8S_MANIFEST:
        return "manifest";

      default:
        unhandled(appManifestKind);
        return "";
    }
  }

  private void printFileNamesInExecutionLogs(
      GitFetchFilesResult gitFetchFilesResult, ExecutionLogCallback executionLogCallback) {
    if (gitFetchFilesResult == null || EmptyPredicate.isEmpty(gitFetchFilesResult.getFiles())) {
      return;
    }

    StringBuilder sb = new StringBuilder(1024);
    gitFetchFilesResult.getFiles().forEach(
        each -> sb.append(color(format("- %s", each.getFilePath()), Gray)).append(System.lineSeparator()));

    executionLogCallback.saveExecutionLog(color("Successfully fetched following files:", White, Bold));
    executionLogCallback.saveExecutionLog(sb.toString());
  }

  private void printFileNamesInExecutionLogs(List<String> filePathList, ExecutionLogCallback executionLogCallback) {
    if (EmptyPredicate.isEmpty(filePathList)) {
      return;
    }

    StringBuilder sb = new StringBuilder(1024);
    filePathList.forEach(filePath -> sb.append(color(format("- %s", filePath), Gray)).append(System.lineSeparator()));

    executionLogCallback.saveExecutionLog(sb.toString());
  }
}
