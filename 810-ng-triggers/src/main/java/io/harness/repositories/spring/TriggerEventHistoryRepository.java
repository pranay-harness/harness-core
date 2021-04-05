package io.harness.repositories.spring;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.entity.TriggerEventHistory;
import io.harness.repositories.custom.TriggerEventHistoryRepositoryCustom;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PIPELINE)
public interface TriggerEventHistoryRepository
    extends PagingAndSortingRepository<TriggerEventHistory, String>, TriggerEventHistoryRepositoryCustom {
  List<TriggerEventHistory> findFirst1ByAccountIdAndOrgIdentifierAndProjectIdentifierAndTriggerIdentifier(
      String accountId, String orgIdentifier, String projectIdentifier, String triggerIdentifier, Sort sort);
}
