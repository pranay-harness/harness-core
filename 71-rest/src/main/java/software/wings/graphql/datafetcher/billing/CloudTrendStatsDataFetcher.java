package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import io.harness.ccm.billing.graphql.CloudBillingAggregate;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingGroupBy;
import io.harness.ccm.billing.graphql.CloudBillingSortCriteria;
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

public class CloudTrendStatsDataFetcher extends AbstractStatsDataFetcherWithAggregationList<CloudBillingAggregate,
    CloudBillingFilter, CloudBillingGroupBy, CloudBillingSortCriteria> {
  @Inject PreAggregateBillingService preAggregateBillingService;
  @Inject CloudBillingHelper cloudBillingHelper;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<CloudBillingAggregate> aggregateFunction,
      List<CloudBillingFilter> filters, List<CloudBillingGroupBy> groupBy, List<CloudBillingSortCriteria> sort) {
    String queryTableName = cloudBillingHelper.getCloudProviderTableName(filters);

    return preAggregateBillingService.getPreAggregateBillingTrendStats(Optional.ofNullable(aggregateFunction)
                                                                           .map(Collection::stream)
                                                                           .orElseGet(Stream::empty)
                                                                           .map(CloudBillingAggregate::toFunctionCall)
                                                                           .collect(Collectors.toList()),
        Optional.ofNullable(filters)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(CloudBillingFilter::toCondition)
            .collect(Collectors.toList()),
        queryTableName, filters);
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
