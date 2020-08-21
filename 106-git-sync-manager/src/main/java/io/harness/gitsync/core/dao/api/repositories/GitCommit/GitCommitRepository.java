package io.harness.gitsync.core.dao.api.repositories.GitCommit;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitCommit;
import io.harness.gitsync.core.beans.GitCommit.Status;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface GitCommitRepository extends CrudRepository<GitCommit, String> {
  Optional<GitCommit> findFirstByAccountIdAndRepoAndBranchNameAndStatusInOrderByCreatedAtDesc(
      String accountId, String repo, String branchName, List<Status> status);

  Optional<GitCommit> findByAccountIdAndCommitIdAndRepoAndBranchNameAndStatusInOrderByCreatedAtDesc(
      String accountId, String commitId, String repo, String branchName, List<Status> status);
}
