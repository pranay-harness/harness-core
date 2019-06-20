package io.harness.event.timeseries.processor;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.hazelcast.util.Preconditions;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ContinuousVerificationService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Singleton
@Slf4j
public class VerificationEventProcessor {
  @Inject private ContinuousVerificationService continuousVerificationService;

  private static final int MAX_RETRY_COUNT = 5;

  /**
   ACCOUNT_ID TEXT NOT NULL,
   APP_ID TEXT NOT NULL,
   SERVICE_ID TEXT NOT NULL,
   WORKFLOW_ID TEXT,
   WORKFLOW_EXECUTION_ID TEXT,
   STATE_EXECUTION_ID TEXT,
   CV_CONFIG_ID TEXT,
   STATE_TYPE TEXT NOT NULL,
   START_TIME TIMESTAMP,
   END_TIME TIMESTAMP NOT NULL,
   STATUS VARCHAR(20),
   IS_247 BOOL,
   HAS_DATA BOOL,
   IS_ROLLED_BACK
   */
  String insert_prepared_statement_sql =
      "INSERT INTO VERIFICATION_WORKFLOW_STATS (ACCOUNT_ID, APP_ID, SERVICE_ID, WORKFLOW_ID, WORKFLOW_EXECUTION_ID,"
      + "STATE_EXECUTION_ID, CV_CONFIG_ID, STATE_TYPE, START_TIME, END_TIME, STATUS,"
      + "IS_247, HAS_DATA, IS_ROLLED_BACK) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

  @Inject private TimeScaleDBService timeScaleDBService;

  public void processEvent(Map<String, String> properties) {
    if (timeScaleDBService.isValid()) {
      Preconditions.checkNotNull(properties.get("accountId"));
      Preconditions.checkNotNull(properties.get("workflowExecutionId"));
      Preconditions.checkNotNull(properties.get("rollback"));

      PageRequest<ContinuousVerificationExecutionMetaData> cvPageRequest =
          aPageRequest()
              .addFilter("accountId", SearchFilter.Operator.IN, properties.get("accountId"))
              .addFilter("workflowExecutionId", SearchFilter.Operator.EQ, properties.get("workflowExecutionId"))
              .build();
      List<ContinuousVerificationExecutionMetaData> cvExecutionMetaDataList =
          continuousVerificationService.getCVDeploymentData(cvPageRequest);

      if (!isEmpty(cvExecutionMetaDataList)) {
        boolean rolledback = Boolean.valueOf(properties.get("rollback"));
        boolean successfulInsert = false;
        int retryCount = 0;
        long startTime = System.currentTimeMillis();
        while (!successfulInsert && retryCount < MAX_RETRY_COUNT) {
          try (Connection connection = timeScaleDBService.getDBConnection();
               PreparedStatement insertPreparedStatement = connection.prepareStatement(insert_prepared_statement_sql)) {
            for (ContinuousVerificationExecutionMetaData cvExecutionMetaData : cvExecutionMetaDataList) {
              insertPreparedStatement.setString(1, cvExecutionMetaData.getAccountId());
              insertPreparedStatement.setString(2, cvExecutionMetaData.getAppId());
              insertPreparedStatement.setString(3, cvExecutionMetaData.getServiceId());
              insertPreparedStatement.setString(4, cvExecutionMetaData.getWorkflowId());
              insertPreparedStatement.setString(5, cvExecutionMetaData.getWorkflowExecutionId());
              insertPreparedStatement.setString(6, cvExecutionMetaData.getStateExecutionId());
              insertPreparedStatement.setString(7, "");
              insertPreparedStatement.setString(8, cvExecutionMetaData.getStateType().getName());
              insertPreparedStatement.setString(11, cvExecutionMetaData.getExecutionStatus().name());
              insertPreparedStatement.setBoolean(12, false);
              insertPreparedStatement.setBoolean(13, !cvExecutionMetaData.isNoData());
              insertPreparedStatement.setBoolean(
                  14, rolledback && cvExecutionMetaData.getExecutionStatus() == ExecutionStatus.FAILED);
              insertPreparedStatement.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
              insertPreparedStatement.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
              insertPreparedStatement.addBatch();
            }
            insertPreparedStatement.executeBatch();
            successfulInsert = true;
          } catch (SQLException e) {
            if (retryCount >= MAX_RETRY_COUNT) {
              logger.error("Failed to save deployment data,[{}]", properties, e);
            } else {
              logger.info("Failed to save deployment data,[{}],retryCount=[{}]", properties, retryCount);
            }
            retryCount++;
          } finally {
            logger.info("Total time=[{}],retryCount=[{}]", System.currentTimeMillis() - startTime, retryCount);
          }
        }
      }
    } else {
      logger.trace("Not processing data:[{}]", properties);
    }
  }
}
