package io.harness.gitsync.gitfileactivity.dao.api.repositories.gitFileActivity;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface GitFileActivityRepository
    extends CrudRepository<GitFileActivity, String>, GitFileActivityRepositoryCustom {
  List<GitFileActivity> findByAccountIdAndCommitId(String accountId, String commitId);
}
