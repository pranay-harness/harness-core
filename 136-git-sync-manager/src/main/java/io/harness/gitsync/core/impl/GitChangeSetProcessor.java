/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.core.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.git.Constants.GIT_YAML_LOG_PREFIX;
import static io.harness.gitsync.gitfileactivity.beans.GitFileActivity.Status.SKIPPED;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.manage.GlobalContextManager.ensureGlobalContextGuard;

import static java.lang.Boolean.FALSE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.DiffResult;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.common.YamlProcessingLogContext;
import io.harness.gitsync.common.dtos.YamlGitConfigGitFileChangeMap;
import io.harness.gitsync.common.helper.GitFileLocationHelper;
import io.harness.gitsync.common.helper.YamlGitConfigHelper;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.service.GitCommitService;
import io.harness.gitsync.core.service.YamlService;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity;
import io.harness.gitsync.gitfileactivity.service.GitSyncService;
import io.harness.logging.AccountLogContext;
import io.harness.manage.GlobalContextManager.GlobalContextGuard;
import io.harness.mongo.ProcessTimeLogContext;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class GitChangeSetProcessor {
  private final GitSyncService gitSyncService;
  private final GitCommitService gitCommitService;
  private final YamlGitConfigService yamlGitConfigService;
  private final YamlService yamlService;

  public void processGitChangeSet(
      String accountId, DiffResult gitDiffResult, String gitConnectorId, String repo, String branch) {
    final Stopwatch stopwatch = Stopwatch.createStarted();
    try (AccountLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         YamlProcessingLogContext ignore2 = YamlProcessingLogContext.builder()
                                                .branchName(gitDiffResult.getBranch())
                                                .repoName(gitDiffResult.getRepoName())
                                                .commitId(gitDiffResult.getCommitId())
                                                .build(OVERRIDE_ERROR)) {
      log.info(GIT_YAML_LOG_PREFIX + "Started processing git diff results with files [{}]",
          emptyIfNull(gitDiffResult.getGitFileChanges()).stream().map(GitFileChange::getFilePath).collect(toList()));

      // ensure gitCommit is not already processed. Else nothing to be done.
      boolean commitAlreadyProcessed =
          gitCommitService.isCommitAlreadyProcessed(accountId, gitDiffResult.getCommitId(), repo, branch);
      if (commitAlreadyProcessed) {
        //        // do nothing
        log.warn(GIT_YAML_LOG_PREFIX + "Commit [{}] already processed for account {}", gitDiffResult.getCommitId(),
            accountId);
        return;
      }

      ingestYamlChanges(accountId, gitDiffResult, gitConnectorId, repo, branch);

      try (ProcessTimeLogContext ignore3 = new ProcessTimeLogContext(stopwatch.elapsed(MILLISECONDS), OVERRIDE_ERROR)) {
        log.info(GIT_YAML_LOG_PREFIX + "Successfully  processed git diff results");
      }
    }
  }

  private void ingestYamlChanges(
      String accountId, DiffResult gitDiffResult, String gitConnectorId, String repo, String branch) {
    try (GlobalContextGuard guard = ensureGlobalContextGuard()) {
      final String processingCommitId = gitDiffResult.getCommitId();

      final List<YamlGitConfigDTO> yamlGitConfigDTOs =
          yamlGitConfigService.getByConnectorRepoAndBranch(gitConnectorId, repo, branch, accountId);
      final List<YamlGitConfigGitFileChangeMap> yamlGitConfigGitFileChangeMaps =
          YamlGitConfigHelper.batchGitFileChangeByRootFolder(gitDiffResult.getGitFileChanges(), yamlGitConfigDTOs);
      preProcessGitFileActivityChanges(processingCommitId, gitDiffResult.getCommitTimeMs(),
          gitDiffResult.getCommitMessage(), yamlGitConfigGitFileChangeMaps);

      final List<GitFileChange> gitDiffResultChangeFiltered = new ArrayList<>();
      // Mark invalid files as SKIPPED
      yamlGitConfigGitFileChangeMaps.forEach(yamlGitConfigGitFileChangeMap -> {
        final List<GitFileChange> gitDiffResultChangeSet = yamlGitConfigGitFileChangeMap.getGitFileChanges();

        final List<GitFileChange> validFilesBasedOnYamlGitConfigFilter =
            obtainValidGitFileChangesBasedOnKnownEntityTypes(gitDiffResultChangeSet, accountId);

        gitSyncService.logActivityForSkippedFiles(validFilesBasedOnYamlGitConfigFilter, gitDiffResultChangeSet,
            "Root Folder not configured.", accountId, gitDiffResultChangeSet.get(0).getCommitId());
        applySyncFromGit(validFilesBasedOnYamlGitConfigFilter);
        gitDiffResultChangeFiltered.addAll(validFilesBasedOnYamlGitConfigFilter);
      });
      yamlService.processChangeSet(gitDiffResultChangeFiltered);
    }
  }

  private void preProcessGitFileActivityChanges(String processingCommitId, Long processingCommitTimeMs,
      String commitMessage, List<YamlGitConfigGitFileChangeMap> yamlGitConfigGitFileChangeMaps) {
    yamlGitConfigGitFileChangeMaps.forEach(yamlGitConfigGitFileChangeMap -> {
      addProcessingCommitDetailsToChangeList(
          processingCommitId, processingCommitTimeMs, commitMessage, yamlGitConfigGitFileChangeMap.getGitFileChanges());
      // All initial activities will be created with status QUEUED
      gitSyncService.logActivityForGitOperation(
          getFileChangesOfCommit(yamlGitConfigGitFileChangeMap.getGitFileChanges()), GitFileActivity.Status.QUEUED,
          true, false, "", "", "", yamlGitConfigGitFileChangeMap.getYamlGitConfigDTO());
    });
  }

  private List<GitFileChange> getFileChangesOfCommit(List<GitFileChange> gitFileChanges) {
    if (isEmpty(gitFileChanges)) {
      return gitFileChanges;
    }
    return gitFileChanges.stream().filter(change -> !change.isChangeFromAnotherCommit()).collect(toList());
  }

  private void addProcessingCommitDetailsToChangeList(String processingCommitId, Long processingCommitTimeMs,
      String processingCommitMessage, List<GitFileChange> gitFileChanges) {
    if (isEmpty(gitFileChanges)) {
      return;
    }
    gitFileChanges.forEach(gitFileChange -> {
      gitFileChange.setProcessingCommitId(processingCommitId);
      gitFileChange.setProcessingCommitTimeMs(processingCommitTimeMs);
      gitFileChange.setProcessingCommitMessage(processingCommitMessage);
      gitFileChange.setChangeFromAnotherCommit(!gitFileChange.getCommitId().equalsIgnoreCase(processingCommitId));
    });
  }

  private void logFileActivityForSkippedFiles(
      List<GitFileChange> allGitFileChanges, Map<String, String> excludedFilePathWithReasonMap, String accountId) {
    final Map<String, GitFileChange> allFilesPathToObjectMapping =
        allGitFileChanges.stream().collect(Collectors.toMap(GitFileChange::getFilePath, identity()));

    excludedFilePathWithReasonMap.forEach((filePath, skipMessage) -> {
      final GitFileChange gitFileChange = allFilesPathToObjectMapping.get(filePath);
      if (filePartOfSameCommit(gitFileChange)) {
        gitSyncService.updateStatusOfGitFileActivity(gitFileChange.getProcessingCommitId(),
            Collections.singletonList(gitFileChange.getFilePath()), SKIPPED, skipMessage, accountId);
      }
    }

    );
  }

  private boolean filePartOfSameCommit(GitFileChange gitFileChange) {
    return !Optional.of(gitFileChange.isChangeFromAnotherCommit()).orElse(FALSE);
  }

  @VisibleForTesting
  List<GitFileChange> obtainValidGitFileChangesBasedOnKnownEntityTypes(
      List<GitFileChange> gitFileChanges, String accountId) {
    return gitFileChanges.stream()
        .map(gitFileChange -> {
          try {
            GitFileLocationHelper.getEntityType(gitFileChange.getFilePath());
            return gitFileChange;
          } catch (Exception e) {
            log.info("Unknown entity type for file path {} ", gitFileChange.getFilePath(), e);
          }
          return null;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @VisibleForTesting
  void applySyncFromGit(List<GitFileChange> gitFileChangeList) {
    if (isEmpty(gitFileChangeList)) {
      return;
    }

    for (GitFileChange gitFileChange : gitFileChangeList) {
      gitFileChange.setSyncFromGit(true);
    }
  }
}
