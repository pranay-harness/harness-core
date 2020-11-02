package io.harness.batch.processing.billing.timeseries.service.impl;

import com.google.inject.Singleton;

import io.harness.batch.processing.billing.timeseries.data.InstanceBillingData;
import io.harness.batch.processing.billing.timeseries.service.support.BillingDataTableNameProvider;
import io.harness.batch.processing.ccm.ActualIdleCostWriterData;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.ccm.commons.utils.DataUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Singleton
@Slf4j
public class BillingDataServiceImpl {
  @Autowired private TimeScaleDBService timeScaleDBService;
  @Autowired private DataUtils utils;

  private static final int BATCH_SIZE = 500;
  private static final int MAX_RETRY_COUNT = 2;
  private static final int SELECT_MAX_RETRY_COUNT = 5;
  static final String INSERT_STATEMENT =
      "INSERT INTO %s (STARTTIME, ENDTIME, ACCOUNTID, INSTANCETYPE, BILLINGACCOUNTID, BILLINGAMOUNT, CPUBILLINGAMOUNT, MEMORYBILLINGAMOUNT, USAGEDURATIONSECONDS, INSTANCEID, CLUSTERNAME, CLUSTERID, SETTINGID,  SERVICEID, APPID, CLOUDPROVIDERID, ENVID, CPUUNITSECONDS, MEMORYMBSECONDS, PARENTINSTANCEID, REGION, LAUNCHTYPE, CLUSTERTYPE, CLOUDPROVIDER, WORKLOADNAME, WORKLOADTYPE, NAMESPACE, CLOUDSERVICENAME, TASKID, IDLECOST, CPUIDLECOST, MEMORYIDLECOST, MAXCPUUTILIZATION, MAXMEMORYUTILIZATION, AVGCPUUTILIZATION, AVGMEMORYUTILIZATION, SYSTEMCOST, CPUSYSTEMCOST, MEMORYSYSTEMCOST, ACTUALIDLECOST, CPUACTUALIDLECOST, MEMORYACTUALIDLECOST, UNALLOCATEDCOST, CPUUNALLOCATEDCOST, MEMORYUNALLOCATEDCOST, INSTANCENAME, CPUREQUEST, MEMORYREQUEST, CPULIMIT, MEMORYLIMIT, MAXCPUUTILIZATIONVALUE, MAXMEMORYUTILIZATIONVALUE, AVGCPUUTILIZATIONVALUE, AVGMEMORYUTILIZATIONVALUE, NETWORKCOST, PRICINGSOURCE ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING";

  static final String UPDATE_STATEMENT =
      "UPDATE %s SET ACTUALIDLECOST = ?, CPUACTUALIDLECOST = ?, MEMORYACTUALIDLECOST = ?, UNALLOCATEDCOST = ?, CPUUNALLOCATEDCOST = ?, MEMORYUNALLOCATEDCOST = ? WHERE ACCOUNTID = ? AND CLUSTERID = ? AND INSTANCEID = ? AND STARTTIME = ?";

  static final String PURGE_DATA_QUERY = "SELECT drop_chunks(interval '16 days', 'billing_data_hourly')";

  private static final String READER_QUERY =
      "SELECT * FROM BILLING_DATA WHERE ACCOUNTID = '%s' AND STARTTIME >= '%s' AND STARTTIME < '%s' OFFSET %s LIMIT %s;";

