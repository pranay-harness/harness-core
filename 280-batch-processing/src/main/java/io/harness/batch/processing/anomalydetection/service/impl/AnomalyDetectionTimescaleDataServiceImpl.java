package io.harness.batch.processing.anomalydetection.service.impl;

import com.google.inject.Singleton;

import io.harness.batch.processing.anomalydetection.Anomaly;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionConstants;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries;
import io.harness.batch.processing.anomalydetection.TimeSeriesSpec;
import io.harness.batch.processing.anomalydetection.TimeSeriesUtils;
import io.harness.batch.processing.pricing.data.CloudProvider;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Service
@Singleton
@Slf4j
public class AnomalyDetectionTimescaleDataServiceImpl {
  @Autowired private TimeScaleDBService timeScaleDBService;
  static final String CLUSTER_STATEMENT =
      "SELECT STARTTIME AS STARTTIME ,  sum(BILLINGAMOUNT) AS COST ,CLUSTERID ,CLUSTERNAME , CLOUDPROVIDERID from billing_data where ACCOUNTID = '%s' and STARTTIME >= '%s' and STARTTIME <= '%s' and instancetype IN ('ECS_TASK_FARGATE','ECS_CONTAINER_INSTANCE','K8S_NODE') group by CLUSTERID , STARTTIME , CLUSTERNAME , CLOUDPROVIDERID order by  CLUSTERID ,STARTTIME , CLUSTERNAME , CLOUDPROVIDERID";

  static final String ANOMALY_INSERT_STATEMENT =
      "INSERT INTO ANOMALIES (ANOMALYTIME,ACCOUNTID,TIMEGRANULARITY,ENTITYID,ENTITYTYPE,CLUSTERID,CLUSTERNAME,WORKLOADNAME,WORKLOADTYPE,NAMESPACE,CLOUDPROVIDERID,ANOMALYSCORE,ANOMALYTYPE,REPORTEDBY,ABSOLUTETHRESHOLD,RELATIVETHRESHOLD,PROBABILISTICTHRESHOLD) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING";

  private static final int MAX_RETRY_COUNT = 2;

