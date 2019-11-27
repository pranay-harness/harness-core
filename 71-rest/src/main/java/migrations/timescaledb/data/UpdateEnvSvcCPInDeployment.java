package migrations.timescaledb.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.utils.Lists;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import migrations.TimeScaleDBDataMigration;
import software.wings.beans.Base;
import software.wings.beans.Environment;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowExecutionService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class UpdateEnvSvcCPInDeployment implements TimeScaleDBDataMigration {
  @Inject TimeScaleDBService timeScaleDBService;

  @Inject WingsPersistence wingsPersistence;
  @Inject WorkflowExecutionService workflowExecutionService;

  private static final int MAX_RETRY = 5;

  private static final long DAY_IN_MILLIS = 24 * 3600 * 1000L;

  // We will process executions that are 31 days old.
  private static final long NUM_OF_DAYS = 31;

  private static final String DEPLOYMENT_QUERY =
      "SELECT EXECUTIONID,APPID FROM DEPLOYMENT WHERE ENDTIME >=? AND ENDTIME <=? AND PIPELINE IS NOT NULL";

  private static final String UPDATE_STATEMENT =
      "UPDATE DEPLOYMENT SET ENVIRONMENTS=?,ENVTYPES=?,CLOUDPROVIDERS=?,SERVICES=? where EXECUTIONID=?";

  @Override
  public boolean migrate() {
    if (!timeScaleDBService.isValid()) {
      logger.info("TimeScaleDB not found, not migrating deployment data to TimeScaleDB");
      return false;
    }

    try {
      long queryEndTime = System.currentTimeMillis();
      long queryStartTime = queryEndTime - DAY_IN_MILLIS;
      long endTime = queryEndTime - NUM_OF_DAYS * DAY_IN_MILLIS;
      while (queryEndTime > endTime) {
        // perform migration for the day
        logger.info("Performing migration for [{}]-[{}]", new Date(queryStartTime), new Date(queryEndTime));
        performMigration(queryStartTime, queryEndTime);
        queryEndTime = queryStartTime;
        queryStartTime = queryEndTime - DAY_IN_MILLIS;
      }
      logger.info("Successfully migrated Env, CloudProvider and Services");
      return true;
    } catch (Exception e) {
      logger.error("migrate:Failed while performing migration", e);
      return false;
    }
  }

  private void performMigration(long queryStartTime, long queryEndTime) {
    int retryCount = 0;
    ResultSet resultSet = null;
    boolean success = false;
    while (!success && retryCount < MAX_RETRY) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement deploymentQueryStatement = connection.prepareStatement(DEPLOYMENT_QUERY)) {
        deploymentQueryStatement.setTimestamp(1, new Timestamp(queryStartTime), Calendar.getInstance());
        deploymentQueryStatement.setTimestamp(2, new Timestamp(queryEndTime), Calendar.getInstance());
        ResultSet queryResultSet = deploymentQueryStatement.executeQuery();
        List<WfData> wfDataList = new ArrayList<>();
        while (queryResultSet.next()) {
          wfDataList.add(
              WfData.builder().executionId(queryResultSet.getString(1)).appId(queryResultSet.getString(2)).build());
        }
        if (!Lists.isNullOrEmpty(wfDataList)) {
          populateHolderList(wfDataList);
          updateHolderList(wfDataList);
        }
        success = true;

      } catch (SQLException e) {
        if (retryCount >= MAX_RETRY) {
          logger.error(
              "Failed to performMigration query for [{}]-[{}]", new Date(queryStartTime), new Date(queryEndTime), e);
          throw new TimeScaleDBMigrationException(e);
        } else {
          logger.warn("Failed trying to performMigration query for [{}]-[{}], retryCount:[{}]",
              new Date(queryStartTime), new Date(queryEndTime), retryCount);
        }
        retryCount++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
  }

  private void updateHolderList(List<WfData> wfDataList) {
    int retryCount = 0;
    boolean success = false;
    while (!success && retryCount < MAX_RETRY) {
      for (WfData wfData : wfDataList) {
        try (Connection connection = timeScaleDBService.getDBConnection();
             PreparedStatement updateStatement = connection.prepareStatement(UPDATE_STATEMENT)) {
          if (wfData.isValid()) {
            List<String> envIds = wfData.getEnvironments().stream().map(Base::getUuid).collect(Collectors.toList());
            Set<String> envTypes = wfData.getEnvironments()
                                       .stream()
                                       .map(env -> env.getEnvironmentType().name())
                                       .collect(Collectors.toSet());

            if (!Lists.isNullOrEmpty(envIds)) {
              updateStatement.setArray(1, connection.createArrayOf("text", envIds.toArray()));
            } else {
              updateStatement.setArray(1, null);
            }
            if (null != envTypes && envTypes.size() > 0) {
              updateStatement.setArray(2, connection.createArrayOf("text", envTypes.toArray()));
            } else {
              updateStatement.setArray(2, null);
            }

            if (!Lists.isNullOrEmpty(wfData.getCloudProviders())) {
              List<String> cloudProviderIds = wfData.getCloudProviders();
              updateStatement.setArray(3, connection.createArrayOf("text", cloudProviderIds.toArray()));
            } else {
              updateStatement.setArray(3, null);
            }

            if (!Lists.isNullOrEmpty(wfData.getServices())) {
              List<String> services = wfData.getServices();
              updateStatement.setArray(4, connection.createArrayOf("text", services.toArray()));
            } else {
              updateStatement.setArray(4, null);
            }

            updateStatement.setString(5, wfData.getExecutionId());

            updateStatement.execute();
          }
        } catch (SQLException e) {
          if (retryCount >= MAX_RETRY) {
            logger.error("Failed to update WFData:[{}]", wfData.getExecutionId());
            throw new TimeScaleDBMigrationException(e);
          } else {
            logger.warn("Failed to update WFData:[{}], retryCount:[{}]", wfData, retryCount);
          }
          retryCount++;
        }
      }
      success = true;
    }
  }

  private void populateHolderList(List<WfData> wfDataList) {
    for (WfData wfData : wfDataList) {
      WorkflowExecution workflowExecution =
          workflowExecutionService.getWorkflowExecution(wfData.getAppId(), wfData.getExecutionId());
      if (workflowExecution != null) {
        wfData.setValid(true);
        wfData.setCloudProviders(workflowExecutionService.getCloudProviderIdsForExecution(workflowExecution));
        wfData.setEnvironments(workflowExecutionService.getEnvironmentsForExecution(workflowExecution));
        wfData.setServices(workflowExecutionService.getServiceIdsForExecution(workflowExecution));
      }
    }
  }

  @Data
  @Builder
  @ToString
  static class WfData {
    private boolean valid;
    private List<Environment> environments;
    private List<String> cloudProviders;
    private List<String> services;
    private String appId;
    private String executionId;
  }
}
