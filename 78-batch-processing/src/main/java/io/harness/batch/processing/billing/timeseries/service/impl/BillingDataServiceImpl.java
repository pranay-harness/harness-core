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
      "INSERT INTO BILLING_DATA (STARTTIME, ENDTIME, ACCOUNTID, INSTANCETYPE, BILLINGACCOUNTID, BILLINGAMOUNT, CPUBILLINGAMOUNT, MEMORYBILLINGAMOUNT, USAGEDURATIONSECONDS, INSTANCEID, CLUSTERNAME, CLUSTERID, SETTINGID,  SERVICEID, APPID, CLOUDPROVIDERID, ENVID, CPUUNITSECONDS, MEMORYMBSECONDS, PARENTINSTANCEID, REGION, LAUNCHTYPE, CLUSTERTYPE, CLOUDPROVIDER, WORKLOADNAME, WORKLOADTYPE, NAMESPACE, CLOUDSERVICENAME, IDLECOST, CPUIDLECOST, MEMORYIDLECOST, MAXCPUUTILIZATION, MAXMEMORYUTILIZATION, AVGCPUUTILIZATION, AVGMEMORYUTILIZATION ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

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
          logger.error("Failed to save instance data,[{}],retryCount=[{}]", instanceBillingData, retryCount);
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
    statement.setBigDecimal(7, instanceBillingData.getCpuBillingAmount());
    statement.setBigDecimal(8, instanceBillingData.getMemoryBillingAmount());
    statement.setDouble(9, instanceBillingData.getUsageDurationSeconds());
    statement.setString(10, instanceBillingData.getInstanceId());
    statement.setString(11, instanceBillingData.getClusterName());
    statement.setString(12, instanceBillingData.getClusterId());
    statement.setString(13, instanceBillingData.getSettingId());
    statement.setString(14, instanceBillingData.getServiceId());
    statement.setString(15, instanceBillingData.getAppId());
    statement.setString(16, instanceBillingData.getCloudProviderId());
    statement.setString(17, instanceBillingData.getEnvId());
    statement.setDouble(18, instanceBillingData.getCpuUnitSeconds());
    statement.setDouble(19, instanceBillingData.getMemoryMbSeconds());
    statement.setString(20, instanceBillingData.getParentInstanceId());
    statement.setString(21, instanceBillingData.getRegion());
    statement.setString(22, instanceBillingData.getLaunchType());
    statement.setString(23, instanceBillingData.getClusterType());
    statement.setString(24, instanceBillingData.getCloudProvider());
    statement.setString(25, instanceBillingData.getWorkloadName());
    statement.setString(26, instanceBillingData.getWorkloadType());
    statement.setString(27, instanceBillingData.getNamespace());
    statement.setString(28, instanceBillingData.getCloudServiceName());
    statement.setBigDecimal(29, instanceBillingData.getIdleCost());
    statement.setBigDecimal(30, instanceBillingData.getCpuIdleCost());
    statement.setBigDecimal(31, instanceBillingData.getMemoryIdleCost());
    statement.setDouble(32, instanceBillingData.getMaxCpuUtilization());
    statement.setDouble(33, instanceBillingData.getMaxMemoryUtilization());
    statement.setDouble(34, instanceBillingData.getAvgCpuUtilization());
    statement.setDouble(35, instanceBillingData.getAvgMemoryUtilization());
  }
}
