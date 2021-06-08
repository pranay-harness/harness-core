package io.harness.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.event.QueryStats;

import java.util.List;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface QueryStatsRepository
    extends PagingAndSortingRepository<QueryStats, String>, QueryStatsRepositoryCustom {
  <S extends QueryStats> S save(S queryStats);

  List<QueryStats> findByServiceIdAndVersionAndExecutionTimeMillisGreaterThanOrderByExecutionTimeMillisDesc(
      String serviceId, String version, long executionTimeLimit);

  List<QueryStats> findByServiceIdAndVersion(String service, String version);
}
