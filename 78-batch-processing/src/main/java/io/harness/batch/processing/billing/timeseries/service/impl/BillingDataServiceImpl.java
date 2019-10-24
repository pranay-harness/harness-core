package io.harness.batch.processing.billing.timeseries.service.impl;

import com.google.inject.Singleton;

import io.harness.batch.processing.billing.timeseries.data.InstanceBillingData;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.wings.graphql.datafetcher.DataFetcherUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

@Service
@Singleton
@Slf4j
public class BillingDataServiceImpl {
  @Autowired private TimeScaleDBService timeScaleDBService;
  @Autowired private DataFetcherUtils utils;

  private static final int MAX_RETRY_COUNT = 5;
  static final String INSERT_STATEMENT =
      "INSERT INTO BILLING_DATA (STARTTIME, ENDTIME, ACCOUNTID, INSTANCETYPE, BILLINGACCOUNTID, BILLINGAMOUNT, USAGEDURATIONSECONDS, INSTANCEID, CLUSTERID, SERVICEID, APPID, CLOUDPROVIDERID, ENVID, CPUUNITSECONDS, MEMORYMBSECONDS, PARENTINSTANCEID, REGION, LAUNCHTYPE, CLUSTERTYPE, CLOUDPROVIDER, WORKLOADNAME, WORKLOADTYPE) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

  public boolean create(InstanceBillingData instanceBillingData) {
    boolean successfulInsert = false;
    if (timeScaleDBService.isValid()) {
      long startTime = System.currentTimeMillis();
      int retryCount = 0;
      while (!successfulInsert && retryCount < MAX_RETRY_COUNT) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             PreparedStatement statement = dbConnection.prepareStatement(INSERT_STATEMENT)) {
          updateInsertStatement(statement, instanceBillingData);
          statement.execute();
          successfulInsert = true;

        } catch (SQLException e) {
          logger.info("Failed to save instance data,[{}],retryCount=[{}]", instanceBillingData, retryCount);
          retryCount++;
        } finally {
          logger.info("Total time=[{}]", System.currentTimeMillis() - startTime);
        }
      }
    } else {
      logger.info("Not processing instance billing data:[{}]", instanceBillingData);
    }
    return successfulInsert;
  }

  void updateInsertStatement(PreparedStatement statement, InstanceBillingData instanceBillingData) throws SQLException {
    statement.setTimestamp(1, new Timestamp(instanceBillingData.getStartTimestamp()), utils.getDefaultCalendar());
    statement.setTimestamp(2, new Timestamp(instanceBillingData.getEndTimestamp()), utils.getDefaultCalendar());
    statement.setString(3, instanceBillingData.getAccountId());
    statement.setString(4, instanceBillingData.getInstanceType());
    statement.setString(5, instanceBillingData.getBillingAccountId());
    statement.setBigDecimal(6, instanceBillingData.getBillingAmount());
    statement.setDouble(7, instanceBillingData.getUsageDurationSeconds());
    statement.setString(8, instanceBillingData.getInstanceId());
    statement.setString(9, instanceBillingData.getClusterName());
    statement.setString(10, instanceBillingData.getServiceId());
    statement.setString(11, instanceBillingData.getAppId());
    statement.setString(12, instanceBillingData.getCloudProviderId());
    statement.setString(13, instanceBillingData.getEnvId());
    statement.setDouble(14, instanceBillingData.getCpuUnitSeconds());
    statement.setDouble(15, instanceBillingData.getMemoryMbSeconds());
    statement.setString(16, instanceBillingData.getParentInstanceId());
    statement.setString(17, instanceBillingData.getRegion());
    statement.setString(18, instanceBillingData.getLaunchType());
    statement.setString(19, instanceBillingData.getClusterType());
    statement.setString(20, instanceBillingData.getCloudProvider());
    statement.setString(21, instanceBillingData.getWorkloadName());
    statement.setString(22, instanceBillingData.getWorkloadType());
  }
}
