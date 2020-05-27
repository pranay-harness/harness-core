package io.harness.batch.processing.billing.timeseries.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Singleton;

import io.harness.batch.processing.billing.service.UtilizationData;
import io.harness.batch.processing.billing.timeseries.data.InstanceUtilizationData;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.exception.InvalidRequestException;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Service
@Singleton
@Slf4j
public class UtilizationDataServiceImpl {
  @Autowired private TimeScaleDBService timeScaleDBService;
  @Autowired private DataFetcherUtils utils;

  private static final int MAX_RETRY_COUNT = 2;
  private static final int BATCH_SIZE = 500;

  static final String INSERT_STATEMENT =
      "INSERT INTO UTILIZATION_DATA (STARTTIME, ENDTIME, ACCOUNTID, MAXCPU, MAXMEMORY, AVGCPU, AVGMEMORY, INSTANCEID, INSTANCETYPE, CLUSTERID, SETTINGID, MAXCPUVALUE, MAXMEMORYVALUE, AVGCPUVALUE, AVGMEMORYVALUE ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING";
  private static final String UTILIZATION_DATA_QUERY =
      "SELECT MAX(MAXCPU) as MAXCPUUTILIZATION, MAX(MAXMEMORY) as MAXMEMORYUTILIZATION, AVG(AVGCPU) as AVGCPUUTILIZATION, AVG(AVGMEMORY) as AVGMEMORYUTILIZATION, MAX(MAXCPUVALUE) as MAXCPUVALUE, MAX(MAXMEMORYVALUE) as MAXMEMORYVALUE, AVG(AVGCPUVALUE) as AVGCPUVALUE, AVG(AVGMEMORYVALUE) as AVGMEMORYVALUE, INSTANCEID FROM UTILIZATION_DATA WHERE ACCOUNTID = '%s' AND SETTINGID = '%s' AND CLUSTERID = '%s' AND INSTANCEID IN ('%s') AND STARTTIME >= '%s' AND STARTTIME < '%s' GROUP BY INSTANCEID;";

