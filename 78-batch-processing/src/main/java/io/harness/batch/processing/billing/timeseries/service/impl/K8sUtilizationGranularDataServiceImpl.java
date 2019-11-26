package io.harness.batch.processing.billing.timeseries.service.impl;

import com.google.inject.Singleton;

import io.harness.batch.processing.billing.timeseries.data.K8sGranularUtilizationData;
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
public class K8sUtilizationGranularDataServiceImpl {
  @Autowired private TimeScaleDBService timeScaleDBService;
  @Autowired private DataFetcherUtils utils;

  private static final int MAX_RETRY_COUNT = 5;
  static final String INSERT_STATEMENT =
      "INSERT INTO KUBERNETES_UTILIZATION_DATA (STARTTIME, ENDTIME, CPU, MEMORY, INSTACEID, INSTANCETYPE, SETTINGID ) VALUES (?,?,?,?,?,?,?)";

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

  void updateInsertStatement(PreparedStatement statement, K8sGranularUtilizationData k8sGranularUtilizationData)
      throws SQLException {
    statement.setTimestamp(
        1, new Timestamp(k8sGranularUtilizationData.getStartTimestamp()), utils.getDefaultCalendar());
    statement.setTimestamp(2, new Timestamp(k8sGranularUtilizationData.getEndTimestamp()), utils.getDefaultCalendar());
    statement.setDouble(3, k8sGranularUtilizationData.getCpu());
    statement.setDouble(4, k8sGranularUtilizationData.getMemory());
    statement.setString(5, k8sGranularUtilizationData.getInstanceId());
    statement.setString(6, k8sGranularUtilizationData.getInstanceType());
    statement.setString(7, k8sGranularUtilizationData.getSettingId());
  }
}
