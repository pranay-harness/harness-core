package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import io.harness.ccm.billing.GcpBillingService;
import io.harness.ccm.billing.graphql.CloudBillingAggregate;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingGroupBy;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationList;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class GcpBillingEntityStatsDataFetcher extends AbstractStatsDataFetcherWithAggregationList<CloudBillingAggregate,
    CloudBillingFilter, CloudBillingGroupBy, QLBillingSortCriteria> {
  @Inject GcpBillingService gcpBillingService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<CloudBillingAggregate> aggregateFunction,
      List<CloudBillingFilter> filters, List<CloudBillingGroupBy> groupBy, List<QLBillingSortCriteria> sort) {
    return gcpBillingService.getGcpBillingEntityStats(Optional.ofNullable(aggregateFunction)
                                                          .map(Collection::stream)
                                                          .orElseGet(Stream::empty)
                                                          .map(CloudBillingAggregate::toFunctionCall)
                                                          .collect(Collectors.toList()),
        Optional.ofNullable(groupBy)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(CloudBillingGroupBy::toGroupbyObject)
            .collect(Collectors.toList()),
        Optional.ofNullable(filters)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(CloudBillingFilter::toCondition)
            .collect(Collectors.toList()));
  }

  @Override
  public String getEntityType() {
    return null;
  }

  @Override
  protected QLData postFetch(String accountId, List<CloudBillingGroupBy> groupByList,
      List<CloudBillingAggregate> aggregations, List<QLBillingSortCriteria> sort, QLData qlData) {
    return null;
  }
}
