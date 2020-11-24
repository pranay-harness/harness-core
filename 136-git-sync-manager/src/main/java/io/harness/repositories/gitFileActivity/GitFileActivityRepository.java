package io.harness.repositories.gitFileActivity;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface GitFileActivityRepository
    extends CrudRepository<GitFileActivity, String>, GitFileActivityRepositoryCustom {
  List<GitFileActivity> findByAccountIdAndCommitId(String accountId, String commitId);
}
