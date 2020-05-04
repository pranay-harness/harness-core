package io.harness.ccm.billing.preaggregated;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.SqlObject;
import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.setup.CECloudAccountDao;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ce.CECloudAccount;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class PreAggregateBillingServiceImpl implements PreAggregateBillingService {
  private BigQueryService bigQueryService;
  private PreAggregatedBillingDataHelper dataHelper;
  private CECloudAccountDao ceCloudAccountDao;

  private static final String TOTAL_COST_LABEL = "Total Cost";

  @Inject
  PreAggregateBillingServiceImpl(
      BigQueryService bigQueryService, PreAggregatedBillingDataHelper dataHelper, CECloudAccountDao ceCloudAccountDao) {
    this.bigQueryService = bigQueryService;
    this.dataHelper = dataHelper;
    this.ceCloudAccountDao = ceCloudAccountDao;
  }

  @Override
  public PreAggregateBillingTimeSeriesStatsDTO getPreAggregateBillingTimeSeriesStats(List<SqlObject> aggregateFunction,
      List<Object> groupByObjects, List<Condition> conditions, List<SqlObject> sort, String tableName) {
    Preconditions.checkNotNull(
        groupByObjects, "Queries to getPreAggregateBillingTimeSeriesStats need at least one groupBy");
    String timeSeriesDataQuery = dataHelper.getQuery(aggregateFunction, groupByObjects, conditions, sort, true);
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
  public PreAggregateBillingEntityStatsDTO getPreAggregateBillingEntityStats(String accountId,
      List<SqlObject> aggregateFunction, List<Object> groupByObjects, List<Condition> conditions, List<SqlObject> sort,
      String queryTableName) {
    Preconditions.checkNotNull(
        groupByObjects, "Queries to getPreAggregateBillingEntityStats need at least one groupBy");
    String entityDataQuery = dataHelper.getQuery(aggregateFunction, groupByObjects, conditions, sort, false);
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
    return dataHelper.convertToPreAggregatesEntityData(result, linkedAccountsMap(accountId));
  }

  @Override
  public PreAggregateBillingTrendStatsDTO getPreAggregateBillingTrendStats(List<SqlObject> aggregateFunction,
      List<Condition> conditions, String queryTableName, List<CloudBillingFilter> filters) {
    PreAggregatedCostDataStats preAggregatedCostDataStats =
        getAggregatedCostData(aggregateFunction, conditions, queryTableName);
    if (preAggregatedCostDataStats != null) {
      return PreAggregateBillingTrendStatsDTO.builder()
          .blendedCost(
              dataHelper.getCostBillingStats(preAggregatedCostDataStats.getBlendedCost(), filters, TOTAL_COST_LABEL))
          .unBlendedCost(
              dataHelper.getCostBillingStats(preAggregatedCostDataStats.getUnBlendedCost(), filters, TOTAL_COST_LABEL))
          .cost(dataHelper.getCostBillingStats(preAggregatedCostDataStats.getCost(), filters, TOTAL_COST_LABEL))
          .build();
    } else {
      return PreAggregateBillingTrendStatsDTO.builder().build();
    }
  }

  @Override
  public PreAggregateFilterValuesDTO getPreAggregateFilterValueStats(
      String accountId, List<Object> groupByObjects, List<Condition> conditions, String queryTableName) {
    String filterValueQuery = dataHelper.getQuery(null, groupByObjects, conditions, null, false);
    // Replacing the Default Table with the Table in the context
    filterValueQuery = filterValueQuery.replaceAll(PreAggregatedTableSchema.defaultTableName, queryTableName);
    logger.info("getPreAggregateFilterValueStats Query {}", filterValueQuery);
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(filterValueQuery).build();
    TableResult result = null;
    try {
      result = bigQueryService.get().query(queryConfig);
    } catch (InterruptedException e) {
      logger.error("Failed to get getPreAggregateBillingEntityStats. {}", e);
      Thread.currentThread().interrupt();
      return null;
    }
    return dataHelper.convertToPreAggregatesFilterValue(result, linkedAccountsMap(accountId));
  }

  public PreAggregatedCostDataStats getAggregatedCostData(
      List<SqlObject> aggregateFunction, List<Condition> conditions, String queryTableName) {
    String trendStatsQuery = dataHelper.getQuery(aggregateFunction, null, conditions, null, false);
    // Replacing the Default Table with the Table in the context
    trendStatsQuery = trendStatsQuery.replaceAll(PreAggregatedTableSchema.defaultTableName, queryTableName);
    logger.info("getAggregatedCostData Query {}", trendStatsQuery);
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(trendStatsQuery).build();
    TableResult result = null;
    try {
      result = bigQueryService.get().query(queryConfig);
    } catch (InterruptedException e) {
      logger.error("Failed to get AggregatedCostData. {}", e);
      Thread.currentThread().interrupt();
      return null;
    }
    return dataHelper.convertToAggregatedCostData(result);
  }

  private Map<String, String> linkedAccountsMap(String harnessAccountId) {
    List<CECloudAccount> awsCloudAccountList = ceCloudAccountDao.getByAWSAccountId(harnessAccountId);
    return awsCloudAccountList.stream().collect(
        Collectors.toMap(CECloudAccount::getInfraAccountId, CECloudAccount::getAccountName));
  }
}
