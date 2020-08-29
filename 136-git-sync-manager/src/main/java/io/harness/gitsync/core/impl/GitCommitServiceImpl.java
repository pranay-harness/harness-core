package io.harness.gitsync.core.impl;

import static io.harness.gitsync.core.beans.GitCommit.GIT_COMMIT_PROCESSED_STATUS;

import com.google.inject.Inject;

import io.harness.gitsync.core.beans.GitCommit;
import io.harness.gitsync.core.dao.api.repositories.GitCommit.GitCommitRepository;
import io.harness.gitsync.core.service.GitCommitService;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class GitCommitServiceImpl implements GitCommitService {
  private GitCommitRepository gitCommitRepository;

  @Override
  public GitCommit save(GitCommit gitCommit) {
    return gitCommitRepository.save(gitCommit);
  }

  @Override
  public Optional<GitCommit> findByAccountIdAndCommitIdAndRepoAndBranchNameAndStatus(
      String accountId, String commitId, String repo, String branchName, List<GitCommit.Status> status) {
    return gitCommitRepository.findByAccountIdAndCommitIdAndRepoAndBranchNameAndStatusInOrderByCreatedAtDesc(
        accountId, commitId, repo, branchName, status);
  }

  @Override
  public Optional<GitCommit> findGitCommitWithProcessedStatus(
      String accountId, String commitId, String repo, String branchName) {
    return findByAccountIdAndCommitIdAndRepoAndBranchNameAndStatus(
        accountId, commitId, repo, branchName, GIT_COMMIT_PROCESSED_STATUS);
  }

  @Override
  public Optional<GitCommit> findLastProcessedGitCommit(String accountId, String repo, String branchName) {
    return findByAccountIdAndCommitIdAndRepoAndBranchName(accountId, repo, branchName, GIT_COMMIT_PROCESSED_STATUS);
  }

  @Override
  public Optional<GitCommit> findByAccountIdAndCommitIdAndRepoAndBranchName(
      String accountId, String repo, String branchName, List<GitCommit.Status> status) {
    return gitCommitRepository.findFirstByAccountIdAndRepoAndBranchNameAndStatusInOrderByCreatedAtDesc(
        accountId, repo, branchName, status);
  }
}
