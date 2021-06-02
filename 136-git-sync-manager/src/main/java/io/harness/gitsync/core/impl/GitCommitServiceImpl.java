package io.harness.gitsync.core.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.core.beans.GitCommit.GIT_COMMIT_PROCESSED_STATUS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitCommit;
import io.harness.gitsync.core.beans.GitCommit.GitCommitProcessingStatus;
import io.harness.gitsync.core.service.GitCommitService;
import io.harness.repositories.gitCommit.GitCommitRepository;

import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class GitCommitServiceImpl implements GitCommitService {
  private GitCommitRepository gitCommitRepository;

  @Override
  public GitCommit save(GitCommit gitCommit) {
    return gitCommitRepository.save(gitCommit);
  }

  @Override
  public Optional<GitCommit> findByAccountIdAndCommitIdAndRepoAndBranchNameAndStatus(
      String accountId, String commitId, String repo, String branchName, List<GitCommitProcessingStatus> status) {
    return gitCommitRepository.findByAccountIdentifierAndCommitIdAndRepoURLAndBranchNameAndStatusIn(
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
      String accountId, String repo, String branchName, List<GitCommitProcessingStatus> status) {
    return gitCommitRepository.findFirstByAccountIdentifierAndRepoURLAndBranchNameAndStatusInOrderByCreatedAtDesc(
        accountId, repo, branchName, status);
  }

  @Override
  public boolean isCommitAlreadyProcessed(String accountId, String headCommit, String repo, String branch) {
    final Optional<GitCommit> gitCommit = findGitCommitWithProcessedStatus(accountId, headCommit, repo, branch);
    if (gitCommit.isPresent()) {
      log.info("Commit [id:{}] already processed [status:{}] on [date:{}]", gitCommit.get().getCommitId(),
          gitCommit.get().getStatus(), gitCommit.get().getLastUpdatedAt());
      return true;
    }
    return false;
  }

  @Override
  public Optional<GitCommit> findLastGitCommit(String accountIdentifier, String repo, String branchName) {
    return gitCommitRepository.findFirstByAccountIdentifierAndRepoURLAndBranchNameOrderByCreatedAtDesc(
        accountIdentifier, repo, branchName);
  }
}
