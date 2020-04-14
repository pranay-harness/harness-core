package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import io.harness.ccm.billing.graphql.BillingAggregate;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudGroupBy;
import io.harness.ccm.billing.graphql.CloudSortCriteria;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingService;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationListAndLimit;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CloudEntityStatsDataFetcher extends AbstractStatsDataFetcherWithAggregationListAndLimit<BillingAggregate,
    CloudBillingFilter, CloudGroupBy, CloudSortCriteria> {
  @Inject PreAggregateBillingService preAggregateBillingService;
  @Inject CloudHelper cloudHelper;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<BillingAggregate> aggregateFunction, List<CloudBillingFilter> filters,
      List<CloudGroupBy> groupByList, List<CloudSortCriteria> sort, Integer limit, Integer offset) {
    String queryTableName = cloudHelper.getCloudProviderTableName(filters);
    filters.removeIf(filter -> filter.getCloudProvider() != null);

    return preAggregateBillingService.getPreAggregateBillingEntityStats(Optional.ofNullable(aggregateFunction)
                                                                            .map(Collection::stream)
                                                                            .orElseGet(Stream::empty)
                                                                            .map(BillingAggregate::toFunctionCall)
                                                                            .collect(Collectors.toList()),
        Optional.ofNullable(groupByList)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(CloudGroupBy::toGroupbyObject)
            .collect(Collectors.toList()),
        Optional.ofNullable(filters)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(CloudBillingFilter::toCondition)
            .collect(Collectors.toList()),
        Optional.ofNullable(sort)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(CloudSortCriteria::toOrderObject)
            .collect(Collectors.toList()),
        queryTableName);
  }

  @Override
  protected QLData postFetch(String accountId, List<CloudGroupBy> groupByList, List<BillingAggregate> aggregations,
      List<CloudSortCriteria> sort, QLData qlData, Integer limit, boolean includeOthers) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
