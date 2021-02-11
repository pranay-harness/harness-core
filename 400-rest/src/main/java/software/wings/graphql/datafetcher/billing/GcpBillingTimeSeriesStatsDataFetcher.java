package software.wings.graphql.datafetcher.billing;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.billing.GcpBillingService;
import io.harness.ccm.billing.GcpBillingTimeSeriesStatsDTO;
import io.harness.ccm.billing.graphql.CloudBillingAggregate;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingGroupBy;

import software.wings.graphql.datafetcher.AbstractStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ce.CeAccountExpirationChecker;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(Module._380_CG_GRAPHQL)
public class GcpBillingTimeSeriesStatsDataFetcher extends AbstractStatsDataFetcher<CloudBillingAggregate,
    CloudBillingFilter, CloudBillingGroupBy, QLBillingSortCriteria> {
  @Inject GcpBillingService gcpBillingService;
  @Inject CeAccountExpirationChecker accountChecker;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected GcpBillingTimeSeriesStatsDTO fetch(String accountId, CloudBillingAggregate aggregateFunction,
      List<CloudBillingFilter> filters, List<CloudBillingGroupBy> groupBys, List<QLBillingSortCriteria> sort) {
    accountChecker.checkIsCeEnabled(accountId);
    return gcpBillingService.getGcpBillingTimeSeriesStats(
        Optional.ofNullable(aggregateFunction).orElse(CloudBillingAggregate.builder().build()).toFunctionCall(),
        Optional.ofNullable(groupBys)
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
  protected QLData postFetch(String accountId, List<CloudBillingGroupBy> groupByList, QLData qlData) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
