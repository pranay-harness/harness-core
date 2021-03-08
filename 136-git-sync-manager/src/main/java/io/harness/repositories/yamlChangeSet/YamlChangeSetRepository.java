package io.harness.repositories.yamlChangeSet;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlChangeSet.Status;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface YamlChangeSetRepository
    extends PagingAndSortingRepository<YamlChangeSet, String>, YamlChangeSetRepositoryCustom {
  int countByAccountIdAndStatus(String accountId, Status status);

  int countByAccountIdAndStatusAndQueueKey(String accountId, Status status, String queueKey);

  Optional<YamlChangeSet> findFirstByAccountIdAndQueueKeyAndStatusOrderByCreatedAt(
      String accountId, String queueKey, Status status);

  List<YamlChangeSet> findByAccountIdAndStatusAndLastUpdatedAtLessThan(
      List<String> accountIds, Status status, Long lastUpdatedCutOff);
}
