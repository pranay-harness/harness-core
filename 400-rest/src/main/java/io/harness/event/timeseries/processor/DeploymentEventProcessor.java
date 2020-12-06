package io.harness.event.timeseries.processor;

import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.service.impl.event.timeseries.TimeSeriesEventInfo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.fabric8.utils.Lists;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class DeploymentEventProcessor implements EventProcessor<TimeSeriesEventInfo> {
  /**
   *    EXECUTIONID TEXT NOT NULL,
   *    STARTTIME TIMESTAMP NOT NULL,
   *    ENDTIME TIMESTAMP NOT NULL,
   *    ACCOUNTID TEXT NOT NULL,
   *    APPID TEXT NOT NULL,
   *    TRIGGERED_BY TEXT,
   *    TRIGGER_ID TEXT,
   *    STATUS VARCHAR(20),
   *    SERVICES TEXT[],
   *    WORKFLOWS TEXT[],
   *    CLOUDPROVIDERS TEXT[],
   *    ENVIRONMENTS TEXT[],
   *    PIPELINE TEXT,
   *    DURATION BIGINT NOT NULL,
   *    ARTIFACTS TEXT[]
   *    ENVTYPES TEXT[]
   *    PARENT_EXECUTION TEXT
   *    STAGENAME TEXT
   *    ROLLBACK_DURATION BIGINT
   */
  private static final Long sqlMinTimestamp = 0L;
  private static final String query_statement = "SELECT * FROM DEPLOYMENT WHERE EXECUTIONID=?";
  private static final String delete_statement = "DELETE FROM DEPLOYMENT WHERE EXECUTIONID=?";
  private static final String insert_statement =
      "INSERT INTO DEPLOYMENT (EXECUTIONID,STARTTIME,ENDTIME,ACCOUNTID,APPID,TRIGGERED_BY,TRIGGER_ID,STATUS,SERVICES,WORKFLOWS,CLOUDPROVIDERS,ENVIRONMENTS,PIPELINE,DURATION,ARTIFACTS,ENVTYPES,PARENT_EXECUTION,STAGENAME,ROLLBACK_DURATION, INSTANCES_DEPLOYED, TAGS) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject DataFetcherUtils utils;

  @Override
  public void processEvent(TimeSeriesEventInfo eventInfo) {
    if (timeScaleDBService.isValid()) {
      long startTime = System.currentTimeMillis();
      boolean successful = false;
      int retryCount = 0;
      while (!successful && retryCount < MAX_RETRY_COUNT) {
        ResultSet queryResult = null;
        try (Connection connection = timeScaleDBService.getDBConnection();
             PreparedStatement queryStatement = connection.prepareStatement(query_statement);
             PreparedStatement deleteStatement = connection.prepareStatement(delete_statement);
             PreparedStatement insertStatement = connection.prepareStatement(insert_statement)) {
          queryResult = queryDataInTimescaleDB(eventInfo, queryStatement);

          if (queryResult != null && queryResult.next()) {
            log.info(
                "WorkflowExecution found:[{}],updating it", eventInfo.getStringData().get(EventProcessor.EXECUTIONID));
            deleteDataInTimescaleDB(eventInfo, deleteStatement);
            insertDataInTimescaleDB(eventInfo, connection, insertStatement);
          } else {
            log.info("WorkflowExecution not found:[{}],inserting it",
                eventInfo.getStringData().get(EventProcessor.EXECUTIONID));
            insertDataInTimescaleDB(eventInfo, connection, insertStatement);
          }
          successful = true;
        } catch (SQLException e) {
          log.error("Failed to save deployment data,[{}],retryCount=[{}] ", eventInfo, retryCount++, e);
        } catch (Exception e) {
          log.error("Failed to save deployment data,[{}]", eventInfo, e);
          retryCount = MAX_RETRY_COUNT + 1;
        } finally {
          DBUtils.close(queryResult);
          log.info("Total time=[{}]", System.currentTimeMillis() - startTime);
        }
      }
    } else {
      log.trace("Not processing deployment time series data:[{}]", eventInfo);
    }
  }

  private ResultSet queryDataInTimescaleDB(TimeSeriesEventInfo eventInfo, PreparedStatement queryStatement)
      throws SQLException {
    queryStatement.setString(1, eventInfo.getStringData().get(EventProcessor.EXECUTIONID));
    return queryStatement.executeQuery();
  }

  private void deleteDataInTimescaleDB(TimeSeriesEventInfo eventInfo, PreparedStatement deleteStatement)
      throws SQLException {
    deleteStatement.setString(1, eventInfo.getStringData().get(EventProcessor.EXECUTIONID));
    deleteStatement.execute();
  }

  private void insertDataInTimescaleDB(
      TimeSeriesEventInfo eventInfo, Connection dbConnection, PreparedStatement insertStatement) throws SQLException {
    int index = 0;

    insertStatement.setString(++index, eventInfo.getStringData().get(EventProcessor.EXECUTIONID));
    insertStatement.setTimestamp(
        ++index, new Timestamp(eventInfo.getLongData().get(EventProcessor.STARTTIME)), utils.getDefaultCalendar());

    Long endtime = eventInfo.getLongData().get(EventProcessor.ENDTIME);
    if (endtime != null) {
      insertStatement.setTimestamp(
          ++index, new Timestamp(eventInfo.getLongData().get(EventProcessor.ENDTIME)), utils.getDefaultCalendar());
    } else {
      insertStatement.setTimestamp(++index, new Timestamp(sqlMinTimestamp), utils.getDefaultCalendar());
    }

    insertStatement.setString(++index, eventInfo.getAccountId());
    insertStatement.setString(++index, eventInfo.getStringData().get(EventProcessor.APPID));
    insertStatement.setString(++index, eventInfo.getStringData().get(EventProcessor.TRIGGERED_BY));
    insertStatement.setString(++index, eventInfo.getStringData().get(EventProcessor.TRIGGER_ID));
    insertStatement.setString(++index, eventInfo.getStringData().get(EventProcessor.STATUS));

    if (eventInfo.getListData() == null) {
      log.warn("TimeSeriesEventInfo has listData=null:[{}]", eventInfo);
    }

    insertArrayData(dbConnection, insertStatement, getListData(eventInfo, EventProcessor.SERVICE_LIST), ++index);
    insertArrayData(dbConnection, insertStatement, getListData(eventInfo, EventProcessor.WORKFLOW_LIST), ++index);
    insertArrayData(dbConnection, insertStatement, getListData(eventInfo, EventProcessor.CLOUD_PROVIDER_LIST), ++index);
    insertArrayData(dbConnection, insertStatement, getListData(eventInfo, EventProcessor.ENV_LIST), ++index);

    insertStatement.setString(++index, eventInfo.getStringData().get(EventProcessor.PIPELINE));

    Long duration = getDuration(eventInfo);
    insertStatement.setLong(++index, duration);

    insertArrayData(dbConnection, insertStatement, getListData(eventInfo, EventProcessor.ARTIFACT_LIST), ++index);
    insertArrayData(dbConnection, insertStatement, getListData(eventInfo, EventProcessor.ENVTYPES), ++index);

    insertStatement.setString(++index, eventInfo.getStringData().get(EventProcessor.PARENT_EXECUTION));
    insertStatement.setString(++index, eventInfo.getStringData().get(EventProcessor.STAGENAME));

    Long rollbackDuration = getRollbackDuration(eventInfo);
    insertStatement.setLong(++index, rollbackDuration);

    Integer instancesDeployed = getInstancesDeployed(eventInfo);
    insertStatement.setInt(++index, instancesDeployed);
    insertStatement.setObject(++index, eventInfo.getData().get(EventProcessor.TAGS));

    insertStatement.execute();
  }

  private Long getDuration(TimeSeriesEventInfo eventInfo) {
    Long duration = eventInfo.getLongData().get(EventProcessor.DURATION);
    if (duration == null) {
      duration = 0L;
    }
    return duration;
  }

  private Long getRollbackDuration(TimeSeriesEventInfo eventInfo) {
    Long rollbackDuration = eventInfo.getLongData().get(EventProcessor.ROLLBACK_DURATION);
    if (rollbackDuration == null) {
      rollbackDuration = 0L;
    }
    return rollbackDuration;
  }

  private Integer getInstancesDeployed(TimeSeriesEventInfo eventInfo) {
    Integer instancesDeployed = eventInfo.getIntegerData().get(EventProcessor.INSTANCES_DEPLOYED);
    if (instancesDeployed == null) {
      instancesDeployed = 0;
    }
    return instancesDeployed;
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