  public boolean create(List<InstanceBillingData> instanceBillingDataList, BatchJobType batchJobType) {
    boolean successfulInsert = false;
    if (timeScaleDBService.isValid() && !instanceBillingDataList.isEmpty()) {
      String insertStatement = BillingDataTableNameProvider.replaceTableName(INSERT_STATEMENT, batchJobType);
      int retryCount = 0;
      while (!successfulInsert && retryCount < MAX_RETRY_COUNT) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             PreparedStatement statement = dbConnection.prepareStatement(insertStatement)) {
          int index = 0;
          for (InstanceBillingData instanceBillingData : instanceBillingDataList) {
            updateInsertStatement(statement, instanceBillingData);
            statement.addBatch();
            index++;

            if (index % BATCH_SIZE == 0 || index == instanceBillingDataList.size()) {
              log.debug("Prepared Statement in BillingDataServiceImpl: {} ", statement);
              statement.executeBatch();
            }
          }
          successfulInsert = true;
        } catch (SQLException e) {
          log.error("Failed to save instance data,[{}],retryCount=[{}], Exception: ", instanceBillingDataList.size(),
              retryCount, e);
          retryCount++;
        }
      }
    } else {
      log.warn("Not processing instance billing data:[{}]", instanceBillingDataList.size());
    }
    return successfulInsert;
  }

  public boolean purgeOldHourlyBillingData() {
    boolean purgedHourlyBillingData = false;
    log.info("Purging old hourly billing data !!");
    if (timeScaleDBService.isValid()) {
      int retryCount = 0;
      while (retryCount < MAX_RETRY_COUNT && !purgedHourlyBillingData) {
        try (Connection connection = timeScaleDBService.getDBConnection();
             Statement statement = connection.createStatement()) {
          statement.execute(PURGE_DATA_QUERY);
          purgedHourlyBillingData = true;
        } catch (SQLException e) {
          log.error("Failed to execute query=[{}]", PURGE_DATA_QUERY, e);
          retryCount++;
        }
      }
    }
    return purgedHourlyBillingData;
  }

  public boolean update(ActualIdleCostWriterData actualIdleCostWriterData, BatchJobType batchJobType) {
    boolean successfulUpdate = false;
    if (timeScaleDBService.isValid()) {
      int retryCount = 0;
      String updateStatement = BillingDataTableNameProvider.replaceTableName(UPDATE_STATEMENT, batchJobType);
      while (!successfulUpdate && retryCount < MAX_RETRY_COUNT) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             PreparedStatement statement = dbConnection.prepareStatement(updateStatement)) {
          updateBillingDataUpdateStatement(statement, actualIdleCostWriterData);
          log.debug("Prepared Statement in BillingDataServiceImpl for actual idle cost: {} ", statement);
          statement.execute();
          successfulUpdate = true;
        } catch (SQLException e) {
          log.error("Failed to update actual idle cost data,[{}],retryCount=[{}], Exception: ",
              actualIdleCostWriterData, retryCount, e);
          retryCount++;
        }
      }
    } else {
      log.warn("Not processing actual idle cost data:[{}]", actualIdleCostWriterData);
    }
    return successfulUpdate;
  }

  void updateBillingDataUpdateStatement(PreparedStatement statement, ActualIdleCostWriterData actualIdleCostWriterData)
      throws SQLException {
    statement.setBigDecimal(1, actualIdleCostWriterData.getActualIdleCost());
    statement.setBigDecimal(2, actualIdleCostWriterData.getCpuActualIdleCost());
    statement.setBigDecimal(3, actualIdleCostWriterData.getMemoryActualIdleCost());
    statement.setBigDecimal(4, actualIdleCostWriterData.getUnallocatedCost());
    statement.setBigDecimal(5, actualIdleCostWriterData.getCpuUnallocatedCost());
    statement.setBigDecimal(6, actualIdleCostWriterData.getMemoryUnallocatedCost());
    statement.setString(7, actualIdleCostWriterData.getAccountId());
    statement.setString(8, actualIdleCostWriterData.getClusterId());
    statement.setString(9, actualIdleCostWriterData.getInstanceId());
    statement.setTimestamp(10, new Timestamp(actualIdleCostWriterData.getStartTime()), utils.getDefaultCalendar());
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
    statement.setString(29, instanceBillingData.getTaskId());
    statement.setBigDecimal(30, instanceBillingData.getIdleCost());
    statement.setBigDecimal(31, instanceBillingData.getCpuIdleCost());
    statement.setBigDecimal(32, instanceBillingData.getMemoryIdleCost());
    statement.setDouble(33, instanceBillingData.getMaxCpuUtilization());
    statement.setDouble(34, instanceBillingData.getMaxMemoryUtilization());
    statement.setDouble(35, instanceBillingData.getAvgCpuUtilization());
    statement.setDouble(36, instanceBillingData.getAvgMemoryUtilization());
    statement.setBigDecimal(37, instanceBillingData.getSystemCost());
    statement.setBigDecimal(38, instanceBillingData.getCpuSystemCost());
    statement.setBigDecimal(39, instanceBillingData.getMemorySystemCost());
    statement.setBigDecimal(40, instanceBillingData.getActualIdleCost());
    statement.setBigDecimal(41, instanceBillingData.getCpuActualIdleCost());
    statement.setBigDecimal(42, instanceBillingData.getMemoryActualIdleCost());
    statement.setBigDecimal(43, instanceBillingData.getUnallocatedCost());
    statement.setBigDecimal(44, instanceBillingData.getCpuUnallocatedCost());
    statement.setBigDecimal(45, instanceBillingData.getMemoryUnallocatedCost());
    statement.setString(46, instanceBillingData.getInstanceName());
    statement.setDouble(47, instanceBillingData.getCpuRequest());
    statement.setDouble(48, instanceBillingData.getMemoryRequest());
    statement.setDouble(49, instanceBillingData.getCpuLimit());
    statement.setDouble(50, instanceBillingData.getMemoryLimit());
    statement.setDouble(51, instanceBillingData.getMaxCpuUtilizationValue());
    statement.setDouble(52, instanceBillingData.getMaxMemoryUtilizationValue());
    statement.setDouble(53, instanceBillingData.getAvgCpuUtilizationValue());
    statement.setDouble(54, instanceBillingData.getAvgMemoryUtilizationValue());
    statement.setDouble(55, instanceBillingData.getNetworkCost());
    statement.setString(56, instanceBillingData.getPricingSource());
  }

  public List<InstanceBillingData> read(
      String accountId, Instant startTime, Instant endTime, int batchSize, int offset) {
    try {
      if (timeScaleDBService.isValid()) {
        String query =
            String.format(READER_QUERY, accountId, startTime.toString(), endTime.toString(), offset, batchSize);

        return getUtilizationDataFromTimescaleDB(query);
      } else {
        throw new InvalidRequestException("Cannot process request in ClusterDataToBigQueryTasklet");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching Instance Billing data {}", e);
    }
  }

  private List<InstanceBillingData> getUtilizationDataFromTimescaleDB(String query) {
    ResultSet resultSet = null;
    List<InstanceBillingData> instanceBillingDataList = new ArrayList<>();
    int retryCount = 0;
    log.debug("ClusterDataToBigQueryTasklet read data query : {}", query);
    while (retryCount < SELECT_MAX_RETRY_COUNT) {
      retryCount++;
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        resultSet = statement.executeQuery(query);
        while (resultSet.next()) {
          instanceBillingDataList.add(
              InstanceBillingData.builder()
                  .endTimestamp(resultSet.getTimestamp("ENDTIME").toInstant().toEpochMilli())
                  .startTimestamp(resultSet.getTimestamp("STARTTIME").toInstant().toEpochMilli())
                  .accountId(resultSet.getString("ACCOUNTID"))
                  .instanceType(resultSet.getString("INSTANCETYPE"))
                  .billingAccountId(resultSet.getString("BILLINGACCOUNTID"))
                  .billingAmount(resultSet.getBigDecimal("BILLINGAMOUNT"))
                  .cpuBillingAmount(resultSet.getBigDecimal("CPUBILLINGAMOUNT"))
                  .memoryBillingAmount(resultSet.getBigDecimal("MEMORYBILLINGAMOUNT"))
                  .usageDurationSeconds(resultSet.getDouble("USAGEDURATIONSECONDS"))
                  .instanceId(resultSet.getString("INSTANCEID"))
                  .clusterName(resultSet.getString("CLUSTERNAME"))
                  .clusterId(resultSet.getString("CLUSTERID"))
                  .settingId(resultSet.getString("SETTINGID"))
                  .serviceId(resultSet.getString("SERVICEID"))
                  .appId(resultSet.getString("APPID"))
                  .cloudProviderId(resultSet.getString("CLOUDPROVIDERID"))
                  .envId(resultSet.getString("ENVID"))
                  .cpuUnitSeconds(resultSet.getDouble("CPUUNITSECONDS"))
                  .memoryMbSeconds(resultSet.getDouble("MEMORYMBSECONDS"))
                  .parentInstanceId(resultSet.getString("PARENTINSTANCEID"))
                  .region(resultSet.getString("REGION"))
                  .launchType(resultSet.getString("LAUNCHTYPE"))
                  .clusterType(resultSet.getString("CLUSTERTYPE"))
                  .cloudProvider(resultSet.getString("CLOUDPROVIDER"))
                  .workloadName(resultSet.getString("WORKLOADNAME"))
                  .workloadType(resultSet.getString("WORKLOADTYPE"))
                  .namespace(resultSet.getString("NAMESPACE"))
                  .cloudServiceName(resultSet.getString("CLOUDSERVICENAME"))
                  .taskId(resultSet.getString("TASKID"))
                  .idleCost(resultSet.getBigDecimal("IDLECOST"))
                  .cpuIdleCost(resultSet.getBigDecimal("CPUIDLECOST"))
                  .memoryIdleCost(resultSet.getBigDecimal("MEMORYIDLECOST"))
                  .maxCpuUtilization(resultSet.getDouble("MAXCPUUTILIZATION"))
                  .maxMemoryUtilization(resultSet.getDouble("MAXMEMORYUTILIZATION"))
                  .avgCpuUtilization(resultSet.getDouble("AVGCPUUTILIZATION"))
                  .avgMemoryUtilization(resultSet.getDouble("AVGMEMORYUTILIZATION"))
                  .systemCost(resultSet.getBigDecimal("SYSTEMCOST"))
                  .cpuSystemCost(resultSet.getBigDecimal("CPUSYSTEMCOST"))
                  .memorySystemCost(resultSet.getBigDecimal("MEMORYSYSTEMCOST"))
                  .actualIdleCost(resultSet.getBigDecimal("ACTUALIDLECOST"))
                  .cpuActualIdleCost(resultSet.getBigDecimal("CPUACTUALIDLECOST"))
                  .memoryActualIdleCost(resultSet.getBigDecimal("MEMORYACTUALIDLECOST"))
                  .unallocatedCost(resultSet.getBigDecimal("UNALLOCATEDCOST"))
                  .cpuUnallocatedCost(resultSet.getBigDecimal("CPUUNALLOCATEDCOST"))
                  .memoryUnallocatedCost(resultSet.getBigDecimal("MEMORYUNALLOCATEDCOST"))
                  .instanceName(resultSet.getString("INSTANCENAME"))
                  .cpuRequest(resultSet.getDouble("CPUREQUEST"))
                  .memoryRequest(resultSet.getDouble("MEMORYREQUEST"))
                  .cpuLimit(resultSet.getDouble("CPULIMIT"))
                  .memoryLimit(resultSet.getDouble("MEMORYLIMIT"))
                  .maxCpuUtilizationValue(resultSet.getDouble("MAXCPUUTILIZATIONVALUE"))
                  .maxMemoryUtilizationValue(resultSet.getDouble("MAXMEMORYUTILIZATIONVALUE"))
                  .avgCpuUtilizationValue(resultSet.getDouble("AVGCPUUTILIZATIONVALUE"))
                  .avgMemoryUtilizationValue(resultSet.getDouble("AVGMEMORYUTILIZATIONVALUE"))
                  .networkCost(resultSet.getDouble("NETWORKCOST"))
                  .pricingSource(resultSet.getString("PRICINGSOURCE"))
                  .build());
        }
        return instanceBillingDataList;
      } catch (SQLException e) {
        log.error("Error while fetching billing Data data : exception", e);
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return null;
  }
}
