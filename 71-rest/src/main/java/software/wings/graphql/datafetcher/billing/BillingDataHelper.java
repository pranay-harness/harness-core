package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import com.hazelcast.util.Preconditions;
import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMTimeSeriesAggregation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Slf4j
public class BillingDataHelper {
  @Inject protected DataFetcherUtils utils;
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject private BillingDataQueryBuilder billingDataQueryBuilder;
  private static final String TOTAL_COST_DATE_PATTERN = "dd MMMM, yyyy";
  private static final String DEFAULT_TIME_ZONE = "GMT";
  private static final long ONE_DAY_MILLIS = 86400000;

  protected double roundingDoubleFieldValue(BillingDataMetaDataFields field, ResultSet resultSet) throws SQLException {
    return Math.round(resultSet.getDouble(field.getFieldName()) * 100D) / 100D;
  }

  protected double roundingDoubleFieldPercentageValue(BillingDataMetaDataFields field, ResultSet resultSet)
      throws SQLException {
    return Math.round(resultSet.getDouble(field.getFieldName()) * 10000D) / 100D;
  }

  protected String getFormattedDate(Instant instant, String datePattern) {
    return instant.atZone(ZoneId.of(DEFAULT_TIME_ZONE)).format(DateTimeFormatter.ofPattern(datePattern));
  }

  protected double getRoundedDoubleValue(BigDecimal value) {
    return Math.round(value.doubleValue() * 100D) / 100D;
  }

  public double getRoundedDoubleValue(double value) {
    return Math.round(value * 100D) / 100D;
  }

  protected double getRoundedDoublePercentageValue(BigDecimal value) {
    return Math.round(value.doubleValue() * 10000D) / 100D;
  }

  protected QLTimeFilter getStartTimeFilter(List<QLBillingDataFilter> filters) {
    Optional<QLBillingDataFilter> startTimeDataFilter =
        filters.stream().filter(qlBillingDataFilter -> qlBillingDataFilter.getStartTime() != null).findFirst();
    if (startTimeDataFilter.isPresent()) {
      return startTimeDataFilter.get().getStartTime();
    } else {
      throw new InvalidRequestException("Start time cannot be null");
    }
  }

  protected QLTimeFilter getEndTimeFilter(List<QLBillingDataFilter> filters) {
    Optional<QLBillingDataFilter> endTimeDataFilter =
        filters.stream().filter(qlBillingDataFilter -> qlBillingDataFilter.getEndTime() != null).findFirst();
    if (endTimeDataFilter.isPresent()) {
      return endTimeDataFilter.get().getEndTime();
    } else {
      throw new InvalidRequestException("End time cannot be null");
    }
  }

  public String getTotalCostFormattedDate(Instant instant) {
    return getFormattedDate(instant, TOTAL_COST_DATE_PATTERN);
  }

  protected List<QLBillingDataFilter> getTrendFilter(
      List<QLBillingDataFilter> filters, Instant startInstant, Instant endInstant) {
    long diffMillis = endInstant.truncatedTo(ChronoUnit.DAYS).toEpochMilli()
        - startInstant.truncatedTo(ChronoUnit.DAYS).toEpochMilli();
    long trendEndTime = startInstant.toEpochMilli() - ONE_DAY_MILLIS;
    long trendStartTime = trendEndTime - diffMillis;
    return updateTimeFilter(filters, trendStartTime, trendEndTime);
  }

  private List<QLBillingDataFilter> updateTimeFilter(List<QLBillingDataFilter> filters, long startTime, long endTime) {
    List<QLBillingDataFilter> entityFilters =
        filters.stream()
            .filter(qlBillingDataFilter
                -> null == qlBillingDataFilter.getStartTime() && null == qlBillingDataFilter.getEndTime())
            .collect(Collectors.toList());
    entityFilters.add(QLBillingDataFilter.builder()
                          .startTime(QLTimeFilter.builder().value(startTime).operator(QLTimeOperator.AFTER).build())
                          .build());
    entityFilters.add(QLBillingDataFilter.builder()
                          .endTime(QLTimeFilter.builder().value(endTime).operator(QLTimeOperator.BEFORE).build())
                          .build());
    return entityFilters;
  }

