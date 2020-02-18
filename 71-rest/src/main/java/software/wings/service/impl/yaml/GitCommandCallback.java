package software.wings.service.impl.yaml;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.yaml.GitCommand.GitCommandType.COMMIT_AND_PUSH;
import static software.wings.beans.yaml.GitCommand.GitCommandType.DIFF;
import static software.wings.yaml.gitSync.YamlGitConfig.BRANCH_NAME_KEY;
import static software.wings.yaml.gitSync.YamlGitConfig.GIT_CONNECTOR_ID_KEY;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.mongodb.DuplicateKeyException;
import io.harness.delegate.beans.ResponseData;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import io.harness.waiter.NotifyCallback;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.beans.GitCommit;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.GitConnectionErrorAlert;
import software.wings.beans.yaml.GitCommand.GitCommandType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.beans.yaml.GitCommandResult;
import software.wings.beans.yaml.GitCommitAndPushResult;
import software.wings.beans.yaml.GitCommitRequest;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.yaml.gitdiff.GitChangeSetProcesser;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.sync.YamlService;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlChangeSet.Status;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class GitCommandCallback implements NotifyCallback {
  private String accountId;
  private String changeSetId;
  private GitCommandType gitCommandType;

  public GitCommandCallback() {}

  public GitCommandCallback(String accountId, String changeSetId, GitCommandType gitCommandType) {
    this.accountId = accountId;
    this.changeSetId = changeSetId;
    this.gitCommandType = gitCommandType;
  }

  @Transient @Inject private transient YamlChangeSetService yamlChangeSetService;
  @Transient @Inject private transient YamlService yamlService;

  @Transient @Inject private transient YamlGitService yamlGitService;
  @Transient @Inject private FeatureFlagService featureFlagService;
  @Transient @Inject private AppService appService;
  @Transient @Inject private YamlDirectoryService yamlDirectoryService;
  @Transient @Inject private WingsPersistence wingsPersistence;
  @Transient @Inject private transient GitChangeSetProcesser gitChangeSetProcesser;

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
          // raise alert if GitConnectionErrorAlert is not already open (changeSetId will be null for webhook request,
          // so putting outside if)
          yamlGitService.raiseAlertForGitFailure(accountId, GLOBAL_APP_ID, gitCommandExecutionResponse.getErrorCode(),
              gitCommandExecutionResponse.getErrorMessage());

          return;
        }

        // close alert if GitConnectionErrorAlert is open as now connection was successful
        yamlGitService.closeAlertForGitFailureIfOpen(accountId, GLOBAL_APP_ID, AlertType.GitConnectionError,
            GitConnectionErrorAlert.builder().accountId(accountId).build());

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

              List<String> yamlGitConfigIds =
                  obtainYamlGitConfigIds(accountId, gitCommitAndPushResult.getYamlGitConfig().getBranchName(),
                      gitCommitAndPushResult.getYamlGitConfig().getGitConnectorId());

              saveCommitFromHarness(gitCommitAndPushResult, yamlChangeSet, yamlGitConfigIds, yamlSetIdsProcessed);
            }
            yamlGitService.removeGitSyncErrors(accountId, yamlChangeSet.getGitFileChanges(), false);
          }
        } else if (gitCommandResult.getGitCommandType() == DIFF) {
          try {
            GitDiffResult gitDiffResult = (GitDiffResult) gitCommandResult;
            gitChangeSetProcesser.processGitChangeSet(accountId, gitDiffResult);
            yamlChangeSetService.updateStatus(accountId, changeSetId, Status.COMPLETED);
          } catch (Exception e) {
            yamlChangeSetService.updateStatus(accountId, changeSetId, Status.FAILED);
            throw e;
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

  private void saveCommitFromHarness(GitCommitAndPushResult gitCommitAndPushResult, YamlChangeSet yamlChangeSet,
      List<String> yamlGitConfigIds, List<String> yamlSetIdsProcessed) {
    saveGitCommit(GitCommit.builder()
                      .accountId(accountId)
                      .yamlChangeSet(yamlChangeSet)
                      .yamlGitConfigIds(yamlGitConfigIds)
                      .status(GitCommit.Status.COMPLETED)
                      .commitId(gitCommitAndPushResult.getGitCommitResult().getCommitId())
                      .gitCommandResult(gitCommitAndPushResult)
                      .yamlChangeSetsProcessed(yamlSetIdsProcessed)
                      .build());
  }

  private void saveGitCommit(GitCommit gitCommit) {
    try {
      yamlGitService.saveCommit(gitCommit);
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

        yamlGitService.saveCommit(gitCommit);
      }
    }
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    logger.warn("Git request failed for command:[{}], changeSetId:[{}], account:[{}], response:[{}]", gitCommandType,
        changeSetId, accountId, response);
    updateChangeSetFailureStatusSafely();
  }

  protected void updateChangeSetFailureStatusSafely() {
    if (isNotEmpty(changeSetId) && (COMMIT_AND_PUSH == gitCommandType || DIFF == gitCommandType)) {
      yamlChangeSetService.updateStatus(accountId, changeSetId, Status.FAILED);
    }
  }

  private List<String> obtainYamlGitConfigIds(String accountId, String branchName, String gitConnectorId) {
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
      context.put("gitCommandCallChangeSetId", changeSetId);
    }

    return context.build();
  }
}
