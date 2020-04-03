package io.harness.batch.processing.events.timeseries.service.impl;

import io.harness.batch.processing.events.timeseries.data.CostEventData;
import io.harness.batch.processing.events.timeseries.service.intfc.CostEventService;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.wings.graphql.datafetcher.DataFetcherUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Slf4j
@Service
public class CostEventServiceImpl implements CostEventService {
  @Autowired private TimeScaleDBService timeScaleDBService;
  @Autowired private DataFetcherUtils utils;

  private static final int BATCH_SIZE = 500;
  private static final int MAX_RETRY_COUNT = 5;

  static final String INSERT_STATEMENT =
      "INSERT INTO COST_EVENT_DATA (STARTTIME, ACCOUNTID, SETTINGID, CLUSTERID, CLUSTERTYPE, INSTANCEID, INSTANCETYPE, "
      + "APPID, SERVICEID, ENVID, CLOUDPROVIDERID, DEPLOYMENTID, CLOUDPROVIDER, EVENTDESCRIPTION, COSTEVENTTYPE, "
      + "COSTEVENTSOURCE, NAMESPACE, WORKLOADNAME, WORKLOADTYPE, CLOUDSERVICENAME, TASKID, LAUNCHTYPE, BILLINGAMOUNT, "
      + "OLDYAMLREF, NEWYAMLREF) "
      + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING ";

  static final String UPDATE_DEPLOYMENT_STATEMENT =
      "UPDATE COST_EVENT_DATA SET SETTINGID=?, CLUSTERID=?, CLUSTERTYPE=?, CLOUDPROVIDER=?, NAMESPACE=?, WORKLOADNAME=?,"
      + "WORKLOADTYPE=?, CLOUDSERVICENAME=?, TASKID=?, LAUNCHTYPE=? WHERE DEPLOYMENTID=? AND CLUSTERID IS NULL ";

  @Override
  public boolean create(List<CostEventData> costEventDataList) {
    boolean successfulInsert = false;
    if (timeScaleDBService.isValid()) {
      int retryCount = 0;
      while (!successfulInsert && retryCount < MAX_RETRY_COUNT) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             PreparedStatement statement = dbConnection.prepareStatement(INSERT_STATEMENT)) {
          int index = 0;
          for (CostEventData costEventData : costEventDataList) {
            updateInsertStatement(statement, costEventData);
            statement.addBatch();
            index++;

            if (index % BATCH_SIZE == 0 || index == costEventDataList.size()) {
              logger.debug("statement is {}", statement);
              statement.executeBatch();
            }
          }
          successfulInsert = true;
        } catch (SQLException e) {
          logger.error("Failed to save cost event retryCount=[{}]", retryCount);
          retryCount++;
        }
      }
    } else {
      logger.warn("Not processing cost event data:[{}]", costEventDataList.size());
    }
    return successfulInsert;
  }

  @Override
  public boolean updateDeploymentEvent(CostEventData costEventData) {
    boolean successfulUpdate = false;
    int retryCount = 0;
    while (!successfulUpdate && retryCount < MAX_RETRY_COUNT) {
      try (Connection dbConnection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = dbConnection.prepareStatement(UPDATE_DEPLOYMENT_STATEMENT)) {
        updateDeploymentStatement(statement, costEventData);
        logger.debug("deployment update {}", statement);
        statement.executeUpdate();
        successfulUpdate = true;
      } catch (SQLException e) {
        logger.error("Failed to update deployment cost event retryCount=[{}]", retryCount);
        retryCount++;
      }
    }
    return successfulUpdate;
  }

  private void updateDeploymentStatement(PreparedStatement statement, CostEventData costEventData) throws SQLException {
    statement.setString(1, costEventData.getSettingId());
    statement.setString(2, costEventData.getClusterId());
    statement.setString(3, costEventData.getClusterType());
    statement.setString(4, costEventData.getCloudProvider());
    statement.setString(5, costEventData.getNamespace());
    statement.setString(6, costEventData.getWorkloadName());
    statement.setString(7, costEventData.getWorkloadType());
    statement.setString(8, costEventData.getCloudServiceName());
    statement.setString(9, costEventData.getTaskId());
    statement.setString(10, costEventData.getLaunchType());
    statement.setString(11, costEventData.getDeploymentId());
  }

  private void updateInsertStatement(PreparedStatement statement, CostEventData costEventData) throws SQLException {
    statement.setTimestamp(1, new Timestamp(costEventData.getStartTimestamp()), utils.getDefaultCalendar());
    statement.setString(2, costEventData.getAccountId());
    statement.setString(3, costEventData.getSettingId());
    statement.setString(4, costEventData.getClusterId());
    statement.setString(5, costEventData.getClusterType());
    statement.setString(6, costEventData.getInstanceId());
    statement.setString(7, costEventData.getInstanceType());
    statement.setString(8, costEventData.getAppId());
    statement.setString(9, costEventData.getServiceId());
    statement.setString(10, costEventData.getEnvId());
    statement.setString(11, costEventData.getCloudProviderId());
    statement.setString(12, costEventData.getDeploymentId());
    statement.setString(13, costEventData.getCloudProvider());
    statement.setString(14, costEventData.getEventDescription());
    statement.setString(15, costEventData.getCostEventType());
    statement.setString(16, costEventData.getCostEventSource());
    statement.setString(17, costEventData.getNamespace());
    statement.setString(18, costEventData.getWorkloadName());
    statement.setString(19, costEventData.getWorkloadType());
    statement.setString(20, costEventData.getCloudServiceName());
    statement.setString(21, costEventData.getTaskId());
    statement.setString(22, costEventData.getLaunchType());
    statement.setBigDecimal(23, costEventData.getBillingAmount());
    statement.setString(24, costEventData.getOldYamlRef());
    statement.setString(25, costEventData.getNewYamlRef());
  }
}
