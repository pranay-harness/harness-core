package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import com.healthmarketscience.sqlbuilder.SqlObject;
import io.harness.ccm.billing.graphql.CloudBillingAggregate;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingGroupBy;
import io.harness.ccm.billing.graphql.CloudBillingSortCriteria;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingService;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationList;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CloudTrendStatsDataFetcher extends AbstractStatsDataFetcherWithAggregationList<CloudBillingAggregate,
    CloudBillingFilter, CloudBillingGroupBy, CloudBillingSortCriteria> {
  @Inject PreAggregateBillingService preAggregateBillingService;
  @Inject CloudBillingHelper cloudBillingHelper;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<CloudBillingAggregate> aggregateFunction,
      List<CloudBillingFilter> filters, List<CloudBillingGroupBy> groupBy, List<CloudBillingSortCriteria> sort) {
    boolean isDiscountsAggregationPresent = cloudBillingHelper.fetchIfDiscountsAggregationPresent(aggregateFunction);
    boolean isQueryRawTableRequired = cloudBillingHelper.fetchIfRawTableQueryRequired(filters, Collections.EMPTY_LIST);
    boolean isAWSCloudProvider = false;
    List<SqlObject> leftJoin = null;
    String queryTableName;
    if (isQueryRawTableRequired) {
      String cloudProvider = cloudBillingHelper.getCloudProvider(filters);
      isAWSCloudProvider = cloudProvider.equals("AWS");
      String tableName = cloudBillingHelper.getTableName(cloudProvider);
      queryTableName = cloudBillingHelper.getCloudProviderTableName(accountId, tableName);
      filters = cloudBillingHelper.removeAndReturnCloudProviderFilter(filters);
      leftJoin = new ArrayList<>();
      leftJoin.add(cloudBillingHelper.getLeftJoin(cloudProvider));
      if (isDiscountsAggregationPresent && !isAWSCloudProvider) {
        leftJoin.add(cloudBillingHelper.getCreditsLeftJoin());
      }
    } else {
      queryTableName = cloudBillingHelper.getCloudProviderTableName(accountId);
    }

    return preAggregateBillingService.getPreAggregateBillingTrendStats(
        Optional.ofNullable(aggregateFunction)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(cloudBillingHelper.getAggregationMapper(isAWSCloudProvider, isQueryRawTableRequired))
            .collect(Collectors.toList()),
        Optional.ofNullable(filters)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(cloudBillingHelper.getFiltersMapper(isAWSCloudProvider, isQueryRawTableRequired))
            .collect(Collectors.toList()),
        queryTableName, filters, leftJoin);
  }

  @Override
  protected QLData postFetch(String accountId, List<CloudBillingGroupBy> groupByList,
      List<CloudBillingAggregate> aggregations, List<CloudBillingSortCriteria> sort, QLData qlData) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
