package io.harness.repositories;

import static io.harness.mongo.tracing.TracerConstants.ANALYZER_CACHE_KEY;

import io.harness.event.QueryStats;
import io.harness.eventsframework.impl.redis.DistributedCache;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class QueryStatsRepositoryCustomImpl implements QueryStatsRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public boolean findAllHashes(int page, int size, DistributedCache queryRecordCache) {
    Pageable pageable = PageUtils.getPageRequest(page, size, new ArrayList<>());
    Query query = new Query().with(pageable);

    List<QueryStats> queryStatsList = mongoTemplate.find(query, QueryStats.class);
    if (queryStatsList.isEmpty()) {
      return false;
    }
    queryStatsList.forEach(queryStats -> {
      queryRecordCache.putInsideMap(String.format(ANALYZER_CACHE_KEY, queryStats.getServiceName()),
          queryStats.getHash(), queryStats.getCount() == null ? 0 : queryStats.getCount());
    });
    return true;
  }

  @Override
  public List<String> findAllServices() {
    return mongoTemplate.findDistinct("serviceName", QueryStats.class, String.class);
  }
}
