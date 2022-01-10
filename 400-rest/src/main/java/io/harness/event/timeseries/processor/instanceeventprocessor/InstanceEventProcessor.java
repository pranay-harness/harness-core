/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.timeseries.processor.instanceeventprocessor;

import io.harness.beans.FeatureName;
import io.harness.event.timeseries.processor.EventProcessor;
import io.harness.exception.InstanceAggregationException;
import io.harness.exception.InstanceProcessorException;
import io.harness.ff.FeatureFlagService;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.service.impl.event.timeseries.TimeSeriesBatchEventInfo;
import software.wings.service.impl.event.timeseries.TimeSeriesBatchEventInfo.DataPoint;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * @author rktummala
 */
@Singleton
@Slf4j
public class InstanceEventProcessor implements EventProcessor<TimeSeriesBatchEventInfo> {
  String insert_prepared_statement_sql =
      "INSERT INTO INSTANCE_STATS (REPORTEDAT, ACCOUNTID, APPID, SERVICEID, ENVID, CLOUDPROVIDERID, INSTANCETYPE, INSTANCECOUNT, ARTIFACTID) VALUES (?,?,?,?,?,?,?,?,?)";

  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject private DataFetcherUtils utils;
  @Inject private InstanceEventAggregator instanceEventAggregator;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public void processEvent(TimeSeriesBatchEventInfo eventInfo) throws InstanceProcessorException {
    if (eventInfo == null) {
      throw new InstanceProcessorException("EventInfo cannot be null");
    }

    if (timeScaleDBService.isValid()) {
      long startTime = System.currentTimeMillis();
      boolean successfulInsert = false;
      int retryCount = 0, QUERY_BATCH_SIZE = 1000, currDataPointIdx = 0, lastIdxProcessed = -1;
      DataPoint[] dataPointArray = eventInfo.getDataPointList().toArray(new DataPoint[0]);

      while (!successfulInsert && retryCount < MAX_RETRY_COUNT) {
        try {
          // Reset currIdx to the lastIdxProcessed in case of retry after failures
          currDataPointIdx = lastIdxProcessed + 1;
          while (currDataPointIdx < dataPointArray.length) {
            lastIdxProcessed = processBatchInserts(eventInfo, currDataPointIdx, QUERY_BATCH_SIZE);
            currDataPointIdx = lastIdxProcessed + 1;
          }

          // Update once all queries/data points are completed processing
          successfulInsert = true;

          // Trigger further aggregation of data points
          try {
            // If instance aggregation is not enabled for given account, skip the aggregation and return
            if (!featureFlagService.isEnabled(
                    FeatureName.CUSTOM_DASHBOARD_ENABLE_REALTIME_INSTANCE_AGGREGATION, eventInfo.getAccountId())) {
              return;
            }
            instanceEventAggregator.doHourlyAggregation(eventInfo);
          } catch (InstanceAggregationException exception) {
            log.error("Instance Aggregation Failure", exception);
          }
        } catch (SQLException e) {
          if (retryCount >= MAX_RETRY_COUNT) {
            String errorLog =
                String.format("MAX RETRY FAILURE : Failed to save instance data , error : [%s]", e.toString());
            // throw error to the queue listener for retry of the event later on
            throw new InstanceProcessorException(errorLog, e);
          } else {
            log.error("Failed to save instance data : [{}] , retryCount : [{}] , error : [{}]", eventInfo, retryCount,
                e.toString(), e);
          }
          retryCount++;
        } catch (Exception ex) {
          String errorLog = String.format(
              "Unchecked Exception : Failed to save instance data : [%s] , error : [%s]", eventInfo, ex.toString());
          // In case of unknown exception, just halt the processing
          throw new InstanceProcessorException(errorLog, ex);
        } finally {
          log.info("Total time=[{}]", System.currentTimeMillis() - startTime);
        }
      }
    } else {
      log.trace("Not processing instance stats event:[{}]", eventInfo);
    }
  }

  // ----------------- PRIVATE METHODS ----------------------

  private Integer processBatchInserts(TimeSeriesBatchEventInfo eventInfo, Integer currElementIdx, Integer batchSize)
      throws Exception {
    DataPoint[] dataPointArray = eventInfo.getDataPointList().toArray(new DataPoint[0]);
    int currentBatchSize = 0;

    try (Connection dbConnection = timeScaleDBService.getDBConnection();
         PreparedStatement statement = dbConnection.prepareStatement(insert_prepared_statement_sql)) {
      for (; currElementIdx < dataPointArray.length && currentBatchSize < batchSize;
           currElementIdx++, currentBatchSize++) {
        try {
          Map<String, Object> dataMap = dataPointArray[currElementIdx].getData();
          statement.setTimestamp(1, new Timestamp(eventInfo.getTimestamp()), utils.getDefaultCalendar());
          statement.setString(2, eventInfo.getAccountId());
          statement.setString(3, (String) dataMap.get(EventProcessor.APPID));
          statement.setString(4, (String) dataMap.get(EventProcessor.SERVICEID));
          statement.setString(5, (String) dataMap.get(EventProcessor.ENVID));
          statement.setString(6, (String) dataMap.get(EventProcessor.CLOUDPROVIDERID));
          statement.setString(7, (String) dataMap.get(EventProcessor.INSTANCETYPE));
          statement.setInt(8, (Integer) dataMap.get(EventProcessor.INSTANCECOUNT));
          statement.setString(9, (String) dataMap.get(EventProcessor.ARTIFACTID));
          statement.addBatch();
        } catch (SQLException e) {
          // Ignore this exception for now, as this is the least expected to happen
          // If any issues come later on regarding missing data, this log will help us to trace it out
          log.error("Failed to process instance event data point : [{}] , error : [{}]",
              dataPointArray[currElementIdx].getData(), e.toString(), e);
        }
      }

      statement.executeBatch();
      return currElementIdx - 1;
    }
  }

  // starting from timestamp, generate data until current time
  //  public void generateInstanceDPs(Long timestamp, String accountId, Map<String, Object> dataPoint) throws Exception
  //  {
  //    Random rand = new Random();
  //    Long currentTime = new Date().getTime();
  //    while (timestamp < currentTime) {
  //      Integer instanceCount = rand.nextInt(10);
  //      dataPoint.put(EventProcessor.INSTANCECOUNT, instanceCount);
  //      List<DataPoint> dpList = new ArrayList<>();
  //      dpList.add(DataPoint.builder().data(dataPoint).build());
  //      TimeSeriesBatchEventInfo event =
  //          TimeSeriesBatchEventInfo.builder().accountId(accountId).timestamp(timestamp).dataPointList(dpList).build();
  //
  //      processEvent(event);
  //      timestamp = DateUtils.addMinutes(timestamp, 10).getTime();
  //    }
  //  }
}
