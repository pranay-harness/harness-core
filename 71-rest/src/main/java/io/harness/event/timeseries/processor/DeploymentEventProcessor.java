package io.harness.event.timeseries.processor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.utils.Lists;
import io.harness.exception.WingsException;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.impl.event.timeseries.TimeSeriesEventInfo;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Singleton
@Slf4j
public class DeploymentEventProcessor implements EventProcessor {
  private PreparedStatement insertPreparedStatement;

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
   */
  String insert_prepared_statement_sql =
      "INSERT INTO DEPLOYMENT (EXECUTIONID,STARTTIME,ENDTIME,ACCOUNTID,APPID,TRIGGERED_BY,TRIGGER_ID,STATUS,SERVICES,WORKFLOWS,CLOUDPROVIDERS,ENVIRONMENTS,PIPELINE,DURATION,ARTIFACTS) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

  @Inject private TimeScaleDBService timeScaleDBService;

  @Override
  public boolean init() {
    if (timeScaleDBService.isValid()) {
      try {
        insertPreparedStatement = timeScaleDBService.getDBConnection().prepareStatement(insert_prepared_statement_sql);
        return true;
      } catch (SQLException e) {
        logger.error("Failed to create a insertPreparedStatement for timeseries database", e);
        return false;
      }
    } else {
      return true;
    }
  }

  @Override
  public void processEvent(TimeSeriesEventInfo eventInfo) {
    if (timeScaleDBService.isValid() && insertPreparedStatement != null) {
      Connection dbConnection = null;
      try {
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
         **/
        dbConnection = timeScaleDBService.getDBConnection();
        insertPreparedStatement.setString(1, eventInfo.getStringData().get(EventProcessor.EXECUTIONID));
        insertPreparedStatement.setTimestamp(2, new Timestamp(eventInfo.getLongData().get(EventProcessor.STARTTIME)));
        insertPreparedStatement.setTimestamp(3, new Timestamp(eventInfo.getLongData().get(EventProcessor.ENDTIME)));
        insertPreparedStatement.setString(4, eventInfo.getAccountId());
        insertPreparedStatement.setString(5, eventInfo.getStringData().get(EventProcessor.APPID));
        insertPreparedStatement.setString(6, eventInfo.getStringData().get(EventProcessor.TRIGGERED_BY));
        insertPreparedStatement.setString(7, eventInfo.getStringData().get(EventProcessor.TRIGGER_ID));
        insertPreparedStatement.setString(8, eventInfo.getStringData().get(EventProcessor.STATUS));

        final List<String> serviceList = eventInfo.getListData().get(EventProcessor.SERVICE_LIST);
        if (!Lists.isNullOrEmpty(serviceList)) {
          Array array = dbConnection.createArrayOf("text", serviceList.toArray());
          insertPreparedStatement.setArray(9, array);
        } else {
          insertPreparedStatement.setArray(9, null);
        }

        final List<String> workflowList = eventInfo.getListData().get(EventProcessor.WORKFLOW_LIST);
        if (!Lists.isNullOrEmpty(workflowList)) {
          Array array = dbConnection.createArrayOf("text", workflowList.toArray());
          insertPreparedStatement.setArray(10, array);
        } else {
          insertPreparedStatement.setArray(10, null);
        }

        final List<String> cloudProviderList = eventInfo.getListData().get(EventProcessor.CLOUD_PROVIDER_LIST);
        if (!Lists.isNullOrEmpty(cloudProviderList)) {
          Array array = dbConnection.createArrayOf("text", cloudProviderList.toArray());
          insertPreparedStatement.setArray(11, array);
        } else {
          insertPreparedStatement.setArray(11, null);
        }

        final List<String> envList = eventInfo.getListData().get(EventProcessor.ENV_LIST);
        if (!Lists.isNullOrEmpty(envList)) {
          Array array = dbConnection.createArrayOf("text", envList.toArray());
          insertPreparedStatement.setArray(12, array);
        } else {
          insertPreparedStatement.setArray(12, null);
        }

        insertPreparedStatement.setString(13, eventInfo.getStringData().get(EventProcessor.PIPELINE));

        insertPreparedStatement.setLong(14, eventInfo.getLongData().get(EventProcessor.DURATION));

        final List<String> artifactList = eventInfo.getListData().get(EventProcessor.ARTIFACT_LIST);
        if (!Lists.isNullOrEmpty(artifactList)) {
          Array array = dbConnection.createArrayOf("text", artifactList.toArray());
          insertPreparedStatement.setArray(15, array);
        } else {
          insertPreparedStatement.setArray(15, null);
        }

        insertPreparedStatement.execute();
      } catch (SQLException e) {
        logger.error("Failed to save deployment data,[{}]", eventInfo, e);
      } finally {
        closeConnection(dbConnection);
      }
    } else {
      logger.trace("Not processing data:[{}]", eventInfo);
    }
  }

  private void closeConnection(Connection connection) throws WingsException {
    try {
      if (connection != null) {
        connection.close();
      }
    } catch (SQLException e) {
      logger.error("Failed to close connection", e);
    }
  }
}
