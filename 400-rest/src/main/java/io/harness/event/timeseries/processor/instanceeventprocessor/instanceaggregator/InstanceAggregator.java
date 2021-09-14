/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.timeseries.processor.instanceeventprocessor.instanceaggregator;

import software.wings.service.impl.event.timeseries.TimeSeriesBatchEventInfo;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import lombok.Getter;

@Getter
public abstract class InstanceAggregator {
  private final TimeSeriesBatchEventInfo eventInfo;
  private final String fetchChildDataPointsSQL;
  private final String upsertParentTableSQL;
  private Integer windowSize;
  private String aggregatorName;

  public InstanceAggregator(TimeSeriesBatchEventInfo eventInfo, String fetchChildDataPointsSQL,
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
                                 .append(eventInfo.getLog())
                                 .append('}');
    return sb.toString();
  }
}
