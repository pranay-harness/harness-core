package io.harness.ccm.views.service.impl;

import com.google.cloud.bigquery.BigQuery;
import com.google.inject.Inject;

import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.graphql.QLCESortOrder;
import io.harness.ccm.views.graphql.QLCEViewAggregateOperation;
import io.harness.ccm.views.graphql.QLCEViewAggregation;
import io.harness.ccm.views.graphql.QLCEViewEntityStatsDataPoint;
import io.harness.ccm.views.graphql.QLCEViewFieldInput;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;
import io.harness.ccm.views.graphql.QLCEViewMetadataFilter;
import io.harness.ccm.views.graphql.QLCEViewSortCriteria;
import io.harness.ccm.views.graphql.QLCEViewSortType;
import io.harness.ccm.views.graphql.QLCEViewTimeFilter;
import io.harness.ccm.views.graphql.QLCEViewTimeFilterOperator;
import io.harness.ccm.views.graphql.QLCEViewTrendInfo;
import io.harness.ccm.views.service.CEReportTemplateBuilderService;
import io.harness.ccm.views.service.CEViewService;
import io.harness.exception.InvalidRequestException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class CEReportTemplateBuilderServiceImpl implements CEReportTemplateBuilderService {
  @Inject CEViewService ceViewService;
  @Inject ViewsBillingServiceImpl viewsBillingService;

  // For table construction
  private static final String TABLE_START =
      "<table style=\"border-collapse: collapse;width: 80%;font-family: arial, sans-serif;margin-left: 7%; min-width: 500px;max-width: 500px;\">";
  private static final String TABLE_END = "</table>";
  private static final String COLUMN1_HEADER =
      "<th style=\"border: 1px solid #dddddd;text-align: left;padding: 8px;background-color: #dddddd;\">%s</th>";
  private static final String COLUMN2_HEADER =
      "<th style=\"border: 1px solid #dddddd;text-align: left;padding: 8px;background-color: #dddddd;\">Total Cost</th>";
  private static final String COLUMN3_HEADER =
      "<th style=\"border: 1px solid #dddddd;text-align: left;padding: 8px;background-color: #dddddd;\">Cost Trend</th>";
  private static final String COLUMN = "<td style=\"border: 1px solid #dddddd;text-align: left;padding: 8px;\">%s</td>";
  private static final String MIDDLE_COLUMN =
      "<td style=\"border: 1px solid #dddddd;text-align: left;padding: 8px;\">%s</td>";
  private static final String ROW_START = "<tr>";
  private static final String ROW_END = "</tr>";

  // Template keys
  private static final String VIEW_NAME = "VIEW_NAME";
  private static final String DATE = "DATE";
  private static final String TOTAL_COST = "TOTAL_COST";
  private static final String TOTAL_COST_TREND = "TOTAL_COST_TREND";
  private static final String PERIOD = "PERIOD";
  private static final String TOTAL_COST_LAST_WEEK = "TOTAL_COST_LAST_WEEK";
  private static final String NUMBER_OF_ROWS = "NUMBER_OF_ROWS";
  private static final String ENTITY = "ENTITY";
  private static final String TABLE = "TABLE";

  // Constants
  private static final String ZERO = "0";
  private static final String WEEK = "WEEK";
  private static final String DAY = "DAY";
  private static final String MONTH = "MONTH";
  private static final long DAY_IN_MILLISECONDS = 86400000L;
  private static final String DEFAULT_TIMEZONE = "GMT";
  private static final String DATE_PATTERN = "MM/dd";
  private static final String COST_TREND = "( %s | %s )";

  @Override
  public Map<String, String> getTemplatePlaceholders(
      String accountId, String viewId, BigQuery bigQuery, String cloudProviderTableName) {
    return getTemplatePlaceholders(accountId, viewId, null, bigQuery, cloudProviderTableName);
  }

  @Override
  public Map<String, String> getTemplatePlaceholders(
      String accountId, String viewId, String reportId, BigQuery bigQuery, String cloudProviderTableName) {
    Map<String, String> templatePlaceholders = new HashMap<>();
    // Get cloud provider table name here
    CEView view = ceViewService.get(viewId);
    if (view == null) {
      throw new InvalidRequestException("Exception while generating report. View doesn't exist.");
    }
    String entity = getEntity(view);
    String period = getPeriod(reportId);
    long startOfDay = getStartOfDayTimestamp(1);
    List<QLCEViewAggregation> aggregationFunction = getTotalCostAggregation();
    List<QLCEViewSortCriteria> sortCriteria = getSortCriteria();
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(getViewFilter(viewId));
    filters.add(getTimeFilter(
        getStartTime(startOfDay, getTimePeriod(period)).toEpochMilli(), QLCEViewTimeFilterOperator.AFTER));
    filters.add(getTimeFilter(getEndTime(startOfDay).toEpochMilli(), QLCEViewTimeFilterOperator.BEFORE));
    List<QLCEViewGroupBy> groupBy = new ArrayList<>();

    // Generating Trend data
    QLCEViewTrendInfo trendData =
        viewsBillingService.getTrendStatsData(bigQuery, filters, aggregationFunction, cloudProviderTableName);

    // Generating table data
    List<QLCEViewEntityStatsDataPoint> tableData = viewsBillingService.getEntityStatsDataPoints(
        bigQuery, filters, groupBy, aggregationFunction, sortCriteria, cloudProviderTableName);

    // Filling template placeholders
    templatePlaceholders.put(VIEW_NAME, view.getName());
    templatePlaceholders.put(ENTITY, entity);
    templatePlaceholders.put(PERIOD, period);
    templatePlaceholders.put(DATE, getReportDateRange(period));

    if (trendData == null) {
      throw new InvalidRequestException("Exception while generating report. No data to for cost trend");
    }
    templatePlaceholders.put(TOTAL_COST, trendData.getStatsValue());
    templatePlaceholders.put(
        TOTAL_COST_TREND, String.format(COST_TREND, trendData.getStatsTrend().toString(), getTotalCostDiff(trendData)));
    templatePlaceholders.put(TOTAL_COST_LAST_WEEK, String.valueOf(getTotalCostLastWeek(trendData)));

    String numberOfRows = getNumberOfRows(tableData);
    if (numberOfRows.equals(ZERO)) {
      throw new InvalidRequestException("Exception while generating report. No data to show in table");
    }
    templatePlaceholders.put(NUMBER_OF_ROWS, numberOfRows);
    templatePlaceholders.put(TABLE, generateTable(tableData, entity));

    return templatePlaceholders;
  }

  private List<QLCEViewAggregation> getTotalCostAggregation() {
    return Collections.singletonList(
        QLCEViewAggregation.builder().columnName("cost").operationType(QLCEViewAggregateOperation.SUM).build());
  }

  private List<QLCEViewSortCriteria> getSortCriteria() {
    return Collections.singletonList(
        QLCEViewSortCriteria.builder().sortOrder(QLCESortOrder.DESCENDING).sortType(QLCEViewSortType.COST).build());
  }

  private QLCEViewFilterWrapper getViewFilter(String viewId) {
    return QLCEViewFilterWrapper.builder()
        .viewMetadataFilter(QLCEViewMetadataFilter.builder().viewId(viewId).isPreview(false).build())
        .build();
  }

  private QLCEViewFilterWrapper getTimeFilter(long timestamp, QLCEViewTimeFilterOperator operator) {
    return QLCEViewFilterWrapper.builder()
        .timeFilter(QLCEViewTimeFilter.builder()
                        .field(QLCEViewFieldInput.builder()
                                   .fieldId("startTime")
                                   .fieldName("startTime")
                                   .identifier(ViewFieldIdentifier.COMMON)
                                   .build())
                        .operator(operator)
                        .value(timestamp)
                        .build())
        .build();
  }

  private String getEntity(CEView view) {
    return view.getViewVisualization().getGroupBy().getFieldName();
  }

  // Todo: Update this when time filters from view are used
  private String getPeriod(String reportId) {
    return WEEK;
  }

  private String getTotalCostDiff(QLCEViewTrendInfo trendInfo) {
    return String.valueOf(
        Math.round(trendInfo.getValue().doubleValue() - getTotalCostLastWeek(trendInfo) * 100D) / 100D);
  }

  private double getTotalCostLastWeek(QLCEViewTrendInfo trendInfo) {
    return Math.round(
               (100 * trendInfo.getValue().doubleValue() / (100 + trendInfo.getStatsTrend().doubleValue())) * 100D)
        / 100D;
  }

  private String generateTable(List<QLCEViewEntityStatsDataPoint> tableData, String entity) {
    String table = "";
    if (tableData != null && !tableData.isEmpty()) {
      StringJoiner joiner = new StringJoiner(" ");
      joiner.add(TABLE_START);
      joiner.add(ROW_START);
      joiner.add(String.format(COLUMN1_HEADER, entity));
      joiner.add(COLUMN2_HEADER);
      joiner.add(COLUMN3_HEADER);
      joiner.add(ROW_END);

      tableData.forEach(data -> {
        joiner.add(ROW_START);
        joiner.add(String.format(COLUMN, data.getName()));
        joiner.add(String.format(MIDDLE_COLUMN, data.getCost()));
        joiner.add(String.format(COLUMN, data.getCostTrend()));
        joiner.add(ROW_END);
      });

      joiner.add(TABLE_END);
      table = joiner.toString();
    }
    return table;
  }

  private String getNumberOfRows(List<QLCEViewEntityStatsDataPoint> tableData) {
    String numberOfRows = ZERO;
    if (tableData != null) {
      numberOfRows = String.valueOf(tableData.size());
    }
    return numberOfRows;
  }

  private String getReportDateRange(String period) {
    long timePeriod = getTimePeriod(period);
    Instant startInstant = getStartTime(getStartOfDayTimestamp(0), timePeriod);
    Instant endInstant = getEndTime(getStartOfDayTimestamp(0));
    return startInstant.atZone(ZoneId.of(DEFAULT_TIMEZONE)).format(DateTimeFormatter.ofPattern(DATE_PATTERN)) + " to "
        + endInstant.atZone(ZoneId.of(DEFAULT_TIMEZONE)).format(DateTimeFormatter.ofPattern(DATE_PATTERN));
  }

  private long getTimePeriod(String period) {
    switch (period) {
      case DAY:
        return DAY_IN_MILLISECONDS;
      case WEEK:
      default:
        return 7 * DAY_IN_MILLISECONDS;
    }
  }

  private Instant getStartTime(long timestamp, long period) {
    return Instant.ofEpochMilli(timestamp - period);
  }

  private Instant getEndTime(long timestamp) {
    return Instant.ofEpochMilli(timestamp - 1000);
  }

  private long getStartOfDayTimestamp(long offset) {
    ZoneId zoneId = ZoneId.of(DEFAULT_TIMEZONE);
    LocalDate today = LocalDate.now(zoneId);
    ZonedDateTime zdtStart = today.atStartOfDay(zoneId);
    return (zdtStart.toEpochSecond() * 1000) - offset;
  }
}
