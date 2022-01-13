/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import static software.wings.graphql.datafetcher.billing.CloudBillingHelper.unified;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.views.service.ViewsBillingService;

import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationListAndLimit;
import software.wings.graphql.datafetcher.billing.CloudBillingHelper;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.cloud.bigquery.BigQuery;
import com.google.inject.Inject;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;

@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public class ViewEntityStatsDataFetcher extends AbstractStatsDataFetcherWithAggregationListAndLimit<QLCEViewAggregation,
    QLCEViewFilterWrapper, QLCEViewGroupBy, QLCEViewSortCriteria> {
  @Inject ViewsBillingService viewsBillingService;
  @Inject CloudBillingHelper cloudBillingHelper;
  @Inject BigQueryService bigQueryService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCEViewAggregation> aggregateFunction,
      List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy, List<QLCEViewSortCriteria> sort,
      Integer limit, Integer offset) {
    String cloudProviderTableName = cloudBillingHelper.getCloudProviderTableName(accountId, unified);
    BigQuery bigQuery = bigQueryService.get();
    return QLCEViewEntityStatsData.builder()
        .data(viewsBillingService.getEntityStatsDataPoints(
            bigQuery, filters, groupBy, aggregateFunction, sort, cloudProviderTableName, limit, offset))
        .build();
  }

  @Override
  protected QLData postFetch(String accountId, List<QLCEViewGroupBy> groupByList,
      List<QLCEViewAggregation> aggregations, List<QLCEViewSortCriteria> sort, QLData qlData, Integer limit,
      boolean includeOthers) {
    return null;
  }

  @Override
  protected QLData fetchSelectedFields(String accountId, List<QLCEViewAggregation> aggregateFunction,
      List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy, List<QLCEViewSortCriteria> sort,
      Integer limit, Integer offset, boolean skipRoundOff, DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }

  @Override
  public boolean isCESampleAccountIdAllowed() {
    return false;
  }
}
