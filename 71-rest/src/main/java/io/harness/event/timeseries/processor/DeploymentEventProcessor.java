package io.harness.event.timeseries.processor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.utils.Lists;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.service.impl.event.timeseries.TimeSeriesEventInfo;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Singleton
@Slf4j
public class DeploymentEventProcessor implements EventProcessor<TimeSeriesEventInfo> {
  /**
   *  EXECUTIONID TEXT NOT NULL,
   * 	STARTTIME TIMESTAMP NOT NULL,
   * 	ENDTIME TIMESTAMP NOT NULL,
   * 	ACCOUNTID TEXT NOT NULL,
   * 	APPID TEXT NOT NULL,
   * 	TRIGGERED_BY TEXT,
   * 	TRIGGER_ID TEXT,
   * 	STATUS VARCHAR(20),
   * 	SERVICES TEXT[],
   * 	WORKFLOWS TEXT[],
   * 	CLOUDPROVIDERS TEXT[],
   * 	ENVIRONMENTS TEXT[],
   * 	PIPELINE TEXT,
   * 	DURATION BIGINT NOT NULL,
   * 	ARTIFACTS TEXT[]
   * 	ENVTYPES TEXT[]
   * 	PARENT_EXECUTION TEXT
   * 	STAGENAME TEXT
   * 	ROLLBACK_DURATION BIGINT
   */
  String insert_prepared_statement_sql =
      "INSERT INTO DEPLOYMENT (EXECUTIONID,STARTTIME,ENDTIME,ACCOUNTID,APPID,TRIGGERED_BY,TRIGGER_ID,STATUS,SERVICES,WORKFLOWS,CLOUDPROVIDERS,ENVIRONMENTS,PIPELINE,DURATION,ARTIFACTS,ENVTYPES,PARENT_EXECUTION,STAGENAME,ROLLBACK_DURATION, INSTANCES_DEPLOYED, TAGS) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject DataFetcherUtils utils;

  @Override
  public void processEvent(TimeSeriesEventInfo eventInfo) {
    if (timeScaleDBService.isValid()) {
      long startTime = System.currentTimeMillis();
      boolean successfulInsert = false;
      int retryCount = 0;
      while (!successfulInsert && retryCount < MAX_RETRY_COUNT) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             PreparedStatement insertPreparedStatement = dbConnection.prepareStatement(insert_prepared_statement_sql)) {
          /**
           *  EXECUTIONID TEXT NOT NULL,
           * 	STARTTIME TIMESTAMP NOT NULL,
           * 	ENDTIME TIMESTAMP NOT NULL,
           * 	ACCOUNTID TEXT NOT NULL,
           * 	APPID TEXT NOT NULL,
           * 	TRIGGERED_BY TEXT,
           * 	TRIGGER_ID TEXT,
           * 	STATUS VARCHAR(20),
           * 	SERVICES TEXT[],
           * 	WORKFLOWS TEXT[],
           * 	CLOUDPROVIDERS TEXT[],
           * 	ENVIRONMENTS TEXT[],
           * 	PIPELINE TEXT,
           * 	DURATION BIGINT NOT NULL,
           * 	ARTIFACTS TEXT[]
           * 	ENVTYPES TEXT[]
           * 	PARENT_EXECUTION TEXT
           * 	STAGENAME TEXT
           * 	ROLLBACK_DURATION BIGINT
           **/
          insertPreparedStatement.setString(1, eventInfo.getStringData().get(EventProcessor.EXECUTIONID));
          insertPreparedStatement.setTimestamp(
              2, new Timestamp(eventInfo.getLongData().get(EventProcessor.STARTTIME)), utils.getDefaultCalendar());
          insertPreparedStatement.setTimestamp(
              3, new Timestamp(eventInfo.getLongData().get(EventProcessor.ENDTIME)), utils.getDefaultCalendar());
          insertPreparedStatement.setString(4, eventInfo.getAccountId());
          insertPreparedStatement.setString(5, eventInfo.getStringData().get(EventProcessor.APPID));
          insertPreparedStatement.setString(6, eventInfo.getStringData().get(EventProcessor.TRIGGERED_BY));
          insertPreparedStatement.setString(7, eventInfo.getStringData().get(EventProcessor.TRIGGER_ID));
          insertPreparedStatement.setString(8, eventInfo.getStringData().get(EventProcessor.STATUS));

          if (eventInfo.getListData() == null) {
            logger.warn("TimeSeriesEventInfo has listData=null:[{}]", eventInfo);
          }

          insertArrayData(
              dbConnection, insertPreparedStatement, getListData(eventInfo, EventProcessor.SERVICE_LIST), 9);

          insertArrayData(
              dbConnection, insertPreparedStatement, getListData(eventInfo, EventProcessor.WORKFLOW_LIST), 10);

          insertArrayData(
              dbConnection, insertPreparedStatement, getListData(eventInfo, EventProcessor.CLOUD_PROVIDER_LIST), 11);

          insertArrayData(dbConnection, insertPreparedStatement, getListData(eventInfo, EventProcessor.ENV_LIST), 12);

          insertPreparedStatement.setString(13, eventInfo.getStringData().get(EventProcessor.PIPELINE));

          insertPreparedStatement.setLong(14, eventInfo.getLongData().get(EventProcessor.DURATION));

          insertArrayData(
              dbConnection, insertPreparedStatement, getListData(eventInfo, EventProcessor.ARTIFACT_LIST), 15);

          insertArrayData(dbConnection, insertPreparedStatement, getListData(eventInfo, EventProcessor.ENVTYPES), 16);

          insertPreparedStatement.setString(17, eventInfo.getStringData().get(EventProcessor.PARENT_EXECUTION));

          insertPreparedStatement.setString(18, eventInfo.getStringData().get(EventProcessor.STAGENAME));

          Long rollbackDuration = eventInfo.getLongData().get(EventProcessor.ROLLBACK_DURATION);
          if (rollbackDuration == null) {
            rollbackDuration = 0L;
          }

          insertPreparedStatement.setLong(19, rollbackDuration);

          Integer instancesDeployed = eventInfo.getIntegerData().get(EventProcessor.INSTANCES_DEPLOYED);
          if (instancesDeployed == null) {
            instancesDeployed = 0;
          }
          insertPreparedStatement.setInt(20, instancesDeployed);

          insertPreparedStatement.setObject(21, eventInfo.getData().get(EventProcessor.TAGS));

          insertPreparedStatement.execute();
          successfulInsert = true;
        } catch (SQLException e) {
          if (retryCount >= MAX_RETRY_COUNT) {
            logger.error("Failed to save deployment data,[{}]", eventInfo, e);
          } else {
            logger.info("Failed to save deployment data,[{}],retryCount=[{}]", eventInfo, retryCount);
          }
          retryCount++;
        } finally {
          logger.info("Total time=[{}]", System.currentTimeMillis() - startTime);
        }
      }
    } else {
      logger.trace("Not processing deployment time series data:[{}]", eventInfo);
    }
  }

  private List<String> getListData(TimeSeriesEventInfo eventInfo, String key) {
    return eventInfo.getListData() != null ? eventInfo.getListData().get(key) : new ArrayList<>();
  }

  private void insertArrayData(Connection dbConnection, PreparedStatement insertPreparedStatement, List<String> data,
      int index) throws SQLException {
    if (!Lists.isNullOrEmpty(data)) {
      Array array = dbConnection.createArrayOf("text", data.toArray());
      insertPreparedStatement.setArray(index, array);
    } else {
      insertPreparedStatement.setArray(index, null);
    }
  }
}
