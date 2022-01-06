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
import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationList;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingStatsInfo;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingTrendStats;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLStatsBreakdownInfo;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ce.CeAccountExpirationChecker;

import com.google.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class BillingTrendStatsDataFetcher extends AbstractStatsDataFetcherWithAggregationList<QLCCMAggregationFunction,
    QLBillingDataFilter, QLCCMGroupBy, QLBillingSortCriteria> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;
  @Inject BillingDataHelper billingDataHelper;
  @Inject private IdleCostTrendStatsDataFetcher idleCostTrendStatsDataFetcher;
  @Inject CeAccountExpirationChecker accountChecker;

  private static final String TOTAL_COST_DESCRIPTION = "of %s - %s";
  private static final String TOTAL_COST_LABEL = "Total Cost";
  private static final String TREND_COST_LABEL = "Cost Trend";
  private static final String TREND_COST_DESCRIPTION = "$%s over %s - %s";
  private static final String TOTAL_COST_VALUE = "$%s";
  private static final String TREND_COST_VALUE = "%s";
  private static final String IDLE_COST_DESCRIPTION = "%s of total";
  private static final String IDLE_COST_LABEL = "Idle Cost";
  private static final String UNALLOCATED_COST_DESCRIPTION = "%s of total";
  private static final String UNALLOCATED_COST_LABEL = "Unallocated Cost";
  private static final String UTILIZED_COST_DESCRIPTION = "%s of total";
  private static final String UTILIZED_COST_LABEL = "Utilized Cost";
  private static final String SYSTEM_COST_DESCRIPTION = "%s of total";
  private static final String SYSTEM_COST_LABEL = "System Cost";
  private static final String COST_VALUE = "$%s";
  private static final String EMPTY_VALUE = "-";
  private static final String NA_VALUE = "NA";
  private static final String EFFICIENCY_SCORE_LABEL = "Efficiency Score";

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort) {
    accountChecker.checkIsCeEnabled(accountId);
    try {
      if (timeScaleDBService.isValid()) {
        return getData(accountId, aggregateFunction, filters);
      } else {
        throw new InvalidRequestException("Cannot process request in BillingTrendStatsDataFetcher");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while billing data", e);
    }
  }

  protected QLBillingTrendStats getData(
      @NotNull String accountId, List<QLCCMAggregationFunction> aggregateFunction, List<QLBillingDataFilter> filters) {
    QLTrendStatsCostData billingAmountData =
        billingDataHelper.getBillingAmountData(accountId, aggregateFunction, filters);
    if (billingAmountData != null && billingAmountData.getTotalCostData() != null) {
      BigDecimal totalBillingAmount = billingAmountData.getTotalCostData().getCost();
      BigDecimal unallocatedCost = null;
      if (isUnallocatedCostRequired(aggregateFunction) && billingAmountData.getUnallocatedCostData() != null) {
        unallocatedCost = billingAmountData.getUnallocatedCostData().getCost();
      }
      return QLBillingTrendStats.builder()
          .totalCost(getTotalBillingStats(billingAmountData.getTotalCostData(), filters))
          .costTrend(getBillingTrend(accountId, totalBillingAmount, aggregateFunction, filters))
          .idleCost(getIdleCostStats(billingAmountData.getIdleCostData(), billingAmountData.getTotalCostData()))
          .unallocatedCost(getUnallocatedCostStats(unallocatedCost, billingAmountData.getTotalCostData()))
          .utilizedCost(getUtilizedCostStats(billingAmountData.getIdleCostData(), billingAmountData.getTotalCostData(),
              billingAmountData.getUnallocatedCostData()))
          .systemCost(getSystemCostStats(billingAmountData.getSystemCostData(), billingAmountData.getTotalCostData()))
          .efficiencyScore(getEfficiencyScore(accountId, billingAmountData, aggregateFunction, filters))
          .build();
    } else {
      return QLBillingTrendStats.builder().build();
    }
  }

  private QLBillingStatsInfo getEfficiencyScore(String accountId, QLTrendStatsCostData currentBillingAmount,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLBillingDataFilter> filters) {
    int currentEfficiencyScore = BillingStatsDefaultKeys.EFFICIENCY_SCORE;
    int previousEfficiencyScore;
    int efficiencyTrend = BillingStatsDefaultKeys.EFFICIENCY_SCORE_TREND;

    if (currentBillingAmount.getTotalCostData() != null && currentBillingAmount.getIdleCostData() != null
        && currentBillingAmount.getTotalCostData().getCost().compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal unallocatedCost;

      if (currentBillingAmount.getUnallocatedCostData() == null) {
        unallocatedCost = BigDecimal.ZERO;
      } else {
        unallocatedCost = currentBillingAmount.getUnallocatedCostData().getCost();
      }
      BigDecimal utilized = currentBillingAmount.getTotalCostData()
                                .getCost()
                                .subtract(currentBillingAmount.getIdleCostData().getCost())
                                .subtract(unallocatedCost);
      QLStatsBreakdownInfo currentCostStats = QLStatsBreakdownInfo.builder()
                                                  .idle(currentBillingAmount.getIdleCostData().getCost())
                                                  .total(currentBillingAmount.getTotalCostData().getCost())
                                                  .unallocated(unallocatedCost)
                                                  .utilized(utilized)
                                                  .build();

      currentEfficiencyScore = billingDataHelper.calculateEfficiencyScore(currentCostStats);
    }

    List<QLBillingDataFilter> trendFilters = billingDataHelper.getTrendFilter(
        filters, billingDataHelper.getStartInstant(filters), billingDataHelper.getEndInstant(filters));
    QLTrendStatsCostData prevBillingAmountData =
        billingDataHelper.getBillingAmountData(accountId, aggregateFunction, trendFilters);

    if (prevBillingAmountData != null && prevBillingAmountData.getTotalCostData() != null
        && prevBillingAmountData.getIdleCostData() != null
        && prevBillingAmountData.getTotalCostData().getCost().compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal unallocatedCost;

      if (prevBillingAmountData.getUnallocatedCostData() == null) {
        unallocatedCost = BigDecimal.ZERO;
      } else {
        unallocatedCost = prevBillingAmountData.getUnallocatedCostData().getCost();
      }
      BigDecimal utilized = prevBillingAmountData.getTotalCostData()
                                .getCost()
                                .subtract(prevBillingAmountData.getIdleCostData().getCost())
                                .subtract(unallocatedCost);

      QLStatsBreakdownInfo previousCostStats = QLStatsBreakdownInfo.builder()
                                                   .idle(prevBillingAmountData.getIdleCostData().getCost())
                                                   .total(prevBillingAmountData.getTotalCostData().getCost())
                                                   .unallocated(unallocatedCost)
                                                   .utilized(utilized)
                                                   .build();

      previousEfficiencyScore = billingDataHelper.calculateEfficiencyScore(previousCostStats);
      if (previousEfficiencyScore > 0) {
        efficiencyTrend = billingDataHelper
                              .calculateTrendPercentage(BigDecimal.valueOf(currentEfficiencyScore),
                                  BigDecimal.valueOf(previousEfficiencyScore))
                              .intValue();
      }
    }
    return QLBillingStatsInfo.builder()
        .statsLabel(EFFICIENCY_SCORE_LABEL)
        .statsDescription(null)
        .statsValue(String.valueOf(currentEfficiencyScore))
        .statsTrend(efficiencyTrend)
        .build();
  }

  private QLBillingStatsInfo getTotalBillingStats(
      QLBillingAmountData billingAmountData, List<QLBillingDataFilter> filters) {
    Instant startInstant = Instant.ofEpochMilli(billingDataHelper.getStartTimeFilter(filters).getValue().longValue());
    Instant endInstant = Instant.ofEpochMilli(billingAmountData.getMaxStartTime());
    boolean isYearRequired = billingDataHelper.isYearRequired(startInstant, endInstant);
    String startInstantFormat = billingDataHelper.getTotalCostFormattedDate(startInstant, isYearRequired);
    String endInstantFormat = billingDataHelper.getTotalCostFormattedDate(endInstant, isYearRequired);
    String totalCostDescription = String.format(TOTAL_COST_DESCRIPTION, startInstantFormat, endInstantFormat);
    String totalCostValue = String.format(
        TOTAL_COST_VALUE, billingDataHelper.formatNumber(getRoundedDoubleValue(billingAmountData.getCost())));
    return QLBillingStatsInfo.builder()
        .statsLabel(TOTAL_COST_LABEL)
        .statsDescription(totalCostDescription)
        .statsValue(totalCostValue)
        .build();
  }

  private QLBillingStatsInfo getIdleCostStats(QLBillingAmountData idleCostData, QLBillingAmountData totalCostData) {
    String idleCostDescription = EMPTY_VALUE;
    String idleCostValue = EMPTY_VALUE;
    if (idleCostData != null) {
      idleCostValue = String.format(
          COST_VALUE, billingDataHelper.formatNumber(billingDataHelper.getRoundedDoubleValue(idleCostData.getCost())));
      if (totalCostData != null && totalCostData.getCost().doubleValue() != 0) {
        double percentageOfTotalCost = billingDataHelper.getRoundedDoublePercentageValue(
            BigDecimal.valueOf(idleCostData.getCost().doubleValue() / totalCostData.getCost().doubleValue()));
        idleCostDescription = String.format(IDLE_COST_DESCRIPTION, percentageOfTotalCost + "%",
            billingDataHelper.getRoundedDoubleValue(totalCostData.getCost()));
      }
    }
    return QLBillingStatsInfo.builder()
        .statsLabel(IDLE_COST_LABEL)
        .statsDescription(idleCostDescription)
        .statsValue(idleCostValue)
        .build();
  }

  private QLBillingStatsInfo getSystemCostStats(QLBillingAmountData systemCostData, QLBillingAmountData totalCostData) {
    String systemCostDescription = EMPTY_VALUE;
    String systemCostValue = EMPTY_VALUE;
    if (systemCostData != null) {
      systemCostValue = String.format(COST_VALUE,
          billingDataHelper.formatNumber(billingDataHelper.getRoundedDoubleValue(systemCostData.getCost())));
      if (totalCostData != null && totalCostData.getCost().doubleValue() != 0) {
        double percentageOfTotalCost = billingDataHelper.getRoundedDoublePercentageValue(
            BigDecimal.valueOf(systemCostData.getCost().doubleValue() / totalCostData.getCost().doubleValue()));
        systemCostDescription = String.format(SYSTEM_COST_DESCRIPTION, percentageOfTotalCost + "%",
            billingDataHelper.getRoundedDoubleValue(totalCostData.getCost()));
      }
    }
    return QLBillingStatsInfo.builder()
        .statsLabel(SYSTEM_COST_LABEL)
        .statsDescription(systemCostDescription)
        .statsValue(systemCostValue)
        .build();
  }

  private QLBillingStatsInfo getUnallocatedCostStats(BigDecimal unallocatedCost, QLBillingAmountData totalCostData) {
    String unallocatedCostDescription = EMPTY_VALUE;
    String unallocatedCostValue = EMPTY_VALUE;
    if (unallocatedCost != null) {
      unallocatedCostValue = String.format(
          COST_VALUE, billingDataHelper.formatNumber(billingDataHelper.getRoundedDoubleValue(unallocatedCost)));
      if (totalCostData != null && totalCostData.getCost().doubleValue() != 0) {
        double percentageOfTotalCost = billingDataHelper.getRoundedDoublePercentageValue(
            BigDecimal.valueOf(unallocatedCost.doubleValue() / totalCostData.getCost().doubleValue()));
        unallocatedCostDescription = String.format(UNALLOCATED_COST_DESCRIPTION, percentageOfTotalCost + "%",
            billingDataHelper.getRoundedDoubleValue(totalCostData.getCost()));
      }
    }
    return QLBillingStatsInfo.builder()
        .statsLabel(UNALLOCATED_COST_LABEL)
        .statsDescription(unallocatedCostDescription)
        .statsValue(unallocatedCostValue)
        .build();
  }

  private QLBillingStatsInfo getUtilizedCostStats(
      QLBillingAmountData idleCostData, QLBillingAmountData totalCostData, QLBillingAmountData unallocatedCostData) {
    String utilizedCostDescription = EMPTY_VALUE;
    String utilizedCostValue = EMPTY_VALUE;
    if (idleCostData != null && totalCostData != null) {
      double utilizedCost = totalCostData.getCost().doubleValue() - idleCostData.getCost().doubleValue();
      if (unallocatedCostData != null) {
        utilizedCost -= unallocatedCostData.getCost().doubleValue();
      }
      utilizedCostValue = String.format(
          COST_VALUE, billingDataHelper.formatNumber(billingDataHelper.getRoundedDoubleValue(utilizedCost)));
      if (totalCostData.getCost().doubleValue() != 0) {
        double percentageOfTotalCost = billingDataHelper.getRoundedDoublePercentageValue(
            BigDecimal.valueOf(utilizedCost / totalCostData.getCost().doubleValue()));
        utilizedCostDescription = String.format(UTILIZED_COST_DESCRIPTION, percentageOfTotalCost + "%",
            billingDataHelper.getRoundedDoubleValue(totalCostData.getCost()));
      }
    }
    return QLBillingStatsInfo.builder()
        .statsLabel(UTILIZED_COST_LABEL)
        .statsDescription(utilizedCostDescription)
        .statsValue(utilizedCostValue)
        .build();
  }
  private QLBillingStatsInfo getBillingTrend(String accountId, BigDecimal totalBillingAmount,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLBillingDataFilter> filters) {
    List<QLBillingDataFilter> trendFilters = billingDataHelper.getTrendFilter(
        filters, billingDataHelper.getStartInstant(filters), billingDataHelper.getEndInstant(filters));
    QLBillingAmountData prevBillingAmountData =
        billingDataHelper.getBillingAmountData(accountId, aggregateFunction, trendFilters).getTotalCostData();
    Instant filterStartTime =
        Instant.ofEpochMilli(billingDataHelper.getStartTimeFilter(trendFilters).getValue().longValue());
    Instant endInstant = Instant.ofEpochMilli(billingDataHelper.getEndTimeFilter(trendFilters).getValue().longValue());
    boolean isYearRequired = billingDataHelper.isYearRequired(filterStartTime, endInstant);
    String trendCostDescription =
        String.format(TREND_COST_DESCRIPTION, billingDataHelper.formatNumber(getRoundedDoubleValue(totalBillingAmount)),
            billingDataHelper.getTotalCostFormattedDate(filterStartTime, isYearRequired),
            billingDataHelper.getTotalCostFormattedDate(endInstant, isYearRequired));
    String trendCostValue = NA_VALUE;
    if (prevBillingAmountData != null && prevBillingAmountData.getCost().compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal prevTotalBillingAmount = prevBillingAmountData.getCost();
      Instant startInstant = Instant.ofEpochMilli(prevBillingAmountData.getMinStartTime());
      isYearRequired = billingDataHelper.isYearRequired(startInstant, endInstant);
      String startInstantFormat = billingDataHelper.getTotalCostFormattedDate(startInstant, isYearRequired);
      String endInstantFormat = billingDataHelper.getTotalCostFormattedDate(endInstant, isYearRequired);
      BigDecimal amountDifference = totalBillingAmount.subtract(prevTotalBillingAmount);
      trendCostDescription = String.format(TREND_COST_DESCRIPTION, Math.abs(getRoundedDoubleValue(amountDifference)),
          startInstantFormat, endInstantFormat);
      if (amountDifference.compareTo(BigDecimal.ZERO) < 0) {
        trendCostDescription = EMPTY_VALUE.concat(trendCostDescription);
      }
      if (filterStartTime.plus(1, ChronoUnit.DAYS).isAfter(startInstant)) {
        BigDecimal trendPercentage =
            amountDifference.multiply(BigDecimal.valueOf(100)).divide(prevTotalBillingAmount, 2, RoundingMode.HALF_UP);
        trendCostValue = String.format(TREND_COST_VALUE, getRoundedDoubleValue(trendPercentage));
      }
    }
    return QLBillingStatsInfo.builder()
        .statsLabel(TREND_COST_LABEL)
        .statsDescription(trendCostDescription)
        .statsValue(trendCostValue)
        .build();
  }

  private boolean isUnallocatedCostRequired(List<QLCCMAggregationFunction> aggregateFunctions) {
    for (QLCCMAggregationFunction aggregationFunction : aggregateFunctions) {
      if (aggregationFunction.getColumnName().equalsIgnoreCase("UNALLOCATEDCOST")) {
        return true;
      }
    }
    return false;
  }

  private double getRoundedDoubleValue(BigDecimal value) {
    return Math.round(value.doubleValue() * 100D) / 100D;
  }

  @Override
  protected QLData postFetch(String accountId, List<QLCCMGroupBy> groupByList,
      List<QLCCMAggregationFunction> aggregations, List<QLBillingSortCriteria> sort, QLData qlData) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }

  @Override
  public boolean isCESampleAccountIdAllowed() {
    return true;
  }
}