  public List<AnomalyDetectionTimeSeries> readClusterLevelData(TimeSeriesSpec timeSeriesSpec) {
    List<AnomalyDetectionTimeSeries> listClusterAnomalyDetectionTimeSeries = new ArrayList<>();
    boolean successfulRead = false;
    String query = String.format(
        CLUSTER_STATEMENT, timeSeriesSpec.getAccountId(), timeSeriesSpec.getTrainStart(), timeSeriesSpec.getTestEnd());
    ResultSet resultSet = null;
    if (timeScaleDBService.isValid()) {
      int retryCount = 0;
      while (!successfulRead && retryCount < MAX_RETRY_COUNT) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             Statement statement = dbConnection.createStatement()) {
          logger.debug("Prepared Statement in AnomalyDetectionTimescaleDataServiceImpl: {} ", statement);
          resultSet = statement.executeQuery(query);
          if (resultSet.next()) {
            listClusterAnomalyDetectionTimeSeries = readTimeSeriesFromResultSet(resultSet, timeSeriesSpec);
          }
          successfulRead = true;
        } catch (SQLException e) {
          logger.error("Failed to fetch cluster time series for accountId ,[{}],retryCount=[{}], Exception: ",
              timeSeriesSpec.getAccountId(), retryCount, e);
          retryCount++;
        } finally {
          DBUtils.close(resultSet);
        }
      }
    }
    if (listClusterAnomalyDetectionTimeSeries.isEmpty()) {
      logger.error("No TimeSeries Data Present");
    }
    return listClusterAnomalyDetectionTimeSeries;
  }

  public List<AnomalyDetectionTimeSeries> readTimeSeriesFromResultSet(
      ResultSet resultSet, TimeSeriesSpec timeSeriesSpec) throws SQLException {
    List<AnomalyDetectionTimeSeries> listClusterAnomalyDetectionTimeSeries = new ArrayList<>();
    AnomalyDetectionTimeSeries currentAnomalyDetectionTimeSeries;
    do {
      currentAnomalyDetectionTimeSeries = readNextTimeSeries(resultSet, timeSeriesSpec);
      if (TimeSeriesUtils.validate(currentAnomalyDetectionTimeSeries, timeSeriesSpec)) {
        listClusterAnomalyDetectionTimeSeries.add(currentAnomalyDetectionTimeSeries);
      }
    } while (!resultSet.isClosed());
    return listClusterAnomalyDetectionTimeSeries;
  }

  public AnomalyDetectionTimeSeries readNextTimeSeries(ResultSet resultSet, TimeSeriesSpec timeSeriesSpec)
      throws SQLException {
    String clusterId = resultSet.getString("CLUSTERID");
    String clusterName = resultSet.getString("CLUSTERNAME");
    CloudProvider cloudProvider = null;

    if (resultSet.getString("CLOUDPROVIDERID") != null) {
      cloudProvider = CloudProvider.valueOf(resultSet.getString("CLOUDPROVIDERID"));
    }

    AnomalyDetectionTimeSeries anomalyDetectionTimeSeries = AnomalyDetectionTimeSeries.builder()
                                                                .accountId(timeSeriesSpec.getAccountId())
                                                                .timeGranularity(timeSeriesSpec.getTimeGranularity())
                                                                .entityId(clusterId)
                                                                .entityType(timeSeriesSpec.getEntityType())
                                                                .clusterId(clusterId)
                                                                .clusterName(clusterName)
                                                                .cloudProvider(cloudProvider)
                                                                .build();

    anomalyDetectionTimeSeries.initialiseTrainData(
        timeSeriesSpec.getTrainStart(), timeSeriesSpec.getTrainEnd(), ChronoUnit.DAYS);
    anomalyDetectionTimeSeries.initialiseTestData(
        timeSeriesSpec.getTestStart(), timeSeriesSpec.getTestEnd(), ChronoUnit.DAYS);

    Instant currentTime;
    Double currentValue;

    do {
      currentTime = resultSet.getTimestamp("STARTTIME").toInstant();
      currentValue = resultSet.getDouble("COST");
      anomalyDetectionTimeSeries.insert(currentTime, currentValue);
      if (!resultSet.next()) {
        DBUtils.close(resultSet);
        break;
      }
    } while (resultSet.getString("CLUSTERID").equals(clusterId));
    return anomalyDetectionTimeSeries;
  }

  //--------------- Write Anomalies to Timescale DB ---------------------------

  public boolean writeAnomaliesToTimescale(List<Anomaly> anomaliesList) {
    boolean successfulInsert = false;
    if (timeScaleDBService.isValid() && !anomaliesList.isEmpty()) {
      String insertStatement = ANOMALY_INSERT_STATEMENT;
      int retryCount = 0;
      while (!successfulInsert && retryCount < MAX_RETRY_COUNT) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             PreparedStatement statement = dbConnection.prepareStatement(insertStatement)) {
          int index = 0;
          for (Anomaly anomaly : anomaliesList) {
            updateInsertStatement(statement, anomaly);
            statement.addBatch();
            index++;
            if (index % AnomalyDetectionConstants.BATCH_SIZE == 0 || index == anomaliesList.size()) {
              logger.debug("Prepared Statement in AnomalyDetectionTimescaleDataServiceImpl: {} ", statement);
              int[] count = statement.executeBatch();
              logger.debug("Successfully inserted {} anomalies into timescaledb", IntStream.of(count).sum());
            }
          }
          successfulInsert = true;
        } catch (SQLException e) {
          logger.error(
              "Failed to save anomalies data,[{}],retryCount=[{}], Exception: ", anomaliesList.size(), retryCount, e);
          retryCount++;
        }
      }
    } else {
      logger.warn("Not able to write {} anomalies to timescale db(validity:{}) for account", anomaliesList.size(),
          timeScaleDBService.isValid());
    }
    return successfulInsert;
  }

  private void updateInsertStatement(PreparedStatement statement, Anomaly anomaly) throws SQLException {
    statement.setTimestamp(1, Timestamp.from(anomaly.getInstant()));
    statement.setString(2, anomaly.getAccountId());
    statement.setString(3, anomaly.getTimeGranularity().toString());
    statement.setString(4, anomaly.getEntityId());
    statement.setString(5, anomaly.getEntityType().toString());
    statement.setString(6, anomaly.getClusterId());
    statement.setString(7, anomaly.getClusterName());
    statement.setString(8, anomaly.getWorkloadName());
    statement.setString(9, anomaly.getWorkloadType());
    statement.setString(10, anomaly.getNamespace());
    if (anomaly.getCloudProvider() != null) {
      statement.setString(11, anomaly.getCloudProvider().toString());
    } else {
      statement.setString(11, null);
    }
    statement.setDouble(12, anomaly.getAnomalyScore());
    statement.setString(13, anomaly.getAnomalyType().toString());
    statement.setString(14, anomaly.getReportedBy().toString());
    statement.setBoolean(15, anomaly.isAbsoluteThreshold());
    statement.setBoolean(16, anomaly.isRelativeThreshold());
    statement.setBoolean(17, anomaly.isProbabilisticThreshold());
  }
}
