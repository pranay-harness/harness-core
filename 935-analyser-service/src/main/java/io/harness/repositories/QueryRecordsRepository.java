package io.harness.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.QueryRecordEntity;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.PIPELINE)
public interface QueryRecordsRepository
    extends PagingAndSortingRepository<QueryRecordEntity, String>, QueryRecordsRepositoryCustom {
  <S extends QueryRecordEntity> S save(S QueryRecordEntity);
}