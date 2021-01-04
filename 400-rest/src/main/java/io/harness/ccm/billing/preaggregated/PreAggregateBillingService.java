package io.harness.ccm.billing.preaggregated;

import io.harness.ccm.billing.graphql.CloudBillingFilter;

import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.SqlObject;
import java.util.List;

public interface PreAggregateBillingService {
  PreAggregateBillingTimeSeriesStatsDTO getPreAggregateBillingTimeSeriesStats(List<SqlObject> aggregateFunction,
      List<Object> groupByObjects, List<Condition> conditions, List<SqlObject> sort, String tableName,
      List<SqlObject> leftJoin);

  PreAggregateBillingEntityStatsDTO getPreAggregateBillingEntityStats(String accountId,
      List<SqlObject> aggregateFunction, List<Object> groupByObjects, List<Condition> conditions, List<SqlObject> sort,
      String queryTableName, List<CloudBillingFilter> filters, List<SqlObject> leftJoin);

  PreAggregateBillingTrendStatsDTO getPreAggregateBillingTrendStats(List<SqlObject> aggregateFunction,
      List<Condition> conditions, String queryTableName, List<CloudBillingFilter> filters, List<SqlObject> leftJoin);

  PreAggregateFilterValuesDTO getPreAggregateFilterValueStats(String accountId, List<Object> groupByObjects,
      List<Condition> conditions, String queryTableName, SqlObject leftJoin, Integer limit, Integer offset);

  PreAggregateCloudOverviewDataDTO getPreAggregateBillingOverview(List<SqlObject> aggregateFunction,
      List<Object> groupByObjects, List<Condition> conditions, List<SqlObject> sort, String queryTableName,
      List<CloudBillingFilter> filters, SqlObject leftJoin);
}
