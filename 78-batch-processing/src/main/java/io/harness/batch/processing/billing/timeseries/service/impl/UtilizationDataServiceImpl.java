package io.harness.batch.processing.billing.timeseries.service.impl;

import com.google.inject.Singleton;

import io.harness.batch.processing.billing.service.UtilizationData;
import io.harness.batch.processing.billing.timeseries.data.InstanceUtilizationData;
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

  private static final int MAX_RETRY_COUNT = 5;

  static final String INSERT_STATEMENT =
      "INSERT INTO UTILIZATION_DATA (STARTTIME, ENDTIME, CLUSTERARN, CLUTERNAME, SERVICEARN, SERVICENAME, MAXCPU, MAXMEMORY, AVGCPU, AVGMEMORY, INSTACEID, INSTANCETYPE, SETTINGID ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
  private static final String UTILIZATION_DATA_QUERY =
      "SELECT MAX(MAXCPU) as CPUUTILIZATION, MAX(MAXMEMORY) as MEMORYUTILIZATION, INSTANCEID FROM UTILIZATION_DATA WHERE INSTANCEID IN ('%s') AND STARTTIME >= '%s' AND ENDTIME <= '%s' GROUP BY INSTANCEID;";

  public boolean create(InstanceUtilizationData instanceUtilizationData) {
    boolean successfulInsert = false;
    if (timeScaleDBService.isValid()) {
      long startTime = System.currentTimeMillis();
      int retryCount = 0;
      while (!successfulInsert && retryCount < MAX_RETRY_COUNT) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             PreparedStatement statement = dbConnection.prepareStatement(INSERT_STATEMENT)) {
          updateInsertStatement(statement, instanceUtilizationData);
          statement.execute();
          successfulInsert = true;

        } catch (SQLException e) {
          logger.info(
              "Failed to save instance Utilization data,[{}],retryCount=[{}]", instanceUtilizationData, retryCount);
          retryCount++;
        } finally {
          logger.info("Total time=[{}]", System.currentTimeMillis() - startTime);
        }
      }
    } else {
      logger.info("Not processing instance Utilization data:[{}]", instanceUtilizationData);
    }
    return successfulInsert;
  }

  void updateInsertStatement(PreparedStatement statement, InstanceUtilizationData instanceUtilizationData)
      throws SQLException {
    statement.setTimestamp(1, new Timestamp(instanceUtilizationData.getStartTimestamp()), utils.getDefaultCalendar());
    statement.setTimestamp(2, new Timestamp(instanceUtilizationData.getEndTimestamp()), utils.getDefaultCalendar());
    statement.setString(3, instanceUtilizationData.getClusterArn());
    statement.setString(4, instanceUtilizationData.getClusterName());
    statement.setString(5, instanceUtilizationData.getServiceArn());
    statement.setString(6, instanceUtilizationData.getServiceName());
    statement.setDouble(7, instanceUtilizationData.getCpuUtilizationMax());
    statement.setDouble(8, instanceUtilizationData.getMemoryUtilizationMax());
    statement.setDouble(9, instanceUtilizationData.getCpuUtilizationAvg());
    statement.setDouble(10, instanceUtilizationData.getMemoryUtilizationAvg());
    statement.setString(11, instanceUtilizationData.getInstanceId());
    statement.setString(12, instanceUtilizationData.getInstanceType());
    statement.setString(13, instanceUtilizationData.getSettingId());
  }

  public Map<String, UtilizationData> getUtilizationDataForInstances(
      List<? extends InstanceData> instanceDataList, String startTime, String endTime) {
    try {
      if (timeScaleDBService.isValid()) {
        logger.info("TimescaleDb is valid");
        Map<String, List<String>> serviceArnToInstanceIds = getServiceArnToInstanceIdMapping(instanceDataList);
        String query = String.format(
            UTILIZATION_DATA_QUERY, String.join("','", serviceArnToInstanceIds.keySet()), startTime, endTime);
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
    logger.info("Utilization data query : {}", query);

    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(query);
      while (resultSet.next()) {
        String instanceId = resultSet.getString("INSTANCEID");
        Double cpuUtilization = resultSet.getDouble("CPUUTILIZATION");
        Double memoryUtilization = resultSet.getDouble("MEMORYUTILIZATION");
        if (serviceArnToInstanceIds.get(instanceId) != null) {
          serviceArnToInstanceIds.get(instanceId).forEach(instance -> {
            utilizationDataForInstances.put(instance,
                UtilizationData.builder().cpuUtilization(cpuUtilization).memoryUtilization(memoryUtilization).build());
          });
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
      if (instanceData.getInstanceType().toString().contains("K8S")) {
        List<String> instances = instanceIds.get(instanceId);
        if (instances == null) {
          instances = new ArrayList<>();
        }
        instances.add(instanceId);
        instanceIds.put(instanceData.getInstanceId(), instances);
      } else {
        String serviceArn = getServiceArnFromInstanceMetaData(instanceData);
        List<String> instances = instanceIds.get(serviceArn);
        if (instances == null) {
          instances = new ArrayList<>();
        }
        instances.add(instanceId);
        instanceIds.put(serviceArn, instances);
      }
    });
    return instanceIds;
  }

  private String getServiceArnFromInstanceMetaData(InstanceData instanceData) {
    if (null != instanceData.getMetaData()
        && instanceData.getMetaData().containsKey(InstanceMetaDataConstants.ECS_SERVICE_ARN)) {
      return instanceData.getMetaData().get(InstanceMetaDataConstants.ECS_SERVICE_ARN);
    }
    return null;
  }

  private void populateDefaultUtilizationData(
      Map<String, UtilizationData> utilizationDataForInstances, Map<String, List<String>> serviceArnToInstanceIds) {
    if (serviceArnToInstanceIds != null) {
      for (Entry<String, List<String>> entry : serviceArnToInstanceIds.entrySet()) {
        if (entry.getValue() != null) {
          entry.getValue().forEach(instance -> {
            utilizationDataForInstances.put(
                instance, UtilizationData.builder().cpuUtilization(1).memoryUtilization(1).build());
          });
        }
      }
    }
  }
}
