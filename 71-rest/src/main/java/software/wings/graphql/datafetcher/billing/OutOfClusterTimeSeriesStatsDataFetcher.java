package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import io.harness.ccm.billing.graphql.BillingAggregate;
import io.harness.ccm.billing.graphql.OutOfClusterBillingFilter;
import io.harness.ccm.billing.graphql.OutOfClusterGroupBy;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingService;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationListAndLimit;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OutOfClusterTimeSeriesStatsDataFetcher
    extends AbstractStatsDataFetcherWithAggregationListAndLimit<BillingAggregate, OutOfClusterBillingFilter,
        OutOfClusterGroupBy, QLNoOpSortCriteria> {
  @Inject PreAggregateBillingService preAggregateBillingService;
  @Inject OutOfClusterHelper outOfClusterHelper;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<BillingAggregate> aggregateFunction,
      List<OutOfClusterBillingFilter> filters, List<OutOfClusterGroupBy> groupByList, List<QLNoOpSortCriteria> sort,
      Integer limit, Integer offset) {
    String queryTableName = outOfClusterHelper.getCloudProviderTableName(filters);
    filters.removeIf(filter -> filter.getCloudProvider() != null);

    return preAggregateBillingService.getPreAggregateBillingTimeSeriesStats(Optional.ofNullable(aggregateFunction)
                                                                                .map(Collection::stream)
                                                                                .orElseGet(Stream::empty)
                                                                                .map(BillingAggregate::toFunctionCall)
                                                                                .collect(Collectors.toList()),
        Optional.ofNullable(groupByList)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(OutOfClusterGroupBy::toGroupbyObject)
            .collect(Collectors.toList()),
        Optional.ofNullable(filters)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(OutOfClusterBillingFilter::toCondition)
            .collect(Collectors.toList()),
        queryTableName);
  }

  @Override
  public String getEntityType() {
    return null;
  }

  @Override
  protected QLData postFetch(String accountId, List<OutOfClusterGroupBy> groupByList,
      List<BillingAggregate> aggregations, List<QLNoOpSortCriteria> sort, QLData qlData, Integer limit,
      boolean includeOthers) {
    return null;
  }
}
