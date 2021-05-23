package io.harness.repositories.yamlChangeSet;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.YamlChangeSet;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface YamlChangeSetRepository
    extends PagingAndSortingRepository<YamlChangeSet, String>, YamlChangeSetRepositoryCustom {
  int countByAccountIdAndStatusIn(String accountId, List<String> status);

  int countByAccountIdAndStatusInAndQueueKey(String accountId, List<String> status, String queueKey);

  Optional<YamlChangeSet> findFirstByAccountIdAndQueueKeyAndStatusOrderByCreatedAt(
      String accountId, String queueKey, String status);

  List<YamlChangeSet> findByAccountIdAndStatusInAndLastUpdatedAtLessThan(
      List<String> accountIds, List<String> status, Long lastUpdatedCutOff);

  List<YamlChangeSet> findByAccountIdAndQueueKeyAndStatus(String accountId, String queueKey, String status);
}
