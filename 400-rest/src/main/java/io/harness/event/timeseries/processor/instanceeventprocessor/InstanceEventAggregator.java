/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.timeseries.processor.instanceeventprocessor;

import io.harness.event.timeseries.processor.EventProcessor;
import io.harness.event.timeseries.processor.instanceeventprocessor.instanceaggregator.HourlyAggregator;
import io.harness.event.timeseries.processor.instanceeventprocessor.instanceaggregator.InstanceAggregator;
import io.harness.exception.InstanceAggregationException;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.service.impl.event.timeseries.TimeSeriesBatchEventInfo;

import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InstanceEventAggregator {
  private final Integer MAX_RETRY_COUNT = 5;
  private final String NUM_OF_RECORDS = "NUM_OF_RECORDS";
  public static final String DATA_MAP_KEY = "DATA_MAP_KEY";

  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject private DataFetcherUtils utils;

  public void doHourlyAggregation(TimeSeriesBatchEventInfo eventInfo) throws InstanceAggregationException {
    if (eventInfo == null) {
      return;
    }

    doAggregation(new HourlyAggregator(eventInfo));
  }

  // ----------------- PRIVATE METHODS ----------------------

  /**
   * It does the aggregation of the instance count for different aggregator patterns/durations
   * For every unique data point, it fetches child data points from DB, aggregates them and upserts into parent table
   *
   * **No use of instance count value in the data point info is done for aggregation, its ignored straightaway**
   *
   * @param aggregator
   * @throws InstanceAggregationException
   */
  private void doAggregation(InstanceAggregator aggregator) throws InstanceAggregationException {
    if (aggregator == null) {
      return;
    }

    boolean successfulUpsert = false;
    int retryCount = 0, QUERY_BATCH_SIZE = 1000, currDataPointIdx = 0, lastIdxProcessed = -1;
    TimeSeriesBatchEventInfo eventInfo = aggregator.getEventInfo();
    TimeSeriesBatchEventInfo.DataPoint[] dataPointArray =
        eventInfo.getDataPointList().toArray(new TimeSeriesBatchEventInfo.DataPoint[0]);

    while (!successfulUpsert && retryCount < MAX_RETRY_COUNT) {
      try {
        currDataPointIdx = lastIdxProcessed + 1;
        while (currDataPointIdx < dataPointArray.length) {
          lastIdxProcessed = processBatchQueries(aggregator, currDataPointIdx, QUERY_BATCH_SIZE);
          currDataPointIdx = lastIdxProcessed + 1;
        }

        successfulUpsert = true;

        // Now aggregation completed for this window, move to next parent window
        doAggregation(aggregator.getParentAggregatorObj());
      } catch (SQLException e) {
        if (retryCount >= MAX_RETRY_COUNT) {
          String errorLog =
              String.format("MAX RETRY FAILURE : Failed to save instance data , error : [%s]", e.toString());
          throw InstanceAggregationExceptionHandler.getException(aggregator, errorLog, e);
        } else {
          log.error("Failed to save instance data : [{}] , retryCount : [{}] , error : [{}]", eventInfo, retryCount,
              e.toString(), e);
        }
        retryCount++;
      } catch (Exception ex) {
        String errorLog = String.format(
            "Unchecked Exception : Failed to save instance data : [%s] , for aggregator : [%s] , error : [%s]",
            eventInfo, aggregator.toString(), ex.toString());
        // In case of unknown exception, just halt the processing and throw exception
        //        log.error(errorLog, ex);
        throw InstanceAggregationExceptionHandler.getException(aggregator, errorLog, ex);
      }
    }
  }

  private Integer processBatchQueries(InstanceAggregator aggregator, Integer currElementIdx, Integer batchSize)
      throws SQLException {
    TimeSeriesBatchEventInfo eventInfo = aggregator.getEventInfo();
    TimeSeriesBatchEventInfo.DataPoint[] dataPointArray =
        eventInfo.getDataPointList().toArray(new TimeSeriesBatchEventInfo.DataPoint[0]);
    int currentBatchSize = 0;

    try (Connection dbConnection = timeScaleDBService.getDBConnection();
         PreparedStatement execStatement = dbConnection.prepareStatement(aggregator.getUpsertParentTableSQL())) {
      for (; currElementIdx < dataPointArray.length && currentBatchSize < batchSize;
           currElementIdx++, currentBatchSize++) {
        try {
          Map<String, Object> dataMap = dataPointArray[currElementIdx].getData();
          Map<String, Object> resultMap = fetchChildDataPointResults(dbConnection, aggregator, dataMap);

          Integer windowInstanceCount = getMedianInstanceCount(resultMap);
          Boolean sanityStatus = getSanityStatus(aggregator, resultMap);

          // Prepare statement additional params
          Map<String, Object> statementParams =
              prepareUpserQueryStatementParams(dataMap, windowInstanceCount, sanityStatus);

          // Now create upsert statements to be executed
          aggregator.prepareUpsertQuery(execStatement, statementParams);
          execStatement.addBatch();
        } catch (SQLException e) {
          log.error("Failed to process instance event data point : [{}] , error : [{}]",
              dataPointArray[currElementIdx].getData(), e.toString(), e);
        }
      }

      execStatement.executeBatch();
      return currElementIdx - 1;
    }
  }

  private Map<String, Object> fetchChildDataPointResults(
      Connection dbConnection, InstanceAggregator aggregator, Map<String, Object> dataMap) throws SQLException {
    // It calculates the window reportedAt timestamp representative
    Date windowBeginTimestamp = aggregator.getWindowBeginTimestamp();
    Date windowEndTimestamp = aggregator.getWindowEndTimestamp();

    // Fetch all records for current hour window until eventTimestamp to calculate Median of the data
    try (PreparedStatement statement = dbConnection.prepareStatement(aggregator.getFetchChildDataPointsSQL())) {
      statement.setTimestamp(1, new Timestamp(windowBeginTimestamp.getTime()), utils.getDefaultCalendar());
      statement.setTimestamp(2, new Timestamp(windowEndTimestamp.getTime()), utils.getDefaultCalendar());
      statement.setString(3, aggregator.getEventInfo().getAccountId());
      statement.setString(4, (String) dataMap.get(EventProcessor.APPID));
      statement.setString(5, (String) dataMap.get(EventProcessor.SERVICEID));
      statement.setString(6, (String) dataMap.get(EventProcessor.ENVID));
      statement.setString(7, (String) dataMap.get(EventProcessor.CLOUDPROVIDERID));
      statement.setString(8, (String) dataMap.get(EventProcessor.INSTANCETYPE));
      ResultSet result = statement.executeQuery();
      return parseResults(result);
    }
  }

  private Map<String, Object> prepareUpserQueryStatementParams(
      Map<String, Object> dataMap, Integer instanceCount, Boolean sanityStatus) {
    Map<String, Object> statementParams = new HashMap<>();
    statementParams.put(DATA_MAP_KEY, dataMap);
    statementParams.put(EventProcessor.SANITYSTATUS, sanityStatus);
    statementParams.put(EventProcessor.INSTANCECOUNT, instanceCount);

    return statementParams;
  }

  private Map<String, Object> parseResults(ResultSet result) throws SQLException {
    Map<String, Object> resultMap = new HashMap<>();
    if (result == null) {
      return resultMap;
    }

    int numOfCols = result.getMetaData().getColumnCount();
    // Sanity status denotes the AND of sanity status of total records fetched internally for the query
    // ** It doesn't denote the final sanity status **
    Boolean sanityStatus = Boolean.FALSE;
    Integer instanceCount = 0;
    // Num of records denote the total records fetched internally by query and later aggregated to find median
    Integer numOfRecords = 0;

    // Only single result row would be present
    while (result.next()) {
      // DATA SANITY Col not present in Instance Stats table (used for Hourly aggregation)
      // So, in that case, cols fetched is 2
      if (numOfCols > 2) {
        sanityStatus = result.getBoolean(EventProcessor.SANITYSTATUS);
      }
      instanceCount = result.getInt(EventProcessor.INSTANCECOUNT);
      numOfRecords = result.getInt(NUM_OF_RECORDS);
    }

    resultMap.put(EventProcessor.SANITYSTATUS, sanityStatus);
    resultMap.put(EventProcessor.INSTANCECOUNT, instanceCount);
    resultMap.put(NUM_OF_RECORDS, numOfRecords);
    return resultMap;
  }

  private Integer getMedianInstanceCount(Map<String, Object> resultMap) {
    return (Integer) resultMap.getOrDefault(EventProcessor.INSTANCECOUNT, 0);
  }

  private Boolean getSanityStatus(InstanceAggregator aggregator, Map<String, Object> resultMap) {
    Boolean sanityStatus = (Boolean) resultMap.getOrDefault(EventProcessor.SANITYSTATUS, Boolean.FALSE);
    Integer numOfRecords = (Integer) resultMap.getOrDefault(NUM_OF_RECORDS, 0);

    // If total num of records aggregated is not equal to aggregator window size
    // Then its straight forward FALSE status as required num of records are not present
    if (!numOfRecords.equals(aggregator.getWindowSize())) {
      return Boolean.FALSE;
    }

    // Required num of records is the only and only param for hourly aggregator sanity status
    if (aggregator instanceof HourlyAggregator) {
      return Boolean.TRUE;
    }

    return sanityStatus;
  }
}
