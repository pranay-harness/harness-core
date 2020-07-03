package software.wings.graphql.datafetcher.cloudefficiencyevents;

import static java.lang.String.format;
import static software.wings.graphql.datafetcher.cloudefficiencyevents.CEEventsQueryMetaData.CEEventsMetaDataFields.BILLINGAMOUNT;
import static software.wings.graphql.datafetcher.cloudefficiencyevents.CEEventsQueryMetaData.CEEventsMetaDataFields.COST_CHANGE_PERCENT;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationListAndLimit;
import software.wings.graphql.datafetcher.billing.QLCCMAggregationFunction;
import software.wings.graphql.datafetcher.cloudefficiencyevents.QLEventsDataPoint.QLEventsDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class EventsStatsDataFetcher
    extends AbstractStatsDataFetcherWithAggregationListAndLimit<QLCCMAggregationFunction, QLEventsDataFilter,
        QLCCMGroupBy, QLEventsSortCriteria> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject private EventsDataQueryBuilder eventsDataQueryBuilder;
  private static String offsetAndLimitQuery = " OFFSET %s LIMIT %s";

  @Override
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLEventsDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLEventsSortCriteria> sort, Integer limit,
      Integer offset) {
    try {
      if (timeScaleDBService.isValid()) {
        return getEventsData(accountId, filters, sort, limit, offset);
      } else {
        throw new InvalidRequestException("Cannot process request in EventsStatsDataFetcher");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching CE Events data {}", e);
    }
  }

  private QLEventData getEventsData(String accountId, List<QLEventsDataFilter> filters,
      List<QLEventsSortCriteria> sortCriteria, Integer limit, Integer offset) {
    CEEventsQueryMetaData queryData;
    ResultSet resultSet = null;
    queryData = eventsDataQueryBuilder.formQuery(accountId, filters, sortCriteria);
    if (isSortByBillingAmount(sortCriteria)) {
      queryData.setQuery(queryData.getQuery() + format(offsetAndLimitQuery, offset, limit));
    }
    logger.info("EventsStatsDataFetcher query!! {}", queryData.getQuery());

    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(queryData.getQuery());
      return generateEventsData(queryData, resultSet);
    } catch (SQLException e) {
      logger.error("EventsStatsDataFetcher Error exception", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return null;
  }

  private boolean isSortByBillingAmount(List<QLEventsSortCriteria> sortCriteria) {
    return sortCriteria.stream()
        .filter(qlBillingSortCriteria
            -> qlBillingSortCriteria.getSortType().getEventsMetaData().getFieldName().equals(
                BILLINGAMOUNT.getFieldName()))
        .findAny()
        .isPresent();
  }

  private QLEventData generateEventsData(CEEventsQueryMetaData queryData, ResultSet resultSet) throws SQLException {
    List<QLEventsDataPoint> dataPointList = new ArrayList<>();
    Map<Long, Integer> chartData = new HashMap<>();
    Map<Long, Integer> notableEventsChartData = new HashMap<>();
    Map<Long, Long> truncatedToFirstTimestampOfTheDayMap = new HashMap<>();
    while (null != resultSet && resultSet.next()) {
      QLEventsDataPointBuilder eventDataBuilder =
          QLEventsDataPoint.builder().eventPriorityType(EventPriorityType.normal.getFieldName());
      for (CEEventsQueryMetaData.CEEventsMetaDataFields field : queryData.getFieldNames()) {
        switch (field) {
          case STARTTIME:
            long timeStamp = resultSet.getTimestamp(field.getFieldName(), utils.getDefaultCalendar()).getTime();
            long truncatedTimestamp = timeStamp - (timeStamp % TimeUnit.DAYS.toMillis(1));
            eventDataBuilder.time(timeStamp);
            truncatedToFirstTimestampOfTheDayMap.putIfAbsent(truncatedTimestamp, timeStamp);
            chartData.put(truncatedToFirstTimestampOfTheDayMap.get(truncatedTimestamp),
                chartData.getOrDefault(truncatedToFirstTimestampOfTheDayMap.get(truncatedTimestamp), 0) + 1);

            if (resultSet.getDouble(COST_CHANGE_PERCENT.getFieldName()) != 0.0) {
              eventDataBuilder.eventPriorityType(EventPriorityType.notable.getFieldName());
              notableEventsChartData.put(truncatedToFirstTimestampOfTheDayMap.get(truncatedTimestamp),
                  notableEventsChartData.getOrDefault(truncatedToFirstTimestampOfTheDayMap.get(truncatedTimestamp), 0)
                      + 1);
            }
            break;
          case EVENTDESCRIPTION:
            eventDataBuilder.details(resultSet.getString(field.getFieldName()));
            break;
          case COSTEVENTTYPE:
            eventDataBuilder.type(resultSet.getString(field.getFieldName()));
            break;
          case COSTEVENTSOURCE:
            eventDataBuilder.source(resultSet.getString(field.getFieldName()));
            break;
          case OLDYAMLREF:
            eventDataBuilder.oldYamlRef(resultSet.getString(field.getFieldName()));
            break;
          case NEWYAMLREF:
            eventDataBuilder.newYamlRef(resultSet.getString(field.getFieldName()));
            break;
          case COST_CHANGE_PERCENT:
            eventDataBuilder.costChangePercentage(resultSet.getDouble(field.getFieldName()));
            break;
          default:
            break;
        }
      }
      dataPointList.add(eventDataBuilder.build());
    }

    List<QLChartDataPoint> chartDataPoints =
        chartData.entrySet()
            .stream()
            .map(entry -> QLChartDataPoint.builder().time(entry.getKey()).eventsCount(entry.getValue()).build())
            .collect(Collectors.toList());

    for (QLChartDataPoint chartDataPoint : chartDataPoints) {
      chartDataPoint.setNotableEventsCount(notableEventsChartData.getOrDefault(chartDataPoint.getTime(), 0));
    }

    return QLEventData.builder().data(dataPointList).chartData(chartDataPoints).build();
  }

  @Override
  public String getEntityType() {
    return null;
  }

  @Override
  protected QLData postFetch(String accountId, List<QLCCMGroupBy> groupByList,
      List<QLCCMAggregationFunction> aggregations, List<QLEventsSortCriteria> sort, QLData qlData, Integer limit,
      boolean includeOthers) {
    return null;
  }
}
