package io.harness.repositories;

import io.harness.eventsframework.impl.redis.DistributedCache;

public interface QueryStatsRepositoryCustom {
  boolean findAllHashes(int page, int size, DistributedCache queryRecordCache);
}
