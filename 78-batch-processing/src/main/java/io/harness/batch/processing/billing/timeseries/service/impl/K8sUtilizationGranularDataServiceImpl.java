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
  private static final int BATCH_SIZE = 500;
  static final String INSERT_STATEMENT =
      "INSERT INTO KUBERNETES_UTILIZATION_DATA (STARTTIME, ENDTIME, CPU, MEMORY, INSTANCEID, INSTANCETYPE, CLUSTERID, ACCOUNTID) VALUES (?,?,?,?,?,?,?,?)";
  static final String SELECT_DISTINCT_INSTANCEID =
      "SELECT DISTINCT INSTANCEID FROM KUBERNETES_UTILIZATION_DATA WHERE ACCOUNTID = '%s' AND STARTTIME >= '%s' AND STARTTIME < '%s'";
  static final String UTILIZATION_DATA_QUERY =
      "SELECT MAX(CPU) as CPUUTILIZATIONMAX, MAX(MEMORY) as MEMORYUTILIZATIONMAX, AVG(CPU) as CPUUTILIZATIONAVG, AVG(MEMORY) as MEMORYUTILIZATIONAVG,"
      + " CLUSTERID, INSTANCEID, INSTANCETYPE FROM KUBERNETES_UTILIZATION_DATA WHERE ACCOUNTID = '%s' AND INSTANCEID IN ('%s') AND STARTTIME >= '%s' AND STARTTIME < '%s' "
      + " GROUP BY INSTANCEID, CLUSTERID, INSTANCETYPE ";

  public boolean create(List<K8sGranularUtilizationData> k8sGranularUtilizationDataList) {
    boolean successfulInsert = false;
    if (timeScaleDBService.isValid()) {
      int retryCount = 0;
      while (!successfulInsert && retryCount < MAX_RETRY_COUNT) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             PreparedStatement statement = dbConnection.prepareStatement(INSERT_STATEMENT)) {
          int index = 0;
          for (K8sGranularUtilizationData k8sGranularUtilizationData : k8sGranularUtilizationDataList) {
            updateInsertStatement(statement, k8sGranularUtilizationData);
            statement.addBatch();
            index++;

            if (index % BATCH_SIZE == 0 || index == k8sGranularUtilizationDataList.size()) {
              statement.executeBatch();
            }
          }
          successfulInsert = true;
        } catch (SQLException e) {
          logger.info(
              "Failed to save K8s Utilization data,[{}],retryCount=[{}]", k8sGranularUtilizationDataList, retryCount);
          retryCount++;
        }
      }
    } else {
      logger.warn("Not processing K8s Utilization data:[{}]", k8sGranularUtilizationDataList);
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
    statement.setString(7, k8sGranularUtilizationData.getClusterId());
    statement.setString(8, k8sGranularUtilizationData.getAccountId());
  }

  public List<String> getDistinctInstantIds(String accountId, long startDate, long endDate) {
    ResultSet resultSet = null;
    List<String> instanceIdsList = new ArrayList<>();

    String query = String.format(
        SELECT_DISTINCT_INSTANCEID, accountId, Instant.ofEpochMilli(startDate), Instant.ofEpochMilli(endDate));

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
      String accountId, List<String> distinctIdsList, long startDate, long endDate) {
    ResultSet resultSet = null;
    String query = String.format(UTILIZATION_DATA_QUERY, accountId, String.join("','", distinctIdsList),
        Instant.ofEpochMilli(startDate), Instant.ofEpochMilli(endDate));

    Map<String, InstanceUtilizationData> instanceUtilizationDataMap = new HashMap<>();
    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(query);
      while (resultSet.next()) {
        String instanceType = resultSet.getString("INSTANCETYPE");
        String instanceId = resultSet.getString("INSTANCEID");
        String clusterid = resultSet.getString("CLUSTERID");
        double cpuMax = resultSet.getDouble("CPUUTILIZATIONMAX");
        double memMax = resultSet.getDouble("MEMORYUTILIZATIONMAX");
        double cpuAvg = resultSet.getDouble("CPUUTILIZATIONAVG");
        double memAvg = resultSet.getDouble("MEMORYUTILIZATIONAVG");

        instanceUtilizationDataMap.put(instanceId,
            InstanceUtilizationData.builder()
                .accountId(accountId)
                .clusterId(clusterid)
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
