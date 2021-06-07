package io.harness.service;

import static io.harness.mongo.tracing.TracerConstants.ANALYZER_CACHE_NAME;

import io.harness.eventsframework.impl.redis.DistributedCache;
import io.harness.repositories.QueryStatsRepository;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class QueryStatsServiceImpl implements QueryStatsService {
  private static final int size = 100;

  @Inject @Named(ANALYZER_CACHE_NAME) DistributedCache queryRecordCache;
  @Inject QueryStatsRepository queryStatsRepository;

  public void storeHashesInsideCache() {
    int page = 1;
    while (queryStatsRepository.findAllHashes(page, size, queryRecordCache)) {
      page++;
    }
  }
}
