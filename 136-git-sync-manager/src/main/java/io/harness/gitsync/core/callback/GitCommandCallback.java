package io.harness.gitsync.core.callback;

import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.git.GitCommandType.COMMIT_AND_PUSH;
import static io.harness.delegate.beans.git.GitCommandType.DIFF;
import static io.harness.gitsync.common.YamlProcessingLogContext.CHANGESET_ID;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.mongodb.DuplicateKeyException;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandType;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.CommitAndPushResult;
import io.harness.git.model.GitBaseResult;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlChangeSet.Status;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.beans.GitCommit;
import io.harness.gitsync.core.service.GitCommitService;
import io.harness.gitsync.core.service.YamlChangeSetService;
import io.harness.gitsync.core.service.YamlGitService;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity;
import io.harness.gitsync.gitfileactivity.service.GitSyncService;
import io.harness.gitsync.gitsyncerror.service.GitSyncErrorService;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.waiter.NotifyCallback;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

  @Override
  public void notify(Map<String, ResponseData> response) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new GitCommandCallbackLogContext(getContext(), OVERRIDE_ERROR)) {
      logger.info("Git command response [{}]", response);

      ResponseData notifyResponseData = response.values().iterator().next();
      if (notifyResponseData instanceof GitCommandExecutionResponse) {
        GitCommandExecutionResponse gitCommandExecutionResponse = (GitCommandExecutionResponse) notifyResponseData;
        GitBaseResult gitCommandResult = gitCommandExecutionResponse.getGitCommandResult();

        if (gitCommandExecutionResponse.getGitCommandStatus() == GitCommandExecutionResponse.GitCommandStatus.FAILURE) {
          if (changeSetId != null) {
            logger.warn("Git Command failed [{}]", gitCommandExecutionResponse.getErrorMessage());
            yamlChangeSetService.updateStatus(accountId, changeSetId, Status.FAILED);
          }
          return;
        }

        logger.info("Git command [type: {}] request completed with status [{}]", gitCommandType,
            gitCommandExecutionResponse.getGitCommandStatus());

        if (gitCommandType == GitCommandType.COMMIT_AND_PUSH) {
          handleGitCommitAndPush(gitCommandExecutionResponse, (CommitAndPushResult) gitCommandResult);
        } else if (gitCommandType == DIFF) {
          // TODO(abhinav): add diff flow
        } else {
          logger.warn("Unexpected commandType result: [{}]", gitCommandExecutionResponse.getErrorMessage());
          yamlChangeSetService.updateStatus(accountId, changeSetId, Status.FAILED);
        }
      } else {
        logger.warn("Unexpected notify response data: [{}]", notifyResponseData);
        updateChangeSetFailureStatusSafely();
      }
    }
  }

  private void handleGitCommitAndPush(
      GitCommandExecutionResponse gitCommandExecutionResponse, CommitAndPushResult gitCommandResult) {
    CommitAndPushResult gitCommitAndPushResult = gitCommandResult;
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
            processingCommitId, accountId, false, GitCommit.Status.COMPLETED);
      }
      yamlGitService.removeGitSyncErrors(accountId, yamlChangeSet.get().getOrganizationId(),
          yamlChangeSet.get().getProjectId(),
          getAllFilesSuccessFullyProccessed(yamlChangeSet.get().getGitFileChanges(), filesCommited), false);
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
                      .gitCommandResult(gitCommitAndPushResult)
                      .gitConnectorId(yamlGitConfig.getGitConnectorId())
                      .branchName(yamlGitConfig.getBranch())
                      .yamlChangeSetsProcessed(yamlSetIdsProcessed)
                      .commitMessage(gitCommitAndPushResult.getGitCommitResult().getCommitMessage())
                      .build());
  }

  private GitCommit saveGitCommit(GitCommit gitCommit) {
    GitCommit gitCommitSaved = null;
    try {
      gitCommitSaved = gitCommitService.save(gitCommit);
    } catch (Exception e) {
      if (e instanceof DuplicateKeyException) {
        logger.info("This was already persisted in DB. May Happens when 2 successive commits"
            + " are made to git in short duration, and when 2nd commit is done before gitDiff"
            + " for 1st one is in progress");
      } else {
        logger.warn("Failed to save gitCommit", e);
        // Try again without gitChangeSet and CommandResults.
        gitCommit.setGitCommandResult(null);

        gitCommitSaved = gitCommitService.save(gitCommit);
      }
    }
    return gitCommitSaved;
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    logger.warn("Git request failed for command:[{}], changeSetId:[{}], account:[{}], response:[{}]", gitCommandType,
        changeSetId, accountId, response);
    updateChangeSetFailureStatusSafely();
  }

  protected void updateChangeSetFailureStatusSafely() {
    if (isNotEmpty(changeSetId) && COMMIT_AND_PUSH == gitCommandType) {
      yamlChangeSetService.updateStatus(accountId, changeSetId, Status.FAILED);
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
