package software.wings.service.impl.yaml;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.ID_KEY;
import static software.wings.beans.yaml.GitCommand.GitCommandType.COMMIT_AND_PUSH;
import static software.wings.utils.Validator.notNullCheck;
import static software.wings.yaml.gitSync.YamlGitConfig.BRANCH_NAME_KEY;
import static software.wings.yaml.gitSync.YamlGitConfig.GIT_CONNECTOR_ID_KEY;

import com.google.inject.Inject;

import com.mongodb.DuplicateKeyException;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.waiter.NotifyCallback;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.GitCommit;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.GitConnectionErrorAlert;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitCommand.GitCommandType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.beans.yaml.GitCommandResult;
import software.wings.beans.yaml.GitCommitAndPushResult;
import software.wings.beans.yaml.GitCommitRequest;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.WingsPersistence;
import software.wings.exception.YamlProcessingException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.sync.YamlService;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlChangeSet.Status;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 10/27/17.
 */
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

  @Transient private static final transient Logger logger = LoggerFactory.getLogger(GitCommandCallback.class);

  @Transient @Inject private transient YamlChangeSetService yamlChangeSetService;
  @Transient @Inject private transient YamlService yamlService;

  @Transient @Inject private transient YamlGitService yamlGitService;
  @Transient @Inject private FeatureFlagService featureFlagService;
  @Transient @Inject private AppService appService;
  @Transient @Inject private YamlDirectoryService yamlDirectoryService;
  @Transient @Inject private WingsPersistence wingsPersistence;

  @Override
  public void notify(Map<String, ResponseData> response) {
    logger.info("Git command response [{}] for changeSetId [{}] for account {}", response, changeSetId, accountId);
    ResponseData notifyResponseData = response.values().iterator().next();
    if (notifyResponseData instanceof GitCommandExecutionResponse) {
      GitCommandExecutionResponse gitCommandExecutionResponse = (GitCommandExecutionResponse) notifyResponseData;
      GitCommandResult gitCommandResult = gitCommandExecutionResponse.getGitCommandResult();

      if (gitCommandExecutionResponse.getGitCommandStatus().equals(GitCommandStatus.FAILURE)) {
        if (changeSetId != null) {
          logger.warn("Git Command failed [{}] for changeSetId [{}] for account {}",
              gitCommandExecutionResponse.getErrorMessage(), changeSetId, accountId);
          yamlChangeSetService.updateStatus(accountId, changeSetId, Status.FAILED);
        }
        // raise alert if GitConnectionErrorAlert is not already open (changeSetId will be null for webhook request, so
        // putting outside if)
        yamlGitService.raiseAlertForGitFailure(accountId, GLOBAL_APP_ID, gitCommandExecutionResponse.getErrorCode(),
            gitCommandExecutionResponse.getErrorMessage());

        return;
      }

      // close alert if GitConnectionErrorAlert is open as now connection was successful
      yamlGitService.closeAlertForGitFailureIfOpen(accountId, GLOBAL_APP_ID, AlertType.GitConnectionError,
          GitConnectionErrorAlert.builder().accountId(accountId).build());

      logger.info("Git command [type: {}] request completed with status [{}] for account {}",
          gitCommandResult.getGitCommandType(), gitCommandExecutionResponse.getGitCommandStatus(), accountId);

      if (gitCommandResult.getGitCommandType().equals(COMMIT_AND_PUSH)) {
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
      } else if (gitCommandResult.getGitCommandType().equals(GitCommandType.DIFF)) {
        GitDiffResult gitDiffResult = (GitDiffResult) gitCommandResult;

        List<GitFileChange> gitFileChangeList = obtainValidGitFileChangesBasedOnYamlGitConfig(
            gitDiffResult.getYamlGitConfig(), gitDiffResult.getGitFileChanges());

        applySyncFromGit(gitFileChangeList);

        try {
          // ensure gitCommit is not already processed
          boolean commitAlreadyProcessed =
              yamlGitService.isCommitAlreadyProcessed(accountId, gitDiffResult.getCommitId());
          if (commitAlreadyProcessed) {
            // do nothing
            logger.warn("Commit [{}] already processed for account {}", gitDiffResult.getCommitId(), accountId);
            return;
          }

          List<ChangeContext> fileChangeContexts = yamlService.processChangeSet(gitFileChangeList);
          logger.info("Processed ChangeSet [{}] for account {}", fileChangeContexts, accountId);

          List<String> yamlGitConfigIds = obtainYamlGitConfigIds(accountId,
              gitDiffResult.getYamlGitConfig().getBranchName(), gitDiffResult.getYamlGitConfig().getGitConnectorId());

          saveCommitFromGit(gitDiffResult, yamlGitConfigIds);
          // this is for GitCommandType.DIFF, where we set gitToHarness = true explicitly as we are responding to
          // webhook invocation
          yamlGitService.removeGitSyncErrors(accountId, gitFileChangeList, true);
        } catch (YamlProcessingException ex) {
          logger.warn("Unable to process git commit {} for account {}. ", gitDiffResult.getCommitId(), accountId, ex);
          // this is for GitCommandType.DIFF, where we set gitToHarness = true explicitly as we are responding to
          // webhook invocation
          yamlGitService.processFailedChanges(accountId, ex.getFailedYamlFileChangeMap(), true);
        }
      } else {
        logger.warn("Unexpected commandType result: [{}] for changeSetId [{}] for account {}",
            gitCommandExecutionResponse.getErrorMessage(), changeSetId, accountId);
        yamlChangeSetService.updateStatus(accountId, changeSetId, Status.FAILED);
      }
    } else {
      logger.warn("Unexpected notify response data: [{}] for changeSetId [{}] for account {}", notifyResponseData,
          changeSetId, accountId);
      updateChangeSetFailureStatusSafely();
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

  private void saveCommitFromGit(GitDiffResult gitDiffResult, List<String> yamlGitConfigIds) {
    saveGitCommit(GitCommit.builder()
                      .accountId(accountId)
                      .yamlChangeSet(YamlChangeSet.builder()
                                         .accountId(accountId)
                                         .appId(Base.GLOBAL_APP_ID)
                                         .gitToHarness(true)
                                         .status(Status.COMPLETED)
                                         .gitFileChanges(gitDiffResult.getGitFileChanges())
                                         .build())
                      .yamlGitConfigIds(yamlGitConfigIds)
                      .status(GitCommit.Status.COMPLETED)
                      .commitId(gitDiffResult.getCommitId())
                      .gitCommandResult(gitDiffResult)
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
    if (isNotEmpty(changeSetId) && COMMIT_AND_PUSH.equals(gitCommandType)) {
      yamlChangeSetService.updateStatus(accountId, changeSetId, Status.FAILED);
    }
  }

  private void applySyncFromGit(List<GitFileChange> gitFileChangeList) {
    if (isEmpty(gitFileChangeList)) {
      return;
    }

    for (GitFileChange gitFileChange : gitFileChangeList) {
      gitFileChange.setSyncFromGit(true);
    }
  }

  private List<GitFileChange> obtainValidGitFileChangesBasedOnYamlGitConfig(
      YamlGitConfig yamlGitConfig, List<GitFileChange> gitFileChanges) {
    List<GitFileChange> gitFileChangeList = new ArrayList<>();
    Map<String, YamlGitConfig> appMap = new HashMap<>();

    if (isEmpty(gitFileChanges)) {
      return gitFileChangeList;
    }

    for (GitFileChange gitFileChange : gitFileChanges) {
      YamlGitConfig currentEntityYamlGitConfig;

      if (yamlGitService.checkApplicationChange(gitFileChange)) {
        // Handles application

        String appName = yamlGitService.obtainAppNameFromGitFileChange(gitFileChange);
        notNullCheck("Application name cannot be null", USER);

        if (!appMap.containsKey(appName)) {
          Application app = appService.getAppByName(gitFileChange.getAccountId(), appName);

          if (app != null) {
            YamlGitConfig appYamlGitConfig = yamlDirectoryService.weNeedToPushChanges(accountId, app.getUuid());
            appMap.put(appName, appYamlGitConfig);
          } else {
            // New app is created on git side. We need to consume those changes

            gitFileChange.setYamlGitConfig(yamlGitConfig);
            gitFileChangeList.add(gitFileChange);
            continue;
          }
        }

        currentEntityYamlGitConfig = appMap.get(appName);
      } else {
        // Handle account level entities
        // This check is there to make sure that the yamlGitConfig is not disabled. If its disabled then we don't need
        // to process this change.
        currentEntityYamlGitConfig = yamlDirectoryService.weNeedToPushChanges(accountId, GLOBAL_APP_ID);
      }

      if (currentEntityYamlGitConfig != null
          && yamlGitConfig.getGitConnectorId().equals(currentEntityYamlGitConfig.getGitConnectorId())
          && yamlGitConfig.getBranchName().equals(currentEntityYamlGitConfig.getBranchName())) {
        gitFileChangeList.add(gitFileChange);
      }
    }

    return gitFileChangeList;
  }

  private List<String> obtainYamlGitConfigIds(String accountId, String branchName, String gitConnectorId) {
    return wingsPersistence.createQuery(YamlGitConfig.class)
        .filter(ACCOUNT_ID_KEY, accountId)
        .filter(GIT_CONNECTOR_ID_KEY, gitConnectorId)
        .filter(BRANCH_NAME_KEY, branchName)
        .project(ID_KEY, true)
        .asList()
        .stream()
        .map(yamlGitConfig -> yamlGitConfig.getUuid())
        .collect(Collectors.toList());
  }
}
