package io.harness.gitsync.core.dao.api.repositories.YamlChangeSet;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlChangeSet.Status;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Optional;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface YamlChangeSetRepository
    extends PagingAndSortingRepository<YamlChangeSet, String>, YamlChangeSetRepositoryCustom {
  int countByAccountIdAndStatus(String accountId, Status status);

  int countByAccountIdAndStatusAndQueueKey(String accountId, Status status, String queueKey);

  Optional<YamlChangeSet> findFirstByAccountIdAndQueueKeyAndStatusOrderByCreatedAt(
      String accountId, String queueKey, Status status);

  Optional<YamlChangeSet> findFirstAccountIdAndQueueKeyAndStatusAndGitToHarnessOrderByCreatedAt(
      String accountId, String queueKey, Status status, boolean gitToHarness);

  List<YamlChangeSet> findByAccountIdAndStatusAndLastUpdatedAtLessThan(
      List<String> accountIds, Status status, Long lastUpdatedCutOff);
}
