/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.service;

import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.analyserservice.AnalyserServiceConfiguration;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.alerts.AlertMetadata;
import io.harness.beans.alerts.CollScanAlertInfo;
import io.harness.beans.alerts.ManyEntriesExaminedAlertInfo;
import io.harness.beans.alerts.SlowQueryAlertInfo;
import io.harness.beans.alerts.SortStageAlertInfo;
import io.harness.event.ExecutionStats;
import io.harness.event.InputStage;
import io.harness.event.QueryAlertCategory;
import io.harness.event.QueryExplainResult;
import io.harness.event.QueryPlanner.WinningPlan;
import io.harness.event.QueryRecordEntity;
import io.harness.event.QueryStats;
import io.harness.event.QueryStats.QueryStatsKeys;
import io.harness.repositories.QueryStatsRepository;
import io.harness.service.beans.QueryRecordKey;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.PIPELINE)
public class QueryStatsServiceImpl implements QueryStatsService {
  private static final int size = 100;

  @Inject QueryStatsRepository queryStatsRepository;
  @Inject private MongoTemplate mongoTemplate;
  @Inject AnalyserServiceConfiguration analyserServiceConfiguration;

  public void updateQueryStatsByAggregation(Map<QueryRecordKey, List<QueryRecordEntity>> queryRecordKeyListMap) {
    for (QueryRecordKey queryRecordKey : queryRecordKeyListMap.keySet()) {
      List<QueryRecordEntity> queryRecordsPerUniqueEntry = queryRecordKeyListMap.get(queryRecordKey);
      QueryStats averageAggregatedStats = getAverageAggregatedStats(queryRecordsPerUniqueEntry);
      if (averageAggregatedStats == null) {
        continue;
      }
      Query query = query(Criteria.where(QueryStatsKeys.hash).is(averageAggregatedStats.getHash()))
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
    QueryExplainResult averageExplainResult = getAverageExplainResult(queryRecordEntityList);
    return QueryStats.builder()
        .hash(latestQueryRecord.getHash())
        .serviceId(latestQueryRecord.getServiceName())
        .version(latestQueryRecord.getMajorVersion())
        .parsedQuery(latestQueryRecord.getParsedQuery())
        .collectionName(latestQueryRecord.getCollectionName())
        .count((long) queryRecordEntityList.size())
        .alerts(getAlertsDataFromLatestRecord(latestQueryRecord))
        .explainResult(averageExplainResult)
        .executionTimeMillis(averageExplainResult.getExecutionStats().getExecutionTimeMillis())
        .indexUsed(isIndexUsed(averageExplainResult))
        .alerts(getAlertsDataFromLatestRecord(latestQueryRecord))
        .createdAt(System.currentTimeMillis())
        .build();
  }

  private QueryExplainResult getAverageExplainResult(List<QueryRecordEntity> queryRecordEntityList) {
    QueryRecordEntity latestQueryRecord = queryRecordEntityList.get(0);

    long nReturned = 0;
    long executionTimeMillis = 0;
    long totalDocsExamined = 0;

    for (QueryRecordEntity queryRecordEntity : queryRecordEntityList) {
      nReturned += queryRecordEntity.getExplainResult().getExecutionStats().getNReturned();
      executionTimeMillis += queryRecordEntity.getExplainResult().getExecutionStats().getExecutionTimeMillis();
      totalDocsExamined += queryRecordEntity.getExplainResult().getExecutionStats().getTotalDocsExamined();
    }
    nReturned /= queryRecordEntityList.size();
    executionTimeMillis /= queryRecordEntityList.size();
    totalDocsExamined /= queryRecordEntityList.size();

    return QueryExplainResult.builder()
        .queryPlanner(latestQueryRecord.getExplainResult().getQueryPlanner())
        .executionStats(
            ExecutionStats.builder()
                .nReturned(nReturned)
                .executionTimeMillis(executionTimeMillis)
                .totalDocsExamined(totalDocsExamined)
                .executionStages(latestQueryRecord.getExplainResult().getExecutionStats().getExecutionStages())
                .build())
        .build();
  }

  private List<AlertMetadata> getAlertsDataFromLatestRecord(QueryRecordEntity queryRecordEntity) {
    List<AlertMetadata> alerts = new LinkedList<>();
    QueryExplainResult explainResult = queryRecordEntity.getExplainResult();
    if (explainResult == null) {
      return Collections.emptyList();
    }
    boolean indexUsed = isIndexUsed(explainResult);
    if (!indexUsed) {
      alerts.add(AlertMetadata.builder()
                     .alertCategory(QueryAlertCategory.COLLSCAN)
                     .alertInfo(CollScanAlertInfo.builder().build())
                     .build());
    }
    if (explainResult.getExecutionStats() != null
        && explainResult.getExecutionStats().getExecutionTimeMillis()
            > analyserServiceConfiguration.getExecutionTimeLimitMillis()) {
      alerts.add(AlertMetadata.builder()
                     .alertCategory(QueryAlertCategory.SLOW_QUERY)
                     .alertInfo(SlowQueryAlertInfo.builder().executionStats(explainResult.getExecutionStats()).build())
                     .build());
    }
    if (isSortStage(explainResult)) {
      alerts.add(AlertMetadata.builder()
                     .alertCategory(QueryAlertCategory.SORT_STAGE)
                     .alertInfo(SortStageAlertInfo.builder()
                                    .queryPlanner(explainResult.getQueryPlanner())
                                    .sortPattern(explainResult.getQueryPlanner().getWinningPlan().getSortPattern())
                                    .build())
                     .build());
    }
    if (explainResult.getExecutionStats().getTotalDocsExamined()
        > analyserServiceConfiguration.getManyEntriesAlertFactor() * explainResult.getExecutionStats().getNReturned()) {
      alerts.add(
          AlertMetadata.builder()
              .alertCategory(QueryAlertCategory.MANY_ENTRIES_EXAMINED)
              .alertInfo(
                  ManyEntriesExaminedAlertInfo.builder().executionStats(explainResult.getExecutionStats()).build())
              .build());
    }
    return alerts;
  }

  private boolean isIndexUsed(QueryExplainResult queryExplainResult) {
    if (queryExplainResult == null) {
      return false;
    }
    WinningPlan winningPlan = queryExplainResult.getQueryPlanner().getWinningPlan();
    if (winningPlan.getStage().equals("COLLSCAN")) {
      return false;
    }
    if (winningPlan.getStage().equals("IDHACK")) {
      return true;
    }
    boolean isIXSCAN = false;
    InputStage inputStage = winningPlan.getInputStage();
    while (inputStage != null) {
      if (inputStage.getStage().equals("COLLSCAN")) {
        return false;
      }
      if (inputStage.getStage().equals("IXSCAN")) {
        isIXSCAN = true;
      }
      if (inputStage.getStage().equals("IDHACK")) {
        return true;
      }
      inputStage = inputStage.getInputStage();
    }

    return isIXSCAN;
  }

  private boolean isSortStage(QueryExplainResult queryExplainResult) {
    if (queryExplainResult == null) {
      return false;
    }
    WinningPlan winningPlan = queryExplainResult.getQueryPlanner().getWinningPlan();
    if (winningPlan.getStage().equals("SORT")) {
      return true;
    }
    InputStage inputStage = winningPlan.getInputStage();
    while (inputStage != null) {
      if (inputStage.getStage().equals("SORT")) {
        return false;
      }
      inputStage = inputStage.getInputStage();
    }
    return false;
  }
}
