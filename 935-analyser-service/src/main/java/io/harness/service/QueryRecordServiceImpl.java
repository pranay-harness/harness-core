package io.harness.service;

import static io.harness.mongo.tracing.TracerConstants.ANALYZER_CACHE_KEY;
import static io.harness.mongo.tracing.TracerConstants.ANALYZER_CACHE_NAME;

import io.harness.eventsframework.impl.redis.DistributedCache;
import io.harness.repositories.QueryRecordsRepository;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Map;
import java.util.Set;
import org.redisson.api.RListMultimapCache;

public class QueryRecordServiceImpl implements QueryRecordService {
  private static final int size = 100;

  @Inject @Named(ANALYZER_CACHE_NAME) DistributedCache queryRecordCache;
  @Inject QueryRecordsRepository queryRecordsRepository;

  public void storeHashesInsideCache() {
    int page = 1;
    while (true) {
      Map<String, Set<String>> queryRecordEntityList = queryRecordsRepository.findAllHashes(page, size);
      if (queryRecordEntityList.isEmpty()) {
        return;
      }
      RListMultimapCache<String, String> queryHashCache = queryRecordCache.getMultiMap(ANALYZER_CACHE_KEY);
      for (Map.Entry<String, Set<String>> entry : queryRecordEntityList.entrySet()) {
        queryHashCache.putAll(entry.getKey(), entry.getValue());
      }
      page++;
    }
  }
}
