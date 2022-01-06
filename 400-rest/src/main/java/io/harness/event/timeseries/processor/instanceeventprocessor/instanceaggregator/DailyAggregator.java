/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.timeseries.processor.instanceeventprocessor.instanceaggregator;

import io.harness.event.timeseries.processor.EventProcessor;
import io.harness.event.timeseries.processor.instanceeventprocessor.InstanceEventAggregator;
import io.harness.event.timeseries.processor.utils.DateUtils;

import software.wings.service.impl.event.timeseries.TimeSeriesBatchEventInfo;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

public class DailyAggregator extends InstanceAggregator {
  private static final String FETCH_CHILD_DATA_POINTS_SQL =
      "SELECT PERCENTILE_DISC(0.50) WITHIN GROUP (ORDER BY INSTANCECOUNT) AS INSTANCECOUNT, BOOL_AND(SANITYSTATUS) AS SANITYSTATUS, COUNT(*) AS NUM_OF_RECORDS "
      + "FROM INSTANCE_STATS_HOUR "
      + "WHERE REPORTEDAT >= ? AND REPORTEDAT < ? "
      + "AND ACCOUNTID=? AND APPID=? AND SERVICEID=? AND ENVID=? AND CLOUDPROVIDERID=? AND INSTANCETYPE=?";

  private static final String UPSERT_PARENT_TABLE_SQL =
      "INSERT INTO INSTANCE_STATS_DAY (REPORTEDAT,ACCOUNTID,APPID,SERVICEID,ENVID,CLOUDPROVIDERID,INSTANCETYPE,INSTANCECOUNT,ARTIFACTID,WEEKTIMESTAMP,MONTHTIMESTAMP,SANITYSTATUS) "
      + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?) "
      + "ON CONFLICT(ACCOUNTID,APPID,SERVICEID,ENVID,CLOUDPROVIDERID,INSTANCETYPE,REPORTEDAT) "
      + "DO UPDATE SET INSTANCECOUNT=EXCLUDED.INSTANCECOUNT, SANITYSTATUS=EXCLUDED.SANITYSTATUS "
      + "WHERE INSTANCE_STATS_DAY.SANITYSTATUS=FALSE";

  public DailyAggregator(TimeSeriesBatchEventInfo eventInfo) {
    super(eventInfo, FETCH_CHILD_DATA_POINTS_SQL, UPSERT_PARENT_TABLE_SQL, 24, "DAILY AGGREGATOR");
  }

  @Override
  public Date getWindowBeginTimestamp() {
    Date windowEndTimestamp = getWindowEndTimestamp();
    return DateUtils.addDays(windowEndTimestamp.getTime(), -1);
  }

  @Override
  public Date getWindowEndTimestamp() {
    long eventTimestamp = this.getEventInfo().getTimestamp();
    return DateUtils.getNextNearestWholeDayUTC(eventTimestamp);
  }

  @Override
  public InstanceAggregator getParentAggregatorObj() {
    //    return new WeeklyAggregator(this.getEventInfo());
    return null;
  }

  @Override
  public void prepareUpsertQuery(PreparedStatement statement, Map<String, Object> params) throws SQLException {
    Map<String, Object> dataMap = (Map<String, Object>) params.get(InstanceEventAggregator.DATA_MAP_KEY);

    statement.setTimestamp(1, new Timestamp(this.getWindowBeginTimestamp().getTime()), DateUtils.getDefaultCalendar());
    statement.setString(2, this.getEventInfo().getAccountId());
    statement.setString(3, (String) dataMap.get(EventProcessor.APPID));
    statement.setString(4, (String) dataMap.get(EventProcessor.SERVICEID));
    statement.setString(5, (String) dataMap.get(EventProcessor.ENVID));
    statement.setString(6, (String) dataMap.get(EventProcessor.CLOUDPROVIDERID));
    statement.setString(7, (String) dataMap.get(EventProcessor.INSTANCETYPE));
    statement.setInt(8, (Integer) params.get(EventProcessor.INSTANCECOUNT));
    statement.setString(9, (String) dataMap.get(EventProcessor.ARTIFACTID));
    statement.setTimestamp(
        10, new Timestamp(this.getWeeklyWindowTimestamp().getTime()), DateUtils.getDefaultCalendar());
    statement.setTimestamp(
        11, new Timestamp(this.getMonthlyWindowTimestamp().getTime()), DateUtils.getDefaultCalendar());
    statement.setBoolean(12, (Boolean) params.get(EventProcessor.SANITYSTATUS));
  }

  // Calculate the month window to which event belongs (starting timestamp of the month)
  private Date getMonthlyWindowTimestamp() {
    long eventTimestamp = this.getEventInfo().getTimestamp();
    return DateUtils.getBeginningTimestampOfMonth(eventTimestamp);
  }

  // Calculate the week window to which event belongs (starting timestamp of the week)
  // Week starts from Sunday
  private Date getWeeklyWindowTimestamp() {
    long eventTimestamp = this.getEventInfo().getTimestamp();
    Date prevWholeDayTimestamp = DateUtils.getPrevNearestWholeDayUTC(eventTimestamp);

    Integer dayOfWeek = DateUtils.getDayOfWeek(prevWholeDayTimestamp.getTime());

    return DateUtils.addDays(prevWholeDayTimestamp.getTime(), 1 - dayOfWeek);
  }
}
