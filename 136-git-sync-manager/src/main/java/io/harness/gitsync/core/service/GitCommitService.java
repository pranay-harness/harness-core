package io.harness.gitsync.core.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitCommit;

import java.util.List;
import java.util.Optional;

@OwnedBy(DX)
public interface GitCommitService {
  GitCommit save(GitCommit gitCommit);

  GitCommit upsertWithYamlGitConfigIdAddition(GitCommit gitCommit);

  Optional<GitCommit> findByAccountIdAndCommitIdAndRepoAndBranchNameAndStatus(
      String accountId, String commitId, String repo, String branchName, List<GitCommit.Status> status);

  Optional<GitCommit> findGitCommitWithProcessedStatus(
      String accountId, String commitId, String repo, String branchName);

  Optional<GitCommit> findLastProcessedGitCommit(String accountId, String repo, String branchName);

  Optional<GitCommit> findByAccountIdAndCommitIdAndRepoAndBranchName(
      String accountId, String repo, String branchName, List<GitCommit.Status> status);

  boolean isCommitAlreadyProcessed(String accountId, String headCommit, String repo, String branch);
}
