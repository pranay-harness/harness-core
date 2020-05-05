package io.harness.ccm.billing.preaggregated;

import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.SqlObject;
import io.harness.ccm.billing.graphql.CloudBillingFilter;

import java.util.List;

public interface PreAggregateBillingService {
  PreAggregateBillingTimeSeriesStatsDTO getPreAggregateBillingTimeSeriesStats(List<SqlObject> aggregateFunction,
      List<Object> groupByObjects, List<Condition> conditions, List<SqlObject> sort, String tableName);

  PreAggregateBillingEntityStatsDTO getPreAggregateBillingEntityStats(String accountId,
      List<SqlObject> aggregateFunction, List<Object> groupByObjects, List<Condition> conditions, List<SqlObject> sort,
      String queryTableName);

  PreAggregateBillingTrendStatsDTO getPreAggregateBillingTrendStats(List<SqlObject> aggregateFunction,
      List<Condition> conditions, String queryTableName, List<CloudBillingFilter> filters);

  PreAggregateFilterValuesDTO getPreAggregateFilterValueStats(
      String accountId, List<Object> groupByObjects, List<Condition> conditions, String queryTableName);

  PreAggregateCloudOverviewDataDTO getPreAggregateBillingOverview(List<SqlObject> aggregateFunction,
      List<Object> groupByObjects, List<Condition> conditions, List<SqlObject> sort, String queryTableName);
}
