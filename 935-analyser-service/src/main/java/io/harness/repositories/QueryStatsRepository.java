package io.harness.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.event.QueryStats;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface QueryStatsRepository
    extends PagingAndSortingRepository<QueryStats, String>, QueryStatsRepositoryCustom {
  <S extends QueryStats> S save(S queryStats);
}