  protected Double getCostTrendForEntity(ResultSet resultSet, QLBillingAmountData prevBillingAmountData,
      List<QLBillingDataFilter> filters) throws SQLException {
    QLBillingAmountData billingAmountData =
        QLBillingAmountData.builder()
            .cost(resultSet.getBigDecimal(BillingDataMetaDataFields.SUM.getFieldName()))
            .minStartTime(
                resultSet
                    .getTimestamp(BillingDataMetaDataFields.MIN_STARTTIME.getFieldName(), utils.getDefaultCalendar())
                    .getTime())
            .maxStartTime(
                resultSet
                    .getTimestamp(BillingDataMetaDataFields.MAX_STARTTIME.getFieldName(), utils.getDefaultCalendar())
                    .getTime())
            .build();
    BigDecimal forecastCost = getForecastCost(billingAmountData, getEndInstant(filters));

    List<QLBillingDataFilter> trendFilters = getTrendFilter(filters, getStartInstant(filters), getEndInstant(filters));
    Instant filterStartTime = getStartInstant(trendFilters);
    Double trendPercent = 0.0;
    if (prevBillingAmountData != null && prevBillingAmountData.getCost().compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal prevTotalBillingAmount = prevBillingAmountData.getCost();
      Instant startInstant = Instant.ofEpochMilli(prevBillingAmountData.getMinStartTime());
      BigDecimal amountDifference = billingAmountData.getCost().subtract(prevTotalBillingAmount);
      if (null != forecastCost) {
        amountDifference = forecastCost.subtract(prevTotalBillingAmount);
      }

      if (filterStartTime.plus(1, ChronoUnit.DAYS).isAfter(startInstant)) {
        BigDecimal trendPercentage =
            amountDifference.multiply(BigDecimal.valueOf(100)).divide(prevTotalBillingAmount, 2, RoundingMode.HALF_UP);
        trendPercent = getRoundedDoubleValue(trendPercentage);
      }
    }
    return trendPercent;
  }

  protected Map<String, QLBillingAmountData> getEntityIdToBillingAmountData(
      ResultSet resultSet, List<BillingDataMetaDataFields> listOfFields) throws SQLException {
    Map<String, QLBillingAmountData> entityIdToBillingAmountData = new HashMap<>();
    while (resultSet != null && resultSet.next()) {
      StringJoiner entityIdAppender = new StringJoiner(":");
      for (BillingDataMetaDataFields field : listOfFields) {
        if (resultSet.getString(field.getFieldName()) != null) {
          entityIdAppender.add(resultSet.getString(field.getFieldName()));
        }
      }
      String entityId = entityIdAppender.toString();
      QLBillingAmountData billingAmountData = null;
      if (resultSet.getBigDecimal(BillingDataMetaDataFields.SUM.getFieldName()) != null) {
        billingAmountData = QLBillingAmountData.builder()
                                .cost(resultSet.getBigDecimal(BillingDataMetaDataFields.SUM.getFieldName()))
                                .minStartTime(resultSet
                                                  .getTimestamp(BillingDataMetaDataFields.MIN_STARTTIME.getFieldName(),
                                                      utils.getDefaultCalendar())
                                                  .getTime())
                                .maxStartTime(resultSet
                                                  .getTimestamp(BillingDataMetaDataFields.MAX_STARTTIME.getFieldName(),
                                                      utils.getDefaultCalendar())
                                                  .getTime())
                                .build();
      }
      entityIdToBillingAmountData.put(entityId, billingAmountData);
    }
    return entityIdToBillingAmountData;
  }

