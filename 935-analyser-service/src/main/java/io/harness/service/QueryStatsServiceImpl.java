package io.harness.service;

import static io.harness.mongo.tracing.TracerConstants.ANALYZER_CACHE_NAME;

import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.event.QueryRecordEntity;
import io.harness.event.QueryStats;
import io.harness.event.QueryStats.QueryStatsBuilder;
import io.harness.event.QueryStats.QueryStatsKeys;
import io.harness.eventsframework.impl.redis.DistributedCache;
import io.harness.repositories.QueryStatsRepository;
import io.harness.service.beans.QueryRecordKey;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Map;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

public class QueryStatsServiceImpl implements QueryStatsService {
  private static final int size = 100;

  @Inject @Named(ANALYZER_CACHE_NAME) DistributedCache queryRecordCache;
  @Inject QueryStatsRepository queryStatsRepository;
  @Inject private MongoTemplate mongoTemplate;

  public void storeHashesInsideCache() {
    int page = 1;
    while (queryStatsRepository.findAllHashes(page, size, queryRecordCache)) {
      page++;
    }
  }

  public void updateQueryStatsByAggregation(Map<QueryRecordKey, List<QueryRecordEntity>> queryRecordKeyListMap) {
    for (QueryRecordKey queryRecordKey : queryRecordKeyListMap.keySet()) {
      List<QueryRecordEntity> queryRecordsPerUniqueEntry = queryRecordKeyListMap.get(queryRecordKey);
      QueryStats averageAggregatedStats = getAverageAggregatedStats(queryRecordsPerUniqueEntry);
      if (averageAggregatedStats == null) {
        continue;
      }
      Query query =
          query(Criteria.where(QueryStatsKeys.hash).is(averageAggregatedStats.getHash()))
              .addCriteria(Criteria.where(QueryStatsKeys.serviceId).is(averageAggregatedStats.getServiceId()))
              .addCriteria(Criteria.where(QueryStatsKeys.version).is(averageAggregatedStats.getVersion()));
      mongoTemplate.remove(query, QueryStats.class);
      queryStatsRepository.save(averageAggregatedStats);
    }
  }

  private QueryStats getAverageAggregatedStats(List<QueryRecordEntity> queryRecordEntityList) {
    if (queryRecordEntityList.isEmpty()) {
      return null;
    }
    QueryRecordEntity latestQueryRecord = queryRecordEntityList.get(0);
    QueryStatsBuilder statsBuilder = QueryStats.builder()
                                         .hash(latestQueryRecord.getHash())
                                         .serviceId(latestQueryRecord.getServiceName())
                                         .version(latestQueryRecord.getVersion())
                                         .data(ByteString.copyFrom(latestQueryRecord.getData()).toStringUtf8());

    return statsBuilder.build();
  }
}
