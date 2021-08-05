package io.harness.repositories.polling;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.polling.bean.PollingDocument;
import io.harness.polling.bean.PollingInfo;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.CDC)
public interface PollingRepository
    extends PagingAndSortingRepository<PollingDocument, String>, PollingRepositoryCustom {
  PollingDocument findByAccountIdAndUuid(String accountId, String uuid);
  PollingDocument findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPollingInfo(
      String accountId, String orgIdentifier, String projectIdentifier, PollingInfo pollingInfo);
}
