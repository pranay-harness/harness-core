package io.harness.repositories;

import io.harness.eventsframework.impl.redis.DistributedCache;

import java.util.List;

public interface QueryStatsRepositoryCustom {
  boolean findAllHashes(int page, int size, DistributedCache queryRecordCache);
  List<String> findAllServices();
}