  public boolean create(List<InstanceUtilizationData> instanceUtilizationDataList) {
    boolean successfulInsert = false;
    if (timeScaleDBService.isValid() && isNotEmpty(instanceUtilizationDataList)) {
      logger.info("Util data size {}", instanceUtilizationDataList.size());
      int retryCount = 0;
      while (!successfulInsert && retryCount < MAX_RETRY_COUNT) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             PreparedStatement statement = dbConnection.prepareStatement(INSERT_STATEMENT)) {
          int index = 0;
          for (InstanceUtilizationData instanceUtilizationData : instanceUtilizationDataList) {
            updateInsertStatement(statement, instanceUtilizationData);
            statement.addBatch();
            index++;

            if (index % BATCH_SIZE == 0 || index == instanceUtilizationDataList.size()) {
              statement.executeBatch();
            }
          }
          successfulInsert = true;
        } catch (SQLException e) {
          logger.error("Failed to save instance Utilization data,[{}],retryCount=[{}], Exception: ",
              instanceUtilizationDataList, retryCount, e);
          retryCount++;
        }
      }
    } else {
      logger.info("Not processing instance Utilization data:[{}]", instanceUtilizationDataList);
    }
    return successfulInsert;
  }

  private void updateInsertStatement(PreparedStatement statement, InstanceUtilizationData instanceUtilizationData)
      throws SQLException {
    statement.setTimestamp(1, new Timestamp(instanceUtilizationData.getStartTimestamp()), utils.getDefaultCalendar());
    statement.setTimestamp(2, new Timestamp(instanceUtilizationData.getEndTimestamp()), utils.getDefaultCalendar());
    statement.setString(3, instanceUtilizationData.getAccountId());
    statement.setDouble(4, instanceUtilizationData.getCpuUtilizationMax());
    statement.setDouble(5, instanceUtilizationData.getMemoryUtilizationMax());
    statement.setDouble(6, instanceUtilizationData.getCpuUtilizationAvg());
    statement.setDouble(7, instanceUtilizationData.getMemoryUtilizationAvg());
    statement.setString(8, instanceUtilizationData.getInstanceId());
    statement.setString(9, instanceUtilizationData.getInstanceType());
    statement.setString(10, instanceUtilizationData.getClusterId());
    statement.setString(11, instanceUtilizationData.getSettingId());
    statement.setDouble(12, instanceUtilizationData.getCpuUtilizationMaxValue());
    statement.setDouble(13, instanceUtilizationData.getMemoryUtilizationMaxValue());
    statement.setDouble(14, instanceUtilizationData.getCpuUtilizationAvgValue());
    statement.setDouble(15, instanceUtilizationData.getMemoryUtilizationAvgValue());
  }

  public Map<String, UtilizationData> getUtilizationDataForInstances(List<? extends InstanceData> instanceDataList,
      String startTime, String endTime, String accountId, String settingId, String clusterId) {
    try {
      if (timeScaleDBService.isValid()) {
        Map<String, List<String>> serviceArnToInstanceIds = getServiceArnToInstanceIdMapping(instanceDataList);
        String query = String.format(UTILIZATION_DATA_QUERY, accountId, settingId, clusterId,
            String.join("','", serviceArnToInstanceIds.keySet()), startTime, endTime);
        return getUtilizationDataFromTimescaleDB(query, serviceArnToInstanceIds);
      } else {
        throw new InvalidRequestException("Cannot process request in InstanceBillingDataWriter");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching utilization data {}", e);
    }
  }

  private Map<String, UtilizationData> getUtilizationDataFromTimescaleDB(
      String query, Map<String, List<String>> serviceArnToInstanceIds) {
    ResultSet resultSet = null;
    Map<String, UtilizationData> utilizationDataForInstances = new HashMap<>();
    populateDefaultUtilizationData(utilizationDataForInstances, serviceArnToInstanceIds);
    logger.debug("Utilization data query : {}", query);

    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(query);
      while (resultSet.next()) {
        String instanceId = resultSet.getString("INSTANCEID");
        double maxCpuUtilization = resultSet.getDouble("MAXCPUUTILIZATION");
        double maxMemoryUtilization = resultSet.getDouble("MAXMEMORYUTILIZATION");
        double avgCpuUtilization = resultSet.getDouble("AVGCPUUTILIZATION");
        double avgMemoryUtilization = resultSet.getDouble("AVGMEMORYUTILIZATION");
        double maxCpuValue = resultSet.getDouble("MAXCPUVALUE");
        double maxMemoryValue = resultSet.getDouble("MAXMEMORYVALUE");
        double avgCpuValue = resultSet.getDouble("AVGCPUVALUE");
        double avgMemoryValue = resultSet.getDouble("AVGMEMORYVALUE");
        if (serviceArnToInstanceIds.get(instanceId) != null) {
          serviceArnToInstanceIds.get(instanceId)
              .forEach(instance
                  -> utilizationDataForInstances.put(instance,
                      UtilizationData.builder()
                          .maxCpuUtilization(maxCpuUtilization)
                          .maxMemoryUtilization(maxMemoryUtilization)
                          .avgCpuUtilization(avgCpuUtilization)
                          .avgMemoryUtilization(avgMemoryUtilization)
                          .maxCpuUtilizationValue(maxCpuValue)
                          .avgCpuUtilizationValue(avgCpuValue)
                          .maxMemoryUtilizationValue(maxMemoryValue)
                          .avgMemoryUtilizationValue(avgMemoryValue)
                          .build()));
        }
      }
      return utilizationDataForInstances;
    } catch (SQLException e) {
      logger.error("Error while fetching utilization data : exception {}", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return null;
  }

  private Map<String, List<String>> getServiceArnToInstanceIdMapping(List<? extends InstanceData> instanceDataList) {
    Map<String, List<String>> instanceIds = new HashMap<>();
    instanceDataList.forEach(instanceData -> {
      String instanceId = instanceData.getInstanceId();
      String utilInstanceId = instanceId;
      if (instanceData.getInstanceType() == InstanceType.ECS_TASK_EC2) {
        utilInstanceId = getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.ECS_SERVICE_ARN, instanceData);
      } else if (instanceData.getInstanceType() == InstanceType.ECS_CONTAINER_INSTANCE) {
        utilInstanceId = instanceData.getClusterName();
      }
      instanceIds.computeIfAbsent(utilInstanceId, k -> new ArrayList<>());
      instanceIds.get(utilInstanceId).add(instanceId);
    });
    return instanceIds;
  }

  private String getValueForKeyFromInstanceMetaData(String metaDataKey, InstanceData instanceData) {
    if (null != instanceData.getMetaData() && instanceData.getMetaData().containsKey(metaDataKey)) {
      return instanceData.getMetaData().get(metaDataKey);
    }
    return null;
  }

  private void populateDefaultUtilizationData(
      Map<String, UtilizationData> utilizationDataForInstances, Map<String, List<String>> serviceArnToInstanceIds) {
    if (serviceArnToInstanceIds != null) {
      for (Entry<String, List<String>> entry : serviceArnToInstanceIds.entrySet()) {
        if (entry.getValue() != null) {
          entry.getValue().forEach(instance
              -> utilizationDataForInstances.put(instance,
                  UtilizationData.builder()
                      .maxCpuUtilization(1)
                      .maxMemoryUtilization(1)
                      .avgCpuUtilization(1)
                      .avgMemoryUtilization(1)
                      .build()));
        }
      }
    }
  }
}
