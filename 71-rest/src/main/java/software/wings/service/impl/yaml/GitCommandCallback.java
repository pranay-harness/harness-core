package software.wings.service.impl.yaml;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.yaml.GitCommand.GitCommandType.COMMIT_AND_PUSH;
import static software.wings.beans.yaml.GitCommand.GitCommandType.DIFF;
import static software.wings.beans.yaml.GitFileChange.Builder.aGitFileChange;
import static software.wings.service.impl.yaml.YamlProcessingLogContext.CHANGESET_ID;
import static software.wings.service.impl.yaml.sync.GitSyncErrorUtils.getCommitIdOfError;
import static software.wings.service.impl.yaml.sync.GitSyncErrorUtils.getCommitMessageOfError;
import static software.wings.service.impl.yaml.sync.GitSyncErrorUtils.getCommitTimeOfError;
import static software.wings.service.impl.yaml.sync.GitSyncErrorUtils.getYamlContentOfError;
import static software.wings.yaml.gitSync.YamlGitConfig.BRANCH_NAME_KEY;
import static software.wings.yaml.gitSync.YamlGitConfig.GIT_CONNECTOR_ID_KEY;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.mongodb.DuplicateKeyException;
import io.harness.delegate.beans.ResponseData;
import io.harness.eraro.ErrorCode;
import io.harness.exception.UnexpectedException;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import io.harness.waiter.NotifyCallback;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.beans.GitCommit;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.GitConnectionErrorAlert;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitCommand.GitCommandType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.beans.yaml.GitCommandResult;
import software.wings.beans.yaml.GitCommitAndPushResult;
import software.wings.beans.yaml.GitCommitRequest;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.yaml.gitdiff.GitChangeSetProcesser;
import software.wings.service.impl.yaml.sync.GitSyncFailureAlertDetails;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.sync.GitSyncErrorService;
import software.wings.service.intfc.yaml.sync.GitSyncService;
import software.wings.service.intfc.yaml.sync.YamlService;
import software.wings.utils.Utils;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.GitFileActivity;
import software.wings.yaml.gitSync.GitWebhookRequestAttributes;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlChangeSet.Status;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class GitCommandCallback implements NotifyCallback {
  private String accountId;
  private String changeSetId;
  private GitCommandType gitCommandType;
  private String gitConnectorId;
  private String branchName;

  public GitCommandCallback() {}

  public GitCommandCallback(
      String accountId, String changeSetId, GitCommandType gitCommandType, String gitConnectorId, String branchName) {
    this.accountId = accountId;
    this.changeSetId = changeSetId;
    this.gitCommandType = gitCommandType;
    this.gitConnectorId = gitConnectorId;
    this.branchName = branchName;
  }

  @Transient @Inject private transient YamlChangeSetService yamlChangeSetService;
  @Transient @Inject private transient YamlService yamlService;

  @Transient @Inject private transient YamlGitService yamlGitService;
  @Transient @Inject private FeatureFlagService featureFlagService;
  @Transient @Inject private AppService appService;
  @Transient @Inject private YamlDirectoryService yamlDirectoryService;
  @Transient @Inject private WingsPersistence wingsPersistence;
  @Transient @Inject private transient GitChangeSetProcesser gitChangeSetProcesser;
  @Transient @Inject GitSyncService gitSyncService;
  @Transient @Inject private GitSyncErrorService gitSyncErrorService;

  @Override
  public void notify(Map<String, ResponseData> response) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new GitCommandCallbackLogContext(getContext(), OVERRIDE_ERROR)) {
      logger.info("Git command response [{}]", response);

      ResponseData notifyResponseData = response.values().iterator().next();
      if (notifyResponseData instanceof GitCommandExecutionResponse) {
        GitCommandExecutionResponse gitCommandExecutionResponse = (GitCommandExecutionResponse) notifyResponseData;
        GitCommandResult gitCommandResult = gitCommandExecutionResponse.getGitCommandResult();

        if (gitCommandExecutionResponse.getGitCommandStatus() == GitCommandStatus.FAILURE) {
          if (changeSetId != null) {
            logger.warn("Git Command failed [{}]", gitCommandExecutionResponse.getErrorMessage());
            yamlChangeSetService.updateStatus(accountId, changeSetId, Status.FAILED);
          }
          if (gitCommandType == DIFF) {
            handleDiffCommandFailure(gitCommandExecutionResponse.getErrorCode(), accountId);
          }
          // raise alert if GitConnectionErrorAlert is not already open

          yamlGitService.raiseAlertForGitFailure(
              accountId, GLOBAL_APP_ID, getGitFailureDetailsFromGitResponse(gitCommandExecutionResponse));

          return;
        }

        // close alert if GitConnectionErrorAlert is open as now connection was successful
        yamlGitService.closeAlertForGitFailureIfOpen(
            accountId, GLOBAL_APP_ID, AlertType.GitConnectionError, createGitConnectionErrorData(accountId));

        logger.info("Git command [type: {}] request completed with status [{}]", gitCommandResult.getGitCommandType(),
            gitCommandExecutionResponse.getGitCommandStatus());

        if (gitCommandResult.getGitCommandType() == COMMIT_AND_PUSH) {
          GitCommitAndPushResult gitCommitAndPushResult = (GitCommitAndPushResult) gitCommandResult;
          YamlChangeSet yamlChangeSet = yamlChangeSetService.get(accountId, changeSetId);
          if (yamlChangeSet != null) {
            yamlChangeSetService.updateStatus(accountId, changeSetId, Status.COMPLETED);
            if (gitCommitAndPushResult.getGitCommitResult().getCommitId() != null) {
              List<String> yamlSetIdsProcessed =
                  ((GitCommitRequest) gitCommandExecutionResponse.getGitCommandRequest()).getYamlChangeSetIds();
              List<String> yamlGitConfigIds = obtainYamlGitConfigIds(accountId, branchName, gitConnectorId);

              saveCommitFromHarness(gitCommitAndPushResult, yamlChangeSet, yamlGitConfigIds, yamlSetIdsProcessed);
              final String processingCommitId = gitCommitAndPushResult.getGitCommitResult().getCommitId();
              final List<GitFileChange> filesCommited = emptyIfNull(gitCommitAndPushResult.getFilesCommitedToGit());
              addYamlChangeSetToFilesCommited(filesCommited, gitCommitAndPushResult.getYamlGitConfig());
              gitSyncService.logActivityForGitOperation(filesCommited, GitFileActivity.Status.SUCCESS, false,
                  yamlChangeSet.isFullSync(), "", processingCommitId);
              gitSyncService.createGitFileActivitySummaryForCommit(
                  processingCommitId, accountId, false, GitCommit.Status.COMPLETED);
            }
            yamlGitService.removeGitSyncErrors(accountId, yamlChangeSet.getGitFileChanges(), false);
          }
        } else if (gitCommandResult.getGitCommandType() == DIFF) {
          try {
            GitDiffResult gitDiffResult = (GitDiffResult) gitCommandResult;

            if (isNotEmpty(gitDiffResult.getGitFileChanges())) {
              addActiveGitSyncErrorsToProcessAgain(gitDiffResult, accountId);
            } else {
              logger.info("No file changes found in git diff. Skip adding active errors for processing");
            }

            gitChangeSetProcesser.processGitChangeSet(accountId, gitDiffResult);
            yamlChangeSetService.updateStatus(accountId, changeSetId, Status.COMPLETED);
          } catch (Exception e) {
            logger.error("error while processing diff request", e);
            yamlChangeSetService.updateStatus(accountId, changeSetId, Status.FAILED);
            handleDiffCommandFailure(null, accountId);
          }

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

  private void addYamlChangeSetToFilesCommited(List<GitFileChange> gitFileChanges, YamlGitConfig yamlGitConfig) {
    if (isEmpty(gitFileChanges)) {
      return;
    }
    gitFileChanges.forEach(gitFileChange -> gitFileChange.setYamlGitConfig(yamlGitConfig));
  }

  private GitConnectionErrorAlert createGitConnectionErrorData(String accountId) {
    return GitConnectionErrorAlert.builder()
        .accountId(accountId)
        .gitConnectorId(gitConnectorId)
        .branchName(branchName)
        .build();
  }

  private GitSyncFailureAlertDetails getGitFailureDetailsFromGitResponse(
      GitCommandExecutionResponse gitCommandExecutionResponse) {
    return GitSyncFailureAlertDetails.builder()
        .errorCode(gitCommandExecutionResponse.getErrorCode())
        .errorMessage(gitCommandExecutionResponse.getErrorMessage())
        .gitConnectorId(gitConnectorId)
        .branchName(branchName)
        .build();
  }

  private List<GitFileChange> getActiveGitSyncErrorFiles(String accountId, String branchName, String gitConnectorId) {
    final long _30_days_millis = System.currentTimeMillis() - Duration.ofDays(30).toMillis();
    return gitSyncErrorService.getActiveGitToHarnessSyncErrors(accountId, gitConnectorId, branchName, _30_days_millis)
        .stream()
        .map(this ::convertToGitFileChange)
        .collect(Collectors.toList());
  }

  private GitFileChange convertToGitFileChange(GitSyncError gitSyncError) {
    return aGitFileChange()
        .withFilePath(gitSyncError.getYamlFilePath())
        .withFileContent(getYamlContentOfError(gitSyncError))
        .withAccountId(gitSyncError.getAccountId())
        .withChangeType(Utils.getEnumFromString(ChangeType.class, gitSyncError.getChangeType()))
        .withSyncFromGit(true)
        .withCommitId(getCommitIdOfError(gitSyncError))
        .withChangeFromAnotherCommit(Boolean.TRUE)
        .withCommitTimeMs(getCommitTimeOfError(gitSyncError))
        .withCommitMessage(getCommitMessageOfError(gitSyncError))
        .build();
  }
  @VisibleForTesting
  void addActiveGitSyncErrorsToProcessAgain(final GitDiffResult gitDiffResult, final String accountId) {
    final List<GitFileChange> activeGitSyncErrorFiles = emptyIfNull(getActiveGitSyncErrorFiles(accountId,
        gitDiffResult.getYamlGitConfig().getBranchName(), gitDiffResult.getYamlGitConfig().getGitConnectorId()));

    logger.info("Active git sync error files =[{}]",
        activeGitSyncErrorFiles.stream().map(GitFileChange::getFilePath).collect(Collectors.toList()));

    if (isNotEmpty(activeGitSyncErrorFiles)) {
      final Set<String> filesAlreadyInDiffSet =
          gitDiffResult.getGitFileChanges().stream().map(GitFileChange::getFilePath).collect(Collectors.toSet());

      final List<GitFileChange> activeErrorsNotInDiff =
          activeGitSyncErrorFiles.stream()
              .filter(gitFileChange -> !filesAlreadyInDiffSet.contains(gitFileChange.getFilePath()))
              .collect(Collectors.toList());

      logger.info("Active git sync error files not in diff =[{}]",
          activeErrorsNotInDiff.stream().map(GitFileChange::getFilePath).collect(Collectors.toList()));

      activeErrorsNotInDiff.forEach(gitDiffResult::addChangeFile);
    }
  }

  private void saveCommitFromHarness(GitCommitAndPushResult gitCommitAndPushResult, YamlChangeSet yamlChangeSet,
      List<String> yamlGitConfigIds, List<String> yamlSetIdsProcessed) {
    String commitId = gitCommitAndPushResult.getGitCommitResult().getCommitId();
    YamlGitConfig yamlGitConfig = gitCommitAndPushResult.getYamlGitConfig();
    if (yamlGitConfig == null) {
      throw new UnexpectedException(String.format(
          "Error while saving commit for commitId=[%s],yamlChangeSetId=[%s] as the yamlGitConfig is null ", commitId,
          yamlChangeSet.getUuid()));
    }
    saveGitCommit(GitCommit.builder()
                      .accountId(accountId)
                      .yamlChangeSet(yamlChangeSet)
                      .yamlGitConfigIds(yamlGitConfigIds)
                      .status(GitCommit.Status.COMPLETED)
                      .commitId(commitId)
                      .gitCommandResult(gitCommitAndPushResult)
                      .gitConnectorId(yamlGitConfig.getGitConnectorId())
                      .branchName(yamlGitConfig.getBranchName())
                      .yamlChangeSetsProcessed(yamlSetIdsProcessed)
                      .commitMessage(gitCommitAndPushResult.getGitCommitResult().getCommitMessage())
                      .build());
  }

  private GitCommit saveGitCommit(GitCommit gitCommit) {
    GitCommit gitCommitSaved = null;
    try {
      gitCommitSaved = yamlGitService.saveCommit(gitCommit);
    } catch (Exception e) {
      if (e instanceof DuplicateKeyException) {
        logger.info("This was already persisted in DB. May Happens when 2 successive commits"
            + " are made to git in short duration, and when 2nd commit is done before gitDiff"
            + " for 1st one is in progress");
      } else {
        logger.warn("Failed to save gitCommit", e);
        // Try again without gitChangeSet and CommandResults.
        gitCommit.getYamlChangeSet().setGitFileChanges(null);
        gitCommit.setGitCommandResult(null);

        gitCommitSaved = yamlGitService.saveCommit(gitCommit);
      }
    }
    return gitCommitSaved;
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    logger.warn("Git request failed for command:[{}], changeSetId:[{}], account:[{}], response:[{}]", gitCommandType,
        changeSetId, accountId, response);
    updateChangeSetFailureStatusSafely();
    updateGitCommitFailureSafely();
  }

  protected void updateChangeSetFailureStatusSafely() {
    if (isNotEmpty(changeSetId) && (COMMIT_AND_PUSH == gitCommandType || DIFF == gitCommandType)) {
      yamlChangeSetService.updateStatus(accountId, changeSetId, Status.FAILED);
    }
  }

  protected void updateGitCommitFailureSafely() {
    if (DIFF == gitCommandType) {
      handleDiffCommandFailure(null, accountId);
    }
  }

  @VisibleForTesting
  List<String> obtainYamlGitConfigIds(String accountId, String branchName, String gitConnectorId) {
    return wingsPersistence.createQuery(YamlGitConfig.class)
        .filter(YamlGitConfig.ACCOUNT_ID_KEY, accountId)
        .filter(GIT_CONNECTOR_ID_KEY, gitConnectorId)
        .filter(BRANCH_NAME_KEY, branchName)
        .project(YamlGitConfig.ID_KEY, true)
        .asList()
        .stream()
        .map(Base::getUuid)
        .collect(Collectors.toList());
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
      final YamlChangeSet yamlChangeSet = yamlChangeSetService.get(accountId, changeSetId);
      if (yamlChangeSet == null) {
        logger.error("no changeset found with id =[{}]", changeSetId);
        return;
      }
      final GitWebhookRequestAttributes gitWebhookRequestAttributes = yamlChangeSet.getGitWebhookRequestAttributes();
      if (isValid(gitWebhookRequestAttributes)) {
        final String headCommitId = gitWebhookRequestAttributes.getHeadCommitId();
        final List<String> yamlConfigIds = obtainYamlGitConfigIds(accountId, branchName, gitConnectorId);

        GitCommit.Status gitCommitStatus = GitCommit.Status.FAILED;
        if (ErrorCode.GIT_DIFF_COMMIT_NOT_IN_ORDER == errorCode) {
          gitCommitStatus = GitCommit.Status.SKIPPED;
        }
        GitCommit gitCommit = saveFailedCommitFromGit(
            headCommitId, yamlConfigIds, accountId, gitCommitStatus, gitConnectorId, branchName);
        gitSyncService.createGitFileSummaryForFailedOrSkippedCommit(gitCommit, true);
      }
    }
  }

  private boolean isValid(GitWebhookRequestAttributes gitWebhookRequestAttributes) {
    return gitWebhookRequestAttributes != null && isNotEmpty(gitWebhookRequestAttributes.getHeadCommitId())
        && isNotEmpty(gitWebhookRequestAttributes.getBranchName())
        && isNotEmpty(gitWebhookRequestAttributes.getGitConnectorId());
  }
  private GitCommit saveFailedCommitFromGit(String commitId, List<String> yamlGitConfigIds, String accountId,
      GitCommit.Status gitCommitStatus, String gitConnectorId, String branchName) {
    return saveGitCommit(GitCommit.builder()
                             .accountId(accountId)
                             .yamlChangeSet(YamlChangeSet.builder()
                                                .accountId(accountId)
                                                .appId(GLOBAL_APP_ID)
                                                .gitToHarness(true)
                                                .status(Status.COMPLETED)
                                                .gitFileChanges(null)
                                                .build())
                             .yamlGitConfigIds(yamlGitConfigIds)
                             .status(gitCommitStatus)
                             .commitId(commitId)
                             .gitConnectorId(gitConnectorId)
                             .branchName(branchName)
                             .gitCommandResult(null)
                             .fileProcessingSummary(null)
                             .build());
  }
}
