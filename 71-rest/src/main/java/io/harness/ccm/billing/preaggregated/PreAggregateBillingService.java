package io.harness.ccm.billing.preaggregated;

import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.SqlObject;

import java.util.List;

public interface PreAggregateBillingService {
  PreAggregateBillingTimeSeriesStatsDTO getPreAggregateBillingTimeSeriesStats(
      List<SqlObject> aggregateFunction, List<Object> groupbyObjects, List<Condition> conditions, String tableName);
}
