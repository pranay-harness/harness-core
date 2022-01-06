/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.billing.preaggregated;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.billing.graphql.CloudBillingFilter;

import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.SqlObject;
import java.util.List;

@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
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
