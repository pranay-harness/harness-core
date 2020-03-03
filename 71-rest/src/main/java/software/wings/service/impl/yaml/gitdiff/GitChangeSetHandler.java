package software.wings.service.impl.yaml.gitdiff;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.collections4.MapUtils.emptyIfNull;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.GitCommit.Status.COMPLETED;
import static software.wings.beans.GitCommit.Status.FAILED;
import static software.wings.yaml.gitSync.YamlGitConfig.BRANCH_NAME_KEY;
import static software.wings.yaml.gitSync.YamlGitConfig.GIT_CONNECTOR_ID_KEY;

import com.google.inject.Inject;

import com.mongodb.DuplicateKeyException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.GitCommit;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.WingsPersistence;
import software.wings.exception.YamlProcessingException;
import software.wings.exception.YamlProcessingException.ChangeWithErrorMsg;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.sync.YamlService;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlChangeSet.Status;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class GitChangeSetHandler {
  @Transient @Inject private YamlService yamlService;
  @Transient @Inject private YamlGitService yamlGitService;
  @Transient @Inject private WingsPersistence wingsPersistence;
  @Transient @Inject private YamlDirectoryService yamlDirectoryService;
  @Transient @Inject private AppService appService;

  public Map<String, ChangeWithErrorMsg> ingestGitYamlChangs(String accountId, GitDiffResult gitDiffResult) {
    List<GitFileChange> gitFileChangeList = obtainValidGitFileChangesBasedOnYamlGitConfig(
        gitDiffResult.getYamlGitConfig(), gitDiffResult.getGitFileChanges(), accountId);

    applySyncFromGit(gitFileChangeList);

    try {
      List<ChangeContext> fileChangeContexts = yamlService.processChangeSet(gitFileChangeList);
      logger.info("Successfully Processed ChangeSet [{}] for account {}", fileChangeContexts, accountId);

      saveProcessedCommit(gitDiffResult, accountId, COMPLETED);
      // this is for GitCommandType.DIFF, where we set gitToHarness = true explicitly as we are responding to
      // webhook invocation
      removeGitSyncErrorsForSuccessfulFiles(gitFileChangeList, emptySet(), accountId);

    } catch (YamlProcessingException ex) {
      logger.warn("Unable to process git commit {} for account {}. ", gitDiffResult.getCommitId(), accountId, ex);
      // this is for GitCommandType.DIFF, where we set gitToHarness = true explicitly as we are responding to
      // webhook invocation

      populatateYamlGitconfigInError(ex.getFailedYamlFileChangeMap(), gitDiffResult.getYamlGitConfig());

      yamlGitService.processFailedChanges(accountId, ex.getFailedYamlFileChangeMap(), true);

      removeGitSyncErrorsForSuccessfulFiles(gitFileChangeList, ex.getFailedYamlFileChangeMap().keySet(), accountId);
      // Add to gitCommits a failed commit.
      saveProcessedCommit(gitDiffResult, accountId, FAILED);

      return ex.getFailedYamlFileChangeMap();
    }
    return Collections.EMPTY_MAP;
  }

  private void populatateYamlGitconfigInError(
      Map<String, ChangeWithErrorMsg> failedYamlFileChangeMap, YamlGitConfig yamlGitConfig) {
    emptyIfNull(failedYamlFileChangeMap)
        .values()
        .stream()
        .map(changeWithErrorMsg -> (GitFileChange) changeWithErrorMsg.getChange())
        .filter(gitFileChange -> gitFileChange.getYamlGitConfig() == null)
        .forEach(gitFileChange -> gitFileChange.setYamlGitConfig(yamlGitConfig));
  }

  private void saveProcessedCommit(GitDiffResult gitDiffResult, String accountId, GitCommit.Status gitCommitStatus) {
    final List<String> yamlGitConfigIds = obtainYamlGitConfigIds(accountId,
        gitDiffResult.getYamlGitConfig().getBranchName(), gitDiffResult.getYamlGitConfig().getGitConnectorId());

    saveCommitFromGit(gitDiffResult, yamlGitConfigIds, accountId, gitCommitStatus);
  }

  private void removeGitSyncErrorsForSuccessfulFiles(
      List<GitFileChange> gitFileChangeList, Set<String> failedFilePathSet, String accountId) {
    final List<GitFileChange> successfullyProcessedFileList =
        emptyIfNull(getSuccessfullyProcessedFiles(gitFileChangeList, failedFilePathSet));
    logger.info("Successfully processed files =[{}]",
        successfullyProcessedFileList.stream().map(GitFileChange::getFilePath).collect(toList()));

    yamlGitService.removeGitSyncErrors(accountId, successfullyProcessedFileList, true);
  }

  private List<GitFileChange> getSuccessfullyProcessedFiles(
      List<GitFileChange> gitFileChangeList, Set<String> failedFilePathSet) {
    if (CollectionUtils.isEmpty(failedFilePathSet)) {
      return gitFileChangeList;
    }
    return emptyIfNull(gitFileChangeList)
        .stream()
        .filter(gitFileChange -> !failedFilePathSet.contains(gitFileChange.getFilePath()))
        .collect(toList());
  }

  private List<GitFileChange> obtainValidGitFileChangesBasedOnYamlGitConfig(
      YamlGitConfig yamlGitConfig, List<GitFileChange> gitFileChanges, String accountId) {
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

  // TODO: Change it as a part of ContextThreadLocal
  private void applySyncFromGit(List<GitFileChange> gitFileChangeList) {
    if (isEmpty(gitFileChangeList)) {
      return;
    }

    for (GitFileChange gitFileChange : gitFileChangeList) {
      gitFileChange.setSyncFromGit(true);
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
        .collect(toList());
  }

  private void saveCommitFromGit(
      GitDiffResult gitDiffResult, List<String> yamlGitConfigIds, String accountId, GitCommit.Status gitCommitStatus) {
    saveGitCommit(GitCommit.builder()
                      .accountId(accountId)
                      .yamlChangeSet(YamlChangeSet.builder()
                                         .accountId(accountId)
                                         .appId(GLOBAL_APP_ID)
                                         .gitToHarness(true)
                                         .status(Status.COMPLETED)
                                         .gitFileChanges(gitDiffResult.getGitFileChanges())
                                         .build())
                      .yamlGitConfigIds(yamlGitConfigIds)
                      .status(gitCommitStatus)
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
}
