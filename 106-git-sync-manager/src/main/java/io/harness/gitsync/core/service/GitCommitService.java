package io.harness.gitsync.core.service;

import io.harness.gitsync.core.beans.GitCommit;

import java.util.List;
import java.util.Optional;

public interface GitCommitService {
  GitCommit save(GitCommit gitCommit);

  Optional<GitCommit> findByAccountIdAndCommitIdAndRepoAndBranchNameAndStatus(
      String accountId, String commitId, String repo, String branchName, List<GitCommit.Status> status);

  Optional<GitCommit> findGitCommitWithProcessedStatus(
      String accountId, String commitId, String repo, String branchName);

  Optional<GitCommit> findLastProcessedGitCommit(String accountId, String repo, String branchName);

  Optional<GitCommit> findByAccountIdAndCommitIdAndRepoAndBranchName(
      String accountId, String repo, String branchName, List<GitCommit.Status> status);
}
