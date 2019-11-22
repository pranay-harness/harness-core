package io.harness.batch.processing.billing.timeseries.service.impl;

import com.google.inject.Singleton;

import io.harness.batch.processing.billing.timeseries.data.InstanceUtilizationData;
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
public class UtilizationDataServiceImpl {
  @Autowired private TimeScaleDBService timeScaleDBService;
  @Autowired private DataFetcherUtils utils;

  private static final int MAX_RETRY_COUNT = 5;
  static final String INSERT_STATEMENT =
      "INSERT INTO UTILIZATION_DATA (STARTTIME, ENDTIME, CLUSTERARN, CLUTERNAME, SERVICEARN, SERVICENAME, MAXCPU, MAXMEMORY, AVGCPU, AVGMEMORY ) VALUES (?,?,?,?,?,?,?,?,?,?)";

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
  }
}
