package io.harness.event.timeseries.processor;

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
import java.util.List;
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

  @Override
  public void processEvent(TimeSeriesBatchEventInfo eventInfo) {
    if (timeScaleDBService.isValid()) {
      long startTime = System.currentTimeMillis();
      boolean successfulInsert = false;
      int retryCount = 0;
      while (!successfulInsert && retryCount < MAX_RETRY_COUNT) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             PreparedStatement statement = dbConnection.prepareStatement(insert_prepared_statement_sql)) {
          List<DataPoint> dataPointList = eventInfo.getDataPointList();
          dataPointList.forEach(dataPoint -> {
            try {
              Map<String, Object> dataMap = dataPoint.getData();
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
              log.error("Failed to save instance event data point for account {} and timestamp {}",
                  eventInfo.getAccountId(), eventInfo.getTimestamp(), e);
              return;
            }
          });

          statement.executeBatch();
          successfulInsert = true;

        } catch (SQLException e) {
          if (retryCount >= MAX_RETRY_COUNT) {
            log.error("Failed to save instance data,[{}]", eventInfo, e);
          } else {
            log.info("Failed to save instance data,[{}],retryCount=[{}]", eventInfo, retryCount);
          }
          retryCount++;
        } finally {
          log.info("Total time=[{}]", System.currentTimeMillis() - startTime);
        }
      }
    } else {
      log.trace("Not processing instance stats event:[{}]", eventInfo);
    }
  }
}
