package io.harness.ccm.billing.preaggregated;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.SqlObject;
import io.harness.ccm.billing.bigquery.BigQueryService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Singleton
public class PreAggregateBillingServiceImpl implements PreAggregateBillingService {
  private BigQueryService bigQueryService;
  private PreAggregatedBillingDataHelper dataHelper;

  @Inject
  PreAggregateBillingServiceImpl(BigQueryService bigQueryService, PreAggregatedBillingDataHelper dataHelper) {
    this.bigQueryService = bigQueryService;
    this.dataHelper = dataHelper;
  }

  @Override
  public PreAggregateBillingTimeSeriesStatsDTO getPreAggregateBillingTimeSeriesStats(List<SqlObject> aggregateFunction,
      List<Object> groupByObjects, List<Condition> conditions, List<SqlObject> sort, String tableName) {
    String timeSeriesDataQuery = dataHelper.getQuery(aggregateFunction, groupByObjects, conditions, sort);
    // Replacing the Default Table with the Table in the context
    timeSeriesDataQuery = timeSeriesDataQuery.replaceAll(PreAggregatedTableSchema.defaultTableName, tableName);
    logger.info("getPreAggregateBillingTimeSeriesStats Query {}", timeSeriesDataQuery);
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(timeSeriesDataQuery).build();
    TableResult result = null;
    try {
      result = bigQueryService.get().query(queryConfig);
    } catch (InterruptedException e) {
      logger.error("Failed to get getPreAggregateBillingTimeSeriesStats. {}", e);
      Thread.currentThread().interrupt();
      return null;
    }
    return dataHelper.convertToPreAggregatesTimeSeriesData(result);
  }

  @Override
  public PreAggregateBillingEntityStatsDTO getPreAggregateBillingEntityStats(List<SqlObject> aggregateFunction,
      List<Object> groupByObjects, List<Condition> conditions, List<SqlObject> sort, String queryTableName) {
    String entityDataQuery = dataHelper.getQuery(aggregateFunction, groupByObjects, conditions, sort);
    // Replacing the Default Table with the Table in the context
    entityDataQuery = entityDataQuery.replaceAll(PreAggregatedTableSchema.defaultTableName, queryTableName);
    logger.info("getPreAggregateBillingEntityStats Query {}", entityDataQuery);
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(entityDataQuery).build();
    TableResult result = null;
    try {
      result = bigQueryService.get().query(queryConfig);
    } catch (InterruptedException e) {
      logger.error("Failed to get getPreAggregateBillingEntityStats. {}", e);
      Thread.currentThread().interrupt();
      return null;
    }
    return dataHelper.convertToPreAggregatesEntityData(result);
  }
}