  public BigDecimal getForecastCost(QLBillingAmountData billingAmountData, Instant endInstant) {
    Preconditions.checkNotNull(billingAmountData);
    Instant currentTime = Instant.now();
    if (currentTime.isAfter(endInstant)) {
      return null;
    }

    long billingTimeDiffMillis = ONE_DAY_MILLIS;
    if (billingAmountData.getMaxStartTime() != billingAmountData.getMinStartTime()) {
      billingTimeDiffMillis = billingAmountData.getMaxStartTime() - billingAmountData.getMinStartTime();
    }

    BigDecimal totalBillingAmount = billingAmountData.getCost();
    long actualTimeDiffMillis = endInstant.toEpochMilli() - billingAmountData.getMinStartTime();
    return totalBillingAmount.multiply(
        new BigDecimal(actualTimeDiffMillis).divide(new BigDecimal(billingTimeDiffMillis), 2, RoundingMode.HALF_UP));
  }

  public Instant getEndInstant(List<QLBillingDataFilter> filters) {
    return Instant.ofEpochMilli(getEndTimeFilter(filters).getValue().longValue());
  }

  public Instant getStartInstant(List<QLBillingDataFilter> filters) {
    return Instant.ofEpochMilli(getStartTimeFilter(filters).getValue().longValue());
  }

  protected Map<String, QLBillingAmountData> getBillingAmountDataForEntityCostTrend(String accountId,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLBillingDataFilter> filters,
      List<QLCCMEntityGroupBy> groupByEntityList, QLCCMTimeSeriesAggregation groupByTime,
      List<QLBillingSortCriteria> sortCriteria) {
    List<BillingDataMetaDataFields> entity = getEntityForCostTrendMapping(groupByEntityList);
    List<QLBillingDataFilter> trendFilters = getTrendFilter(filters, getStartInstant(filters), getEndInstant(filters));
    boolean addInstanceTypeFilter = setInstanceTypeBoolean(groupByEntityList);
    BillingDataQueryMetadata queryData = billingDataQueryBuilder.formQuery(accountId, trendFilters, aggregateFunction,
        groupByEntityList, groupByTime, sortCriteria, addInstanceTypeFilter, true);
    String query = queryData.getQuery();
    logger.info("Billing data query for cost trend {}", query);
    ResultSet resultSet = null;
    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(query);
      return getEntityIdToBillingAmountData(resultSet, entity);
    } catch (SQLException e) {
      logger.error("BillingDataHelper cost trend query Error exception", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return null;
  }

  private boolean setInstanceTypeBoolean(List<QLCCMEntityGroupBy> groupByEntityList) {
    for (QLCCMEntityGroupBy groupBy : groupByEntityList) {
      if (groupBy == QLCCMEntityGroupBy.WorkloadName || groupBy == QLCCMEntityGroupBy.Namespace
          || groupBy == QLCCMEntityGroupBy.TaskId || groupBy == QLCCMEntityGroupBy.CloudServiceName) {
        return false;
      }
    }
    return true;
  }

  protected List<BillingDataMetaDataFields> getEntityForCostTrendMapping(List<QLCCMEntityGroupBy> groupByEntityList) {
    List<BillingDataMetaDataFields> listOfFields = new ArrayList<>();
    for (QLCCMEntityGroupBy groupByEntity : groupByEntityList) {
      switch (groupByEntity) {
        case Cluster:
          listOfFields.add(BillingDataMetaDataFields.CLUSTERID);
          break;
        case Namespace:
          listOfFields.add(BillingDataMetaDataFields.NAMESPACE);
          break;
        case WorkloadName:
          listOfFields.add(BillingDataMetaDataFields.WORKLOADNAME);
          break;
        case Application:
          listOfFields.add(BillingDataMetaDataFields.APPID);
          break;
        case Service:
          listOfFields.add(BillingDataMetaDataFields.SERVICEID);
          break;
        case Environment:
          listOfFields.add(BillingDataMetaDataFields.ENVID);
          break;
        case CloudProvider:
          listOfFields.add(BillingDataMetaDataFields.CLOUDPROVIDERID);
          break;
        default:
          break;
      }
    }
    return listOfFields;
  }
}
