package io.harness.gitsync.core.callback;

import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.git.GitCommandType.COMMIT_AND_PUSH;
import static io.harness.delegate.beans.git.GitCommandType.DIFF;
import static io.harness.gitsync.common.YamlProcessingLogContext.CHANGESET_ID;
import static io.harness.gitsync.gitsyncerror.utils.GitSyncErrorUtils.getCommitIdOfError;
import static io.harness.gitsync.gitsyncerror.utils.GitSyncErrorUtils.getCommitMessageOfError;
import static io.harness.gitsync.gitsyncerror.utils.GitSyncErrorUtils.getCommitTimeOfError;
import static io.harness.gitsync.gitsyncerror.utils.GitSyncErrorUtils.getYamlContentOfError;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandType;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.eraro.ErrorCode;
import io.harness.git.model.CommitAndPushResult;
import io.harness.git.model.DiffResult;
import io.harness.git.model.GitBaseResult;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlChangeSet.Status;
import io.harness.gitsync.common.helper.GitFileLocationHelper;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.beans.GitCommit;
import io.harness.gitsync.core.beans.GitWebhookRequestAttributes;
import io.harness.gitsync.core.impl.GitChangeSetProcessor;
import io.harness.gitsync.core.service.GitCommitService;
import io.harness.gitsync.core.service.YamlChangeSetService;
import io.harness.gitsync.core.service.YamlGitService;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity;
import io.harness.gitsync.gitfileactivity.service.GitSyncService;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.service.GitSyncErrorService;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.tasks.ResponseData;
import io.harness.waiter.NotifyCallback;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.mongodb.DuplicateKeyException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GitCommandCallback implements NotifyCallback {
  private String accountId;
  private String changeSetId;
  private GitCommandType gitCommandType;
  private String gitConnectorId;
  private String branchName;
  private String repo;
  private YamlGitConfigDTO yamlGitConfig;

  public GitCommandCallback() {}

  public GitCommandCallback(String accountId, String changeSetId, GitCommandType gitCommandType, String gitConnectorId,
      String repo, String branchName, YamlGitConfigDTO yamlGitConfig) {
    this.accountId = accountId;
    this.changeSetId = changeSetId;
    this.gitCommandType = gitCommandType;
    this.gitConnectorId = gitConnectorId;
    this.repo = repo;
    this.branchName = branchName;
    this.yamlGitConfig = yamlGitConfig;
  }

  @Inject private transient YamlChangeSetService yamlChangeSetService;
  @Inject private transient YamlGitService yamlGitService;
  @Inject private transient GitSyncErrorService gitSyncErrorService;
  @Inject private transient GitSyncService gitSyncService;
  @Inject private transient YamlGitConfigService yamlGitConfigService;
  @Inject private transient GitCommitService gitCommitService;
  @Inject private transient GitChangeSetProcessor gitChangeSetProcessor;

  @Override
  public void notify(Map<String, ResponseData> response) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new GitCommandCallbackLogContext(getContext(), OVERRIDE_ERROR)) {
      log.info("Git command response [{}]", response);

      DelegateResponseData notifyResponseData = (DelegateResponseData) response.values().iterator().next();
      if (notifyResponseData instanceof GitCommandExecutionResponse) {
        GitCommandExecutionResponse gitCommandExecutionResponse = (GitCommandExecutionResponse) notifyResponseData;
        GitBaseResult gitCommandResult = gitCommandExecutionResponse.getGitCommandResult();

        if (gitCommandExecutionResponse.getGitCommandStatus() == GitCommandExecutionResponse.GitCommandStatus.FAILURE) {
          if (gitCommandType == DIFF) {
            handleDiffCommandFailure(gitCommandExecutionResponse.getErrorCode(), accountId);
          }
          if (changeSetId != null) {
            log.warn("Git Command failed [{}]", gitCommandExecutionResponse.getErrorMessage());
            yamlChangeSetService.updateStatus(accountId, changeSetId, Status.FAILED);
          }
          return;
        }

        log.info("Git command [type: {}] request completed with status [{}]", gitCommandType,
            gitCommandExecutionResponse.getGitCommandStatus());

        if (gitCommandType == GitCommandType.COMMIT_AND_PUSH) {
          handleGitCommitAndPush(gitCommandExecutionResponse, (CommitAndPushResult) gitCommandResult);
        } else if (gitCommandType == DIFF) {
          handleGitDiff((DiffResult) gitCommandResult);
        } else {
          log.warn("Unexpected commandType result: [{}]", gitCommandExecutionResponse.getErrorMessage());
          yamlChangeSetService.updateStatus(accountId, changeSetId, Status.FAILED);
        }
      } else {
        log.warn("Unexpected notify response data: [{}]", notifyResponseData);
        updateChangeSetFailureStatusSafely();
      }
    }
  }

  private void handleGitDiff(DiffResult gitCommandResult) {
    try {
      DiffResult gitDiffResult = gitCommandResult;

      if (isNotEmpty(gitDiffResult.getGitFileChanges())) {
        addActiveGitSyncErrorsToProcessAgain(gitCommandResult, accountId);
      } else {
        log.info("No file changes found in git diff. Skip adding active errors for processing");
      }
      gitChangeSetProcessor.processGitChangeSet(accountId, gitDiffResult, gitConnectorId, repo, branchName);
    } catch (Exception e) {
      log.error("error while processing diff request", e);
      yamlChangeSetService.updateStatus(accountId, changeSetId, Status.FAILED);
      handleDiffCommandFailure(null, accountId);
    }
  }

  private void handleGitCommitAndPush(
      GitCommandExecutionResponse gitCommandExecutionResponse, CommitAndPushResult gitCommitAndPushResult) {
    Optional<YamlChangeSet> yamlChangeSet = yamlChangeSetService.get(accountId, changeSetId);
    List<GitFileChange> filesCommited = Collections.emptyList();
    if (yamlChangeSet.isPresent()) {
      yamlChangeSetService.updateStatus(accountId, changeSetId, Status.COMPLETED);
      if (gitCommitAndPushResult.getGitCommitResult().getCommitId() != null) {
        String yamlSetIdProcessed = changeSetId;

        saveCommitFromHarness(gitCommitAndPushResult, yamlChangeSet.get(), yamlSetIdProcessed, yamlGitConfig);
        final String processingCommitId = gitCommitAndPushResult.getGitCommitResult().getCommitId();
        final String processingCommitMessage = gitCommitAndPushResult.getGitCommitResult().getCommitMessage();
        filesCommited = new ArrayList<>(emptyIfNull(gitCommitAndPushResult.getFilesCommittedToGit()));
        gitSyncService.logActivityForGitOperation(filesCommited, GitFileActivity.Status.SUCCESS, false,
            yamlChangeSet.get().isFullSync(), "", processingCommitId, processingCommitMessage, yamlGitConfig);
        gitSyncService.createGitFileActivitySummaryForCommit(
            processingCommitId, accountId, false, GitCommit.Status.COMPLETED, yamlGitConfig);
      }
      yamlGitService.removeGitSyncErrors(accountId, yamlChangeSet.get().getOrganizationId(),
          yamlChangeSet.get().getProjectId(),
          getAllFilesSuccessFullyProccessed(yamlChangeSet.get().getGitFileChanges(), filesCommited), false);
    }
  }

  private List<GitFileChange> getActiveGitSyncErrorFiles(
      String accountId, String branchName, String repoName, String gitConnectorId) {
    final long _30_days_millis = System.currentTimeMillis() - Duration.ofDays(30).toMillis();
    return gitSyncErrorService
        .getActiveGitToHarnessSyncErrors(accountId, gitConnectorId, repoName, branchName, null, _30_days_millis)
        .stream()
        .map(this::convertToGitFileChange)
        .collect(Collectors.toList());
  }

  private GitFileChange convertToGitFileChange(GitSyncError gitSyncError) {
    return GitFileChange.builder()
        .filePath(gitSyncError.getYamlFilePath())
        .fileContent(getYamlContentOfError(gitSyncError))
        .accountId(gitSyncError.getAccountId())
        .changeType(gitSyncError.getChangeType())
        .syncFromGit(true)
        .commitId(getCommitIdOfError(gitSyncError))
        .changeFromAnotherCommit(Boolean.TRUE)
        .commitTimeMs(getCommitTimeOfError(gitSyncError))
        .commitMessage(getCommitMessageOfError(gitSyncError))
        .rootPath(GitFileLocationHelper.getRootPathSafely(gitSyncError.getYamlFilePath()))
        .build();
  }

  @VisibleForTesting
  void addActiveGitSyncErrorsToProcessAgain(final DiffResult gitDiffResult, final String accountId) {
    final List<GitFileChange> activeGitSyncErrorFiles =
        emptyIfNull(getActiveGitSyncErrorFiles(accountId, branchName, repo, gitConnectorId));

    log.info("Active git sync error files =[{}]",
        activeGitSyncErrorFiles.stream().map(GitFileChange::getFilePath).collect(Collectors.toList()));

    if (isNotEmpty(activeGitSyncErrorFiles)) {
      final Set<String> filesAlreadyInDiffSet =
          gitDiffResult.getGitFileChanges().stream().map(GitFileChange::getFilePath).collect(Collectors.toSet());

      final List<GitFileChange> activeErrorsNotInDiff =
          activeGitSyncErrorFiles.stream()
              .filter(gitFileChange -> !filesAlreadyInDiffSet.contains(gitFileChange.getFilePath()))
              .collect(Collectors.toList());

      log.info("Active git sync error files not in diff =[{}]",
          activeErrorsNotInDiff.stream().map(GitFileChange::getFilePath).collect(Collectors.toList()));
      activeErrorsNotInDiff.forEach(gitDiffResult::addChangeFile);
    }
  }

  private void saveCommitFromHarness(CommitAndPushResult gitCommitAndPushResult, YamlChangeSet yamlChangeSet,
      String yamlSetIdsProcessed, YamlGitConfigDTO yamlGitConfig) {
    String commitId = gitCommitAndPushResult.getGitCommitResult().getCommitId();

    saveGitCommit(GitCommit.builder()
                      .accountId(accountId)
                      .yamlChangeSetId(yamlChangeSet.getUuid())
                      .status(GitCommit.Status.COMPLETED)
                      .commitId(commitId)
                      .gitConnectorId(yamlGitConfig.getGitConnectorId())
                      .branchName(yamlGitConfig.getBranch())
                      .yamlChangeSetId(yamlSetIdsProcessed)
                      .commitMessage(gitCommitAndPushResult.getGitCommitResult().getCommitMessage())
                      .build());
  }

  private GitCommit saveGitCommit(GitCommit gitCommit) {
    GitCommit gitCommitSaved = null;
    try {
      gitCommitSaved = gitCommitService.save(gitCommit);
    } catch (Exception e) {
      if (e instanceof DuplicateKeyException) {
        log.info("This was already persisted in DB. May Happens when 2 successive commits"
            + " are made to git in short duration, and when 2nd commit is done before gitDiff"
            + " for 1st one is in progress");
      } else {
        log.warn("Failed to save gitCommit", e);
        // Try again without gitChangeSet and CommandResults.
        gitCommitSaved = gitCommitService.save(gitCommit);
      }
    }
    return gitCommitSaved;
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    log.warn("Git request failed for command:[{}], changeSetId:[{}], account:[{}], response:[{}]", gitCommandType,
        changeSetId, accountId, response);
    updateChangeSetFailureStatusSafely();
    updateGitCommitFailureSafely();
  }

  protected void updateChangeSetFailureStatusSafely() {
    if (isNotEmpty(changeSetId) && COMMIT_AND_PUSH == gitCommandType) {
      yamlChangeSetService.updateStatus(accountId, changeSetId, Status.FAILED);
    }
  }

  protected void updateGitCommitFailureSafely() {
    if (DIFF == gitCommandType) {
      handleDiffCommandFailure(null, accountId);
    }
  }

  private ImmutableMap<String, String> getContext() {
    final ImmutableMap.Builder<String, String> context = ImmutableMap.builder();
    context.put("gitCommandCallBackType", gitCommandType.toString());

    if (isNotBlank(changeSetId)) {
      context.put(CHANGESET_ID, changeSetId);
    }

    return context.build();
  }

  private void handleDiffCommandFailure(ErrorCode errorCode, String accountId) {
    if (isNotEmpty(changeSetId)) {
      final Optional<YamlChangeSet> yamlChangeSet = yamlChangeSetService.get(accountId, changeSetId);
      if (!yamlChangeSet.isPresent()) {
        log.error("no changeset found with id =[{}]", changeSetId);
        return;
      }
      final GitWebhookRequestAttributes gitWebhookRequestAttributes =
          yamlChangeSet.get().getGitWebhookRequestAttributes();
      if (isValid(gitWebhookRequestAttributes)) {
        final String headCommitId = gitWebhookRequestAttributes.getHeadCommitId();

        GitCommit.Status gitCommitStatus = GitCommit.Status.FAILED;
        if (ErrorCode.GIT_DIFF_COMMIT_NOT_IN_ORDER == errorCode) {
          gitCommitStatus = GitCommit.Status.SKIPPED;
        }
        GitCommit gitCommit =
            saveFailedCommitFromGit(headCommitId, accountId, gitCommitStatus, yamlGitConfig, changeSetId);
        gitSyncService.createGitFileSummaryForFailedOrSkippedCommit(gitCommit, true);
      }
    }
  }

  private boolean isValid(GitWebhookRequestAttributes gitWebhookRequestAttributes) {
    return gitWebhookRequestAttributes != null && isNotEmpty(gitWebhookRequestAttributes.getHeadCommitId())
        && isNotEmpty(gitWebhookRequestAttributes.getBranchName())
        && isNotEmpty(gitWebhookRequestAttributes.getGitConnectorId());
  }

  private GitCommit saveFailedCommitFromGit(String commitId, String accountId, GitCommit.Status gitCommitStatus,
      YamlGitConfigDTO yamlGitConfig, String changeSetId) {
    return saveGitCommit(GitCommit.builder()
                             .accountId(accountId)
                             .status(gitCommitStatus)
                             .commitId(commitId)
                             .gitConnectorId(yamlGitConfig.getGitConnectorId())
                             .repo(yamlGitConfig.getRepo())
                             .branchName(yamlGitConfig.getBranch())
                             .fileProcessingSummary(null)
                             .yamlChangeSetId(changeSetId)
                             .projectId(yamlGitConfig.getProjectId())
                             .organizationId(yamlGitConfig.getOrganizationId())
                             .yamlGitConfigIds(yamlGitConfig.getIdentifier())
                             .build());
  }

  @VisibleForTesting
  List<GitFileChange> getAllFilesSuccessFullyProccessed(
      List<GitFileChange> fileChangesPartOfYamlChangeSet, List<GitFileChange> filesCommited) {
    List<GitFileChange> allFilesProcessed = new ArrayList<>(fileChangesPartOfYamlChangeSet);
    if (isEmpty(filesCommited)) {
      return allFilesProcessed;
    }
    Set<String> nameOfFilesProcessed =
        fileChangesPartOfYamlChangeSet.stream().map(GitFileChange::getFilePath).collect(Collectors.toSet());
    filesCommited.forEach(change -> {
      if (!nameOfFilesProcessed.contains(change.getFilePath())) {
        allFilesProcessed.add(change);
      }
    });
    return allFilesProcessed;
  }
}
