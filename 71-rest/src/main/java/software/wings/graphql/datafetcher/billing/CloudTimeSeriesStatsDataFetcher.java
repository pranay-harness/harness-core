package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import io.harness.ccm.billing.TimeSeriesDataPoints;
import io.harness.ccm.billing.graphql.CloudBillingAggregate;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingGroupBy;
import io.harness.ccm.billing.graphql.CloudBillingSortCriteria;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingService;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingTimeSeriesStatsDTO;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationListAndLimit;
import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CloudTimeSeriesStatsDataFetcher
    extends AbstractStatsDataFetcherWithAggregationListAndLimit<CloudBillingAggregate, CloudBillingFilter,
        CloudBillingGroupBy, CloudBillingSortCriteria> {
  @Inject PreAggregateBillingService preAggregateBillingService;
  @Inject CloudBillingHelper cloudBillingHelper;
  @Inject BillingDataHelper billingDataHelper;

  public static final String OTHERS = "Others";

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<CloudBillingAggregate> aggregateFunction,
      List<CloudBillingFilter> filters, List<CloudBillingGroupBy> groupByList, List<CloudBillingSortCriteria> sort,
      Integer limit, Integer offset) {
    String queryTableName = cloudBillingHelper.getCloudProviderTableName(accountId);

    return preAggregateBillingService.getPreAggregateBillingTimeSeriesStats(
        Optional.ofNullable(aggregateFunction)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(CloudBillingAggregate::toFunctionCall)
            .collect(Collectors.toList()),
        Optional.ofNullable(groupByList)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(CloudBillingGroupBy::toGroupbyObject)
            .collect(Collectors.toList()),
        Optional.ofNullable(filters)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(CloudBillingFilter::toCondition)
            .collect(Collectors.toList()),
        Optional.ofNullable(sort)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(CloudBillingSortCriteria::toOrderObject)
            .collect(Collectors.toList()),
        queryTableName);
  }

  @Override
  public String getEntityType() {
    return null;
  }

  @Override
  protected QLData postFetch(String accountId, List<CloudBillingGroupBy> groupByList,
      List<CloudBillingAggregate> aggregations, List<CloudBillingSortCriteria> sort, QLData qlData, Integer limit,
      boolean includeOthers) {
    PreAggregateBillingTimeSeriesStatsDTO data = (PreAggregateBillingTimeSeriesStatsDTO) qlData;
    Map<String, Double> aggregatedData = new HashMap<>();
    data.getStats().forEach(dataPoint -> {
      for (QLBillingDataPoint entry : dataPoint.getValues()) {
        QLReference qlReference = entry.getKey();
        if (qlReference != null && qlReference.getId() != null) {
          String key = qlReference.getId();
          if (aggregatedData.containsKey(key)) {
            aggregatedData.put(key, entry.getValue().doubleValue() + aggregatedData.get(key));
          } else {
            aggregatedData.put(key, entry.getValue().doubleValue());
          }
        }
      }
    });
    if (aggregatedData.isEmpty()) {
      return qlData;
    }
    List<String> selectedIdsAfterLimit = billingDataHelper.getElementIdsAfterLimit(aggregatedData, limit);

    return PreAggregateBillingTimeSeriesStatsDTO.builder()
        .stats(getDataAfterLimit(data, selectedIdsAfterLimit, includeOthers))
        .build();
  }

  private List<TimeSeriesDataPoints> getDataAfterLimit(
      PreAggregateBillingTimeSeriesStatsDTO data, List<String> selectedIdsAfterLimit, boolean includeOthers) {
    List<TimeSeriesDataPoints> limitProcessedData = new ArrayList<>();
    data.getStats().forEach(dataPoint -> {
      List<QLBillingDataPoint> limitProcessedValues = new ArrayList<>();
      QLBillingDataPoint others =
          QLBillingDataPoint.builder().key(QLReference.builder().id(OTHERS).name(OTHERS).build()).value(0).build();
      for (QLBillingDataPoint entry : dataPoint.getValues()) {
        String key = entry.getKey().getId();
        if (selectedIdsAfterLimit.contains(key)) {
          limitProcessedValues.add(entry);
        } else {
          others.setValue(others.getValue().doubleValue() + entry.getValue().doubleValue());
        }
      }

      if (others.getValue().doubleValue() > 0 && includeOthers) {
        others.setValue(billingDataHelper.getRoundedDoubleValue(others.getValue().doubleValue()));
        limitProcessedValues.add(others);
      }

      limitProcessedData.add(
          TimeSeriesDataPoints.builder().time(dataPoint.getTime()).values(limitProcessedValues).build());
    });
    return limitProcessedData;
  }
}
