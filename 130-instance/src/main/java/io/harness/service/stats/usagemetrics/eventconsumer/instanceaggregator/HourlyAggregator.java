package io.harness.service.stats.usagemetrics.eventconsumer.instanceaggregator;

import io.harness.event.timeseries.processor.EventProcessor;
import io.harness.event.timeseries.processor.instanceeventprocessor.InstanceEventAggregator;
import io.harness.event.timeseries.processor.utils.DateUtils;
import io.harness.eventsframework.schemas.timeseriesevent.TimeseriesBatchEventInfo;
import io.harness.service.stats.Constants;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

public class HourlyAggregator extends InstanceAggregator {
  // TODO Fix queries by changing app id
  private static final String FETCH_CHILD_DATA_POINTS_SQL =
      "SELECT PERCENTILE_DISC(0.50) WITHIN GROUP (ORDER BY INSTANCECOUNT) AS INSTANCECOUNT, COUNT(*) AS NUM_OF_RECORDS "
      + "FROM INSTANCE_STATS "
      + "WHERE REPORTEDAT >= ? AND REPORTEDAT < ? "
      + "AND ACCOUNTID=? AND APPID=? AND SERVICEID=? AND ENVID=? AND CLOUDPROVIDERID=? AND INSTANCETYPE=?";

  private static final String UPSERT_PARENT_TABLE_SQL =
      "INSERT INTO INSTANCE_STATS_HOUR (REPORTEDAT, ACCOUNTID, APPID, SERVICEID, ENVID, CLOUDPROVIDERID, INSTANCETYPE, INSTANCECOUNT, ARTIFACTID, SANITYSTATUS) "
      + "VALUES(?,?,?,?,?,?,?,?,?,?) "
      + "ON CONFLICT(ACCOUNTID,APPID,SERVICEID,ENVID,CLOUDPROVIDERID,INSTANCETYPE,REPORTEDAT) "
      + "DO UPDATE SET INSTANCECOUNT=EXCLUDED.INSTANCECOUNT, SANITYSTATUS=EXCLUDED.SANITYSTATUS "
      + "WHERE INSTANCE_STATS_HOUR.SANITYSTATUS=FALSE";

  public HourlyAggregator(TimeseriesBatchEventInfo eventInfo) {
    super(eventInfo, FETCH_CHILD_DATA_POINTS_SQL, UPSERT_PARENT_TABLE_SQL, 6, "HOURLY AGGREGATOR");
  }

  @Override
  public Date getWindowBeginTimestamp() {
    long eventTimestamp = this.getEventInfo().getTimestamp();
    return DateUtils.getPrevWholeHourUTC(eventTimestamp);
  }

  @Override
  public Date getWindowEndTimestamp() {
    Date windowBeginTimestamp = getWindowBeginTimestamp();
    return DateUtils.addHours(windowBeginTimestamp.getTime(), 1);
  }

  @Override
  public InstanceAggregator getParentAggregatorObj() {
    return new DailyAggregator(this.getEventInfo());
  }

  @Override
  public void prepareUpsertQuery(PreparedStatement statement, Map<String, Object> params) throws SQLException {
    Map<String, String> dataMap = (Map<String, String>) params.get(InstanceEventAggregator.DATA_MAP_KEY);

    statement.setTimestamp(1, new Timestamp(this.getWindowBeginTimestamp().getTime()), DateUtils.getDefaultCalendar());
    statement.setString(2, this.getEventInfo().getAccountId());
    // TODO handle app id
    //    statement.setString(3, (String) dataMap.get(EventProcessor.APPID));
    statement.setString(4, dataMap.get(Constants.SERVICE_ID.getKey()));
    statement.setString(5, dataMap.get(Constants.ENV_ID.getKey()));
    statement.setString(6, dataMap.get(Constants.CLOUDPROVIDER_ID.getKey()));
    statement.setString(7, dataMap.get(Constants.INSTANCE_TYPE.getKey()));
    statement.setInt(8, (Integer) params.get(Constants.INSTANCECOUNT.getKey()));
    statement.setString(9, dataMap.get(Constants.ARTIFACT_ID.getKey()));
    statement.setBoolean(10, (Boolean) params.get(Constants.SANITYSTATUS.getKey()));
  }
}
