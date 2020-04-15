package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import io.harness.ccm.billing.graphql.BillingAggregate;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudGroupBy;
import io.harness.ccm.billing.graphql.CloudSortCriteria;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingService;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationList;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CloudTrendStatsDataFetcher extends AbstractStatsDataFetcherWithAggregationList<BillingAggregate,
    CloudBillingFilter, CloudGroupBy, CloudSortCriteria> {
  @Inject PreAggregateBillingService preAggregateBillingService;
  @Inject CloudHelper cloudHelper;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<BillingAggregate> aggregateFunction, List<CloudBillingFilter> filters,
      List<CloudGroupBy> groupBy, List<CloudSortCriteria> sort) {
    String queryTableName = cloudHelper.getCloudProviderTableName(filters);
    filters.removeIf(filter -> filter.getCloudProvider() != null);

    return preAggregateBillingService.getPreAggregateBillingTrendStats(Optional.ofNullable(aggregateFunction)
                                                                           .map(Collection::stream)
                                                                           .orElseGet(Stream::empty)
                                                                           .map(BillingAggregate::toFunctionCall)
                                                                           .collect(Collectors.toList()),
        Optional.ofNullable(filters)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(CloudBillingFilter::toCondition)
            .collect(Collectors.toList()),
        queryTableName, filters);
  }

  @Override
  protected QLData postFetch(String accountId, List<CloudGroupBy> groupByList, List<BillingAggregate> aggregations,
      List<CloudSortCriteria> sort, QLData qlData) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
