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
  int countByAccountIdentifierAndStatus(String accountIdentifier, Status status);

  int countByAccountIdentifierAndStatusAndQueueKey(String accountIdentifier, Status status, String queueKey);

  Optional<YamlChangeSet> findFirstByAccountIdentifierAndQueueKeyAndStatusOrderByCreatedAt(
      String accountIdentifier, String queueKey, Status status);

  List<YamlChangeSet> findByAccountIdentifierAndStatusAndLastUpdatedAtLessThan(
      List<String> accountIdentifiers, Status status, Long lastUpdatedCutOff);
}
