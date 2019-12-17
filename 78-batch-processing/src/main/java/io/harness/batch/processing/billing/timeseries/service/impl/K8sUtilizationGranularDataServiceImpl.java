package io.harness.batch.processing.billing.timeseries.service.impl;

import com.google.inject.Singleton;

import io.harness.batch.processing.billing.timeseries.data.InstanceUtilizationData;
import io.harness.batch.processing.billing.timeseries.data.K8sGranularUtilizationData;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.wings.graphql.datafetcher.DataFetcherUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Singleton
@Slf4j
public class K8sUtilizationGranularDataServiceImpl {
  @Autowired private TimeScaleDBService timeScaleDBService;
  @Autowired private DataFetcherUtils utils;

  private static final int MAX_RETRY_COUNT = 5;
  static final String INSERT_STATEMENT =
      "INSERT INTO KUBERNETES_UTILIZATION_DATA (STARTTIME, ENDTIME, CPU, MEMORY, INSTANCEID, INSTANCETYPE, SETTINGID, ACCOUNTID) VALUES (?,?,?,?,?,?,?,?)";
  static final String SELECT_DISTINCT_INSTANCEID =
      "SELECT DISTINCT INSTANCEID FROM KUBERNETES_UTILIZATION_DATA WHERE STARTTIME >= '%s' AND ENDTIME <= '%s'";
  static final String UTILIZATION_DATA_QUERY =
      "SELECT MAX(CPU) as CPUUTILIZATIONMAX, MAX(MEMORY) as MEMORYUTILIZATIONMAX, AVG(CPU) as CPUUTILIZATIONAVG, AVG(MEMORY) as MEMORYUTILIZATIONAVG,"
      + " SETTINGID, INSTANCEID,  INSTANCETYPE, ACCOUNTID FROM KUBERNETES_UTILIZATION_DATA WHERE INSTANCEID IN ('%s') AND STARTTIME >= '%s' AND ENDTIME <= '%s' "
      + " GROUP BY ACCOUNTID, INSTANCEID, SETTINGID, INSTANCETYPE ";

  public boolean create(K8sGranularUtilizationData k8sGranularUtilizationData) {
    boolean successfulInsert = false;
    if (timeScaleDBService.isValid()) {
      long startTime = System.currentTimeMillis();
      int retryCount = 0;
      while (!successfulInsert && retryCount < MAX_RETRY_COUNT) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             PreparedStatement statement = dbConnection.prepareStatement(INSERT_STATEMENT)) {
          updateInsertStatement(statement, k8sGranularUtilizationData);
          statement.execute();
          successfulInsert = true;

        } catch (SQLException e) {
          logger.info(
              "Failed to save instance Utilization data,[{}],retryCount=[{}]", k8sGranularUtilizationData, retryCount);
          retryCount++;
        } finally {
          logger.info("Total time=[{}]", System.currentTimeMillis() - startTime);
        }
      }
    } else {
      logger.info("Not processing instance Utilization data:[{}]", k8sGranularUtilizationData);
    }
    return successfulInsert;
  }

  private void updateInsertStatement(PreparedStatement statement, K8sGranularUtilizationData k8sGranularUtilizationData)
      throws SQLException {
    statement.setTimestamp(
        1, new Timestamp(k8sGranularUtilizationData.getStartTimestamp()), utils.getDefaultCalendar());
    statement.setTimestamp(2, new Timestamp(k8sGranularUtilizationData.getEndTimestamp()), utils.getDefaultCalendar());
    statement.setDouble(3, k8sGranularUtilizationData.getCpu());
    statement.setDouble(4, k8sGranularUtilizationData.getMemory());
    statement.setString(5, k8sGranularUtilizationData.getInstanceId());
    statement.setString(6, k8sGranularUtilizationData.getInstanceType());
    statement.setString(7, k8sGranularUtilizationData.getSettingId());
    statement.setString(8, k8sGranularUtilizationData.getAccountId());
  }

  public List<String> getDistinctInstantIds(long startDate, long endDate) {
    ResultSet resultSet = null;
    List<String> instanceIdsList = new ArrayList<>();

    String query =
        String.format(SELECT_DISTINCT_INSTANCEID, Instant.ofEpochMilli(startDate), Instant.ofEpochMilli(endDate));

    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(query);
      while (resultSet.next()) {
        instanceIdsList.add(resultSet.getString("INSTANCEID"));
      }
      return instanceIdsList;
    } catch (SQLException e) {
      logger.error("Error while fetching instanceIds List : exception", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return null;
  }

  public Map<String, InstanceUtilizationData> getAggregatedUtilizationData(
      List<String> distinctIdsList, long startDate, long endDate) {
    ResultSet resultSet = null;
    String query = String.format(UTILIZATION_DATA_QUERY, String.join("','", distinctIdsList),
        Instant.ofEpochMilli(startDate), Instant.ofEpochMilli(endDate));

    Map<String, InstanceUtilizationData> instanceUtilizationDataMap = new HashMap<>();
    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(query);
      while (resultSet.next()) {
        String instanceType = resultSet.getString("INSTANCETYPE");
        String instanceId = resultSet.getString("INSTANCEID");
        String settingId = resultSet.getString("SETTINGID");
        String accountId = resultSet.getString("ACCOUNTID");
        double cpuMax = resultSet.getDouble("CPUUTILIZATIONMAX");
        double memMax = resultSet.getDouble("MEMORYUTILIZATIONMAX");
        double cpuAvg = resultSet.getDouble("CPUUTILIZATIONAVG");
        double memAvg = resultSet.getDouble("MEMORYUTILIZATIONAVG");

        instanceUtilizationDataMap.put(instanceId,
            InstanceUtilizationData.builder()
                .accountId(accountId)
                .settingId(settingId)
                .instanceType(instanceType)
                .instanceId(instanceId)
                .cpuUtilizationMax(cpuMax)
                .cpuUtilizationAvg(cpuAvg)
                .memoryUtilizationMax(memMax)
                .memoryUtilizationAvg(memAvg)
                .build());
      }
      return instanceUtilizationDataMap;
    } catch (SQLException e) {
      logger.error("Error while fetching Aggregated Utilization Data : exception ", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return null;
  }
}
