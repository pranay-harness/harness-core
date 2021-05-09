package io.harness.service.stats.usagemetrics.eventconsumer;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.eventsframework.NgEventLogContext;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.schemas.timeseriesevent.DataPoint;
import io.harness.eventsframework.schemas.timeseriesevent.TimeseriesBatchEventInfo;
import io.harness.exception.InstanceAggregationException;
import io.harness.exception.InstanceProcessorException;
import io.harness.ff.FeatureFlagService;
import io.harness.logging.AutoLogContext;
import io.harness.ng.core.event.MessageListener;
import io.harness.service.stats.Constants;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.graphql.datafetcher.DataFetcherUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.DX)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class InstanceStatsEventListener implements MessageListener {
  private static final String insert_prepared_statement_sql =
      "INSERT INTO INSTANCE_STATS (REPORTEDAT, ACCOUNTID, ORGID, PROJECID, SERVICEID, ENVID, CLOUDPROVIDERID, INSTANCETYPE, INSTANCECOUNT, ARTIFACTID) VALUES (?,?,?,?,?,?,?,?,?,?)";
  private static final Integer MAX_RETRY_COUNT = 5;

  private TimeScaleDBService timeScaleDBService;
  private InstanceEventAggregator instanceEventAggregator;
  private FeatureFlagService featureFlagService;
  private DataFetcherUtils utils;

  @Override
  public boolean handleMessage(Message message) {
    final String messageId = message.getId();
    log.info("Processing the instance stats timescale event with the id {}", messageId);
    try (AutoLogContext ignore1 = new NgEventLogContext(messageId, OVERRIDE_ERROR)) {
      TimeseriesBatchEventInfo eventInfo = TimeseriesBatchEventInfo.parseFrom(message.getMessage().getData());
      //
      if (timeScaleDBService.isValid()) {
        boolean successfulInsert = false;
        int retryCount = 0, QUERY_BATCH_SIZE = 1000, currDataPointIdx = 0, lastIdxProcessed = -1;

        while (!successfulInsert && retryCount < MAX_RETRY_COUNT) {
          try {
            // Reset currIdx to the lastIdxProcessed in case of retry after failures
            currDataPointIdx = lastIdxProcessed + 1;
            while (currDataPointIdx < eventInfo.getDataPointListList().size()) {
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
                return true;
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
          }
        }
      } else {
        log.trace("Not processing instance stats event:[{}]", eventInfo);
        return false;
      }
      return true;
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking TimeseriesBatchEventInfo for key {}", message.getId(), e);
      return false;
    } catch (Exception ex) {
      log.error("Unchecked exception faced during handling TimeseriesBatchEventInfo for key {}", message.getId(), ex);
      return false;
    }
  }

  // --------------------------- PRIVATE METHODS ------------------------------

  private Integer processBatchInserts(TimeseriesBatchEventInfo eventInfo, Integer currElementIdx, Integer batchSize)
      throws SQLException {
    DataPoint[] dataPointArray = eventInfo.getDataPointListList().toArray(new DataPoint[0]);
    int currentBatchSize = 0;

    try (Connection dbConnection = timeScaleDBService.getDBConnection();
         PreparedStatement statement = dbConnection.prepareStatement(insert_prepared_statement_sql)) {
      for (; currElementIdx < dataPointArray.length && currentBatchSize < batchSize;
           currElementIdx++, currentBatchSize++) {
        try {
          Map<String, String> dataMap = dataPointArray[currElementIdx].getDataMap();
          statement.setTimestamp(1, new Timestamp(eventInfo.getTimestamp()), utils.getDefaultCalendar());
          statement.setString(2, eventInfo.getAccountId());
          statement.setString(3, dataMap.get(Constants.ORG_ID.getKey()));
          statement.setString(4, dataMap.get(Constants.PROJECT_ID.getKey()));
          statement.setString(5, dataMap.get(Constants.SERVICE_ID.getKey()));
          statement.setString(6, dataMap.get(Constants.ENV_ID.getKey()));
          statement.setString(7, dataMap.get(Constants.CLOUDPROVIDER_ID.getKey()));
          statement.setString(8, dataMap.get(Constants.INSTANCE_TYPE.getKey()));
          statement.setInt(9, Integer.parseInt(dataMap.get(Constants.INSTANCECOUNT.getKey())));
          statement.setString(10, dataMap.get(Constants.ARTIFACT_ID.getKey()));
          statement.addBatch();
        } catch (SQLException e) {
          // Ignore this exception for now, as this is the least expected to happen
          // If any issues come later on regarding missing data, this log will help us to trace it out
          log.error("Failed to process instance event data point : [{}] , error : [{}]",
              dataPointArray[currElementIdx].getDataMap(), e.toString(), e);
        }
      }

      statement.executeBatch();
      return currElementIdx - 1;
    }
  }
}
