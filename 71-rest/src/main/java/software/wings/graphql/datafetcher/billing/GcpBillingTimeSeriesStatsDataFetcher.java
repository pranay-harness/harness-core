package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import io.harness.ccm.billing.GcpBillingService;
import io.harness.ccm.billing.GcpBillingTimeSeriesStatsDTO;
import io.harness.ccm.billing.graphql.BillingAggregate;
import io.harness.ccm.billing.graphql.OutOfClusterBillingFilter;
import io.harness.ccm.billing.graphql.OutOfClusterGroupBy;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcher;
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
public class GcpBillingTimeSeriesStatsDataFetcher extends AbstractStatsDataFetcher<BillingAggregate,
    OutOfClusterBillingFilter, OutOfClusterGroupBy, QLBillingSortCriteria> {
  @Inject GcpBillingService gcpBillingService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected GcpBillingTimeSeriesStatsDTO fetch(String accountId, BillingAggregate aggregateFunction,
      List<OutOfClusterBillingFilter> filters, List<OutOfClusterGroupBy> groupBys, List<QLBillingSortCriteria> sort) {
    return gcpBillingService.getGcpBillingTimeSeriesStats(
        Optional.ofNullable(aggregateFunction).orElse(BillingAggregate.builder().build()).toFunctionCall(),
        Optional.ofNullable(groupBys)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(OutOfClusterGroupBy::toGroupbyObject)
            .collect(Collectors.toList()),
        Optional.ofNullable(filters)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(OutOfClusterBillingFilter::toCondition)
            .collect(Collectors.toList()));
  }

  @Override
  protected QLData postFetch(String accountId, List<OutOfClusterGroupBy> groupByList, QLData qlData) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
