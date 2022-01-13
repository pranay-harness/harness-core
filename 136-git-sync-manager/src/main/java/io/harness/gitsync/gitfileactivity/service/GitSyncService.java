/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.gitfileactivity.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.core.beans.ChangeWithErrorMsg;
import io.harness.gitsync.core.beans.GitCommit;
import io.harness.gitsync.core.beans.GitCommit.GitCommitProcessingStatus;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity.Status;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivitySummary;

import java.util.List;

@OwnedBy(DX)
public interface GitSyncService {
  Iterable<GitFileActivity> logActivityForGitOperation(List<GitFileChange> changeList, Status status,
      boolean isGitToHarness, boolean isFullSync, String message, String commitId, String commitMessage,
      YamlGitConfigDTO yamlGitConfig);

  void logActivityForSkippedFiles(List<GitFileChange> changeList, List<GitFileChange> completeChangeList,
      String message, String accountId, String commitId);

  GitFileActivitySummary createGitFileActivitySummaryForCommit(String commitId, String accountId, Boolean gitToHarness,
      GitCommitProcessingStatus status, YamlGitConfigDTO yamlGitConfig);

  void logActivityForSuccessfulFiles(
      List<GitFileChange> gitFileChanges, String accountId, String commitMessage, YamlGitConfigDTO yamlGitConfig);

  void onGitFileProcessingSuccess(
      GitFileChange change, String accountId, String commitMessage, YamlGitConfigDTO yamlGitConfig);

  void updateStatusOfGitFileActivity(
      String commitId, List<String> fileNames, Status status, String errorMessage, String accountId);

  List<GitFileActivity> saveAll(List<GitFileActivity> gitFileActivities);

  void updateGitFileActivity(List<ChangeWithErrorMsg> failedYamlFileChangeMap,
      List<GitFileChange> successfulGitFileChanges, List<GitFileChange> skippedGitFileChanges, String accountId,
      boolean isFullSync, String commitMessage, YamlGitConfigDTO yamlGitConfig);

  void logActivitiesForFailedChanges(List<ChangeWithErrorMsg> failedYamlFileChangeMap, String accountId,
      boolean isFullSync, String commitMessage, YamlGitConfigDTO yamlGitConfig);

  void createGitFileSummaryForFailedOrSkippedCommit(GitCommit gitCommit, boolean gitToHarness);
}
