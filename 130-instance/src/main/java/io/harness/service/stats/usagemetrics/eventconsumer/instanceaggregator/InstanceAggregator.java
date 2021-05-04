package io.harness.service.stats.usagemetrics.eventconsumer.instanceaggregator;

import io.harness.eventsframework.schemas.timeseriesevent.TimeseriesBatchEventInfo;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import lombok.Getter;

@Getter
public abstract class InstanceAggregator {
  private final TimeseriesBatchEventInfo eventInfo;
  private final String fetchChildDataPointsSQL;
  private final String upsertParentTableSQL;
  private Integer windowSize;
  private String aggregatorName;

  public InstanceAggregator(TimeseriesBatchEventInfo eventInfo, String fetchChildDataPointsSQL,
      String upsertParentTableSQL, Integer windowSize, String aggregatorName) {
    this.eventInfo = eventInfo;
    this.fetchChildDataPointsSQL = fetchChildDataPointsSQL;
    this.upsertParentTableSQL = upsertParentTableSQL;
    this.windowSize = windowSize;
    this.aggregatorName = aggregatorName;
  }

  public abstract Date getWindowBeginTimestamp();

  public abstract Date getWindowEndTimestamp();

  public abstract InstanceAggregator getParentAggregatorObj();

  public abstract void prepareUpsertQuery(PreparedStatement statement, Map<String, Object> params) throws SQLException;

  public void setWindowSize(Integer windowSize) {
    this.windowSize = windowSize;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("InstanceAggregator{aggregatorName='")
                                 .append(aggregatorName)
                                 .append("'eventInfo=")
                                 .append(eventInfo.toString())
                                 .append('}');
    return sb.toString();
  }
}
