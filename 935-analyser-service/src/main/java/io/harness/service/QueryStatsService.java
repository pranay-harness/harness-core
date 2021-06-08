package io.harness.service;

import io.harness.event.QueryRecordEntity;
import io.harness.service.beans.QueryRecordKey;

import java.util.List;
import java.util.Map;

public interface QueryStatsService {
  void storeHashesInsideCache();
  void updateQueryStatsByAggregation(Map<QueryRecordKey, List<QueryRecordEntity>> queryRecordKeyListMap);
}
