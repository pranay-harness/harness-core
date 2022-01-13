/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
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
import software.wings.service.intfc.ce.CeAccountExpirationChecker;

import com.google.inject.Inject;
import com.healthmarketscience.sqlbuilder.SqlObject;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class CloudTimeSeriesStatsDataFetcher
    extends AbstractStatsDataFetcherWithAggregationListAndLimit<CloudBillingAggregate, CloudBillingFilter,
        CloudBillingGroupBy, CloudBillingSortCriteria> {
  @Inject PreAggregateBillingService preAggregateBillingService;
  @Inject CloudBillingHelper cloudBillingHelper;
  @Inject BillingDataHelper billingDataHelper;
  @Inject CeAccountExpirationChecker accountChecker;

  public static final String OTHERS = "Others";

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<CloudBillingAggregate> aggregateFunction,
      List<CloudBillingFilter> filters, List<CloudBillingGroupBy> groupByList, List<CloudBillingSortCriteria> sort,
      Integer limit, Integer offset) {
    accountChecker.checkIsCeEnabled(accountId);
    boolean isQueryRawTableRequired = cloudBillingHelper.fetchIfRawTableQueryRequired(filters, groupByList);
    boolean isDiscountsAggregationPresent = cloudBillingHelper.fetchIfDiscountsAggregationPresent(aggregateFunction);
    boolean isAWSCloudProvider = false;

    List<SqlObject> leftJoin = null;
    String queryTableName;
    if (isQueryRawTableRequired) {
      String cloudProvider = cloudBillingHelper.getCloudProvider(filters);
      isAWSCloudProvider = cloudProvider.equals("AWS");
      leftJoin = new ArrayList<>();
      leftJoin.add(cloudBillingHelper.getLeftJoin(cloudProvider));
      if (isDiscountsAggregationPresent && !isAWSCloudProvider) {
        leftJoin.add(cloudBillingHelper.getCreditsLeftJoin());
      }
      String tableName = cloudBillingHelper.getTableName(cloudBillingHelper.getCloudProvider(filters));
      queryTableName = cloudBillingHelper.getCloudProviderTableName(accountId, tableName);
      filters = cloudBillingHelper.removeAndReturnCloudProviderFilter(filters);
      groupByList = cloudBillingHelper.removeAndReturnCloudProviderGroupBy(groupByList);
    } else {
      queryTableName = cloudBillingHelper.getCloudProviderTableName(accountId);
    }

    cloudBillingHelper.processAndAddLinkedAccountsFilter(accountId, filters);

    return preAggregateBillingService.getPreAggregateBillingTimeSeriesStats(
        Optional.ofNullable(aggregateFunction)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(cloudBillingHelper.getAggregationMapper(isAWSCloudProvider, isQueryRawTableRequired))
            .collect(Collectors.toList()),
        Optional.ofNullable(groupByList)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(cloudBillingHelper.getGroupByMapper(isAWSCloudProvider, isQueryRawTableRequired))
            .collect(Collectors.toList()),
        Optional.ofNullable(filters)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(cloudBillingHelper.getFiltersMapper(isAWSCloudProvider, isQueryRawTableRequired))
            .filter(condition -> condition != null)
            .collect(Collectors.toList()),
        Optional.ofNullable(sort)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(CloudBillingSortCriteria::toOrderObject)
            .collect(Collectors.toList()),
        queryTableName, leftJoin);
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

  @Override
  protected QLData fetchSelectedFields(String accountId, List<CloudBillingAggregate> aggregateFunction,
      List<CloudBillingFilter> filters, List<CloudBillingGroupBy> groupBy, List<CloudBillingSortCriteria> sort,
      Integer limit, Integer offset, boolean skipRoundOff, DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
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

  @Override
  public boolean isCESampleAccountIdAllowed() {
    return false;
  }
}
