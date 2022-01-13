/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.cloudefficiencyevents;

import static io.harness.annotations.dev.HarnessTeam.CE;

import static software.wings.graphql.datafetcher.cloudefficiencyevents.CEEventsQueryMetaData.CEEventsMetaDataFields.BILLINGAMOUNT;
import static software.wings.graphql.datafetcher.cloudefficiencyevents.CEEventsQueryMetaData.CEEventsMetaDataFields.COST_CHANGE_PERCENT;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationListAndLimit;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.datafetcher.billing.QLBillingStatsHelper;
import software.wings.graphql.datafetcher.billing.QLCCMAggregationFunction;
import software.wings.graphql.datafetcher.cloudefficiencyevents.QLEventsDataPoint.QLEventsDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.service.intfc.ce.CeAccountExpirationChecker;

import com.google.inject.Inject;
import graphql.schema.DataFetchingEnvironment;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class EventsStatsDataFetcher
    extends AbstractStatsDataFetcherWithAggregationListAndLimit<QLCCMAggregationFunction, QLEventsDataFilter,
        QLCCMGroupBy, QLEventsSortCriteria> {
  @Inject QLBillingStatsHelper statsHelper;
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject private EventsDataQueryBuilder eventsDataQueryBuilder;
  @Inject CeAccountExpirationChecker accountChecker;
  private static String offsetAndLimitQuery = " OFFSET %s LIMIT %s";

  @Override
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLEventsDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLEventsSortCriteria> sort, Integer limit,
      Integer offset) {
    accountChecker.checkIsCeEnabled(accountId);
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
    log.info("EventsStatsDataFetcher query!! {}", queryData.getQuery());

    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(queryData.getQuery());
      return generateEventsData(queryData, resultSet);
    } catch (SQLException e) {
      log.error("EventsStatsDataFetcher Error exception", e);
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
          case NAMESPACE:
            eventDataBuilder.namespace(resultSet.getString(field.getFieldName()));
            break;
          case WORKLOADNAME:
            eventDataBuilder.workloadName(resultSet.getString(field.getFieldName()));
            break;
          case CLUSTERID:
            String clusterId = resultSet.getString(field.getFieldName());
            eventDataBuilder.clusterId(clusterId);
            eventDataBuilder.clusterName(statsHelper.getEntityName(BillingDataMetaDataFields.CLUSTERID, clusterId));
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

  @Override
  protected QLData fetchSelectedFields(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLEventsDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLEventsSortCriteria> sort, Integer limit,
      Integer offset, boolean skipRoundOff, DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
  }

  @Override
  public boolean isCESampleAccountIdAllowed() {
    return true;
  }
}
