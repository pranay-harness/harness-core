/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.timeseries.processor.instanceeventprocessor.instancereconservice;

public class InstanceReconConstants {
  public static final Integer DEFAULT_QUERY_BATCH_SIZE = 100;
  public static final Integer DEFAULT_EVENTS_LIMIT = 10;
  public static final Integer MAX_RETRY_COUNT = 5;

  public static final String FETCH_INSTANCE_DATA_POINTS_INTERVAL_BATCH_SQL =
      "SELECT * FROM INSTANCE_STATS WHERE ACCOUNTID = ? "
      + "AND REPORTEDAT >= ? AND REPORTEDAT <= ? ORDER BY REPORTEDAT DESC OFFSET ? LIMIT ?";

  public static final String FETCH_INSTANCE_STATS_HOUR_OLDEST_COMPLETE_RECORD =
      "SELECT * FROM INSTANCE_STATS_HOUR WHERE ACCOUNTID = ? AND SANITYSTATUS = TRUE ORDER BY REPORTEDAT LIMIT 1";

  public static final String FETCH_NEAREST_OLDER_INSTANCE_STATS_DATA_POINT_TO_REPORTED_AT =
      "SELECT * FROM INSTANCE_STATS WHERE ACCOUNTID = ? AND REPORTEDAT < ? ORDER BY REPORTEDAT DESC LIMIT 1";
}
