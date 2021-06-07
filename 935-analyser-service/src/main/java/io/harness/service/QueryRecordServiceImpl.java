package io.harness.service;

import io.harness.eventsframework.impl.redis.DistributedCache;
import io.harness.repositories.QueryRecordsRepository;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Map;
import java.util.Set;
import org.redisson.api.RListMultimapCache;

public class QueryRecordServiceImpl implements QueryRecordService {
  private static final int size = 100;
  public static final String ANALYZER_CACHE_KEY = "dbAnalayzerCache";

  @Inject @Named("analyser_cache") DistributedCache queryRecordCache;
  @Inject QueryRecordsRepository queryRecordsRepository;

  public void storeHashesInsideCache() {
    int page = 1;
    while (true) {
      Map<String, Set<String>> queryRecordEntityList = queryRecordsRepository.findAllHashes(page, size);
      if (queryRecordEntityList.isEmpty()) {
        return;
      }
      RListMultimapCache<String, String> queryHashCache = queryRecordCache.getMultiMap("dbAnalayzerCache");
      for (Map.Entry<String, Set<String>> entry : queryRecordEntityList.entrySet()) {
        queryHashCache.putAll(entry.getKey(), entry.getValue());
      }
      page++;
    }
  }
}
