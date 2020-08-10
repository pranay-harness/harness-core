package io.harness.gitsync.gitfileactivity.service;

import io.harness.gitsync.common.beans.GitFileChange;
import io.harness.gitsync.core.beans.GitCommit;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivitySummary;

import java.util.List;

public interface GitSyncService {
  Iterable<GitFileActivity> logActivityForGitOperation(List<GitFileChange> changeList, GitFileActivity.Status status,
      boolean isGitToHarness, boolean isFullSync, String message, String commitId, String commitMessage);

  GitFileActivitySummary createGitFileActivitySummaryForCommit(
      String commitId, String accountId, Boolean gitToHarness, GitCommit.Status status);
}
