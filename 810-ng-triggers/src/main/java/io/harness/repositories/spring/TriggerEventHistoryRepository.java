package io.harness.repositories.ng.core.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ngtriggers.beans.entity.TriggerEventHistory;
import io.harness.repositories.ng.core.custom.TriggerEventHistoryRepositoryCustom;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface TriggerEventHistoryRepository
    extends PagingAndSortingRepository<TriggerEventHistory, String>, TriggerEventHistoryRepositoryCustom {
  List<TriggerEventHistory> findFirst1ByAccountIdAndOrgIdentifierAndProjectIdentifierAndTriggerIdentifier(
      String accountId, String orgIdentifier, String projectIdentifier, String triggerIdentifier, Sort sort);
}
