package io.harness.batch.processing.anomalydetection.service.impl;

import com.google.inject.Singleton;

import io.harness.batch.processing.anomalydetection.Anomaly;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionConstants;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries.AnomalyDetectionTimeSeriesBuilder;
import io.harness.batch.processing.anomalydetection.TimeSeriesSpec;
import io.harness.batch.processing.anomalydetection.TimeSeriesUtils;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
      "SELECT STARTTIME AS STARTTIME ,  sum(BILLINGAMOUNT) AS COST ,CLUSTERID ,CLUSTERNAME from billing_data where ACCOUNTID = ? and STARTTIME >= ? and STARTTIME <= ? and instancetype IN ('ECS_TASK_FARGATE','ECS_CONTAINER_INSTANCE','K8S_NODE') group by CLUSTERID , STARTTIME , CLUSTERNAME  order by  CLUSTERID ,STARTTIME , CLUSTERNAME";

  static final String ANOMALY_INSERT_STATEMENT =
      "INSERT INTO ANOMALIES (ANOMALYTIME,ACCOUNTID,TIMEGRANULARITY,ENTITYID,ENTITYTYPE,CLUSTERID,CLUSTERNAME,WORKLOADNAME,WORKLOADTYPE,NAMESPACE,ANOMALYSCORE,ANOMALYTYPE,REPORTEDBY,ABSOLUTETHRESHOLD,RELATIVETHRESHOLD,PROBABILISTICTHRESHOLD) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING";
  static final String NAMESPACE_STATEMENT =
      "SELECT STARTTIME ,  sum(BILLINGAMOUNT) AS COST , NAMESPACE , CLUSTERID from billing_data where ACCOUNTID = ? and STARTTIME >= ? and STARTTIME <= ? and instancetype IN ('K8S_POD')  group by namespace , CLUSTERID , STARTTIME  order by  NAMESPACE, CLUSTERID ,STARTTIME ";

  private static final int MAX_RETRY_COUNT = 2;

  public List<AnomalyDetectionTimeSeries> readData(TimeSeriesSpec timeSeriesSpec) {
    List<AnomalyDetectionTimeSeries> listClusterAnomalyDetectionTimeSeries = new ArrayList<>();
    boolean successfulRead = false;

    String queryStatement = "default";

    switch (timeSeriesSpec.getEntityType()) {
      case CLUSTER:
        queryStatement = CLUSTER_STATEMENT;
        break;
      case NAMESPACE:
        queryStatement = NAMESPACE_STATEMENT;
        break;
      default:
        logger.error("entity type is undefined in timeseries spec");
        break;
    }

    ResultSet resultSet = null;
    if (timeScaleDBService.isValid()) {
      int retryCount = 0;
      while (!successfulRead && retryCount < MAX_RETRY_COUNT) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             PreparedStatement statement = dbConnection.prepareStatement(queryStatement)) {
          statement.setString(1, timeSeriesSpec.getAccountId());
          statement.setTimestamp(2, Timestamp.from(timeSeriesSpec.getTrainStart()));
          statement.setTimestamp(3, Timestamp.from(timeSeriesSpec.getTestEnd()));
          logger.debug("Prepared Statement in AnomalyDetectionTimescaleDataServiceImpl: {} ", statement);
          resultSet = statement.executeQuery();
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
      } else {
        logger.info("Invalid time series data of {}:{} ", currentAnomalyDetectionTimeSeries.getEntityType(),
            currentAnomalyDetectionTimeSeries.getEntityId());
      }
    } while (!resultSet.isClosed());
    return listClusterAnomalyDetectionTimeSeries;
  }

  public AnomalyDetectionTimeSeries readNextTimeSeries(ResultSet resultSet, TimeSeriesSpec timeSeriesSpec)
      throws SQLException {
    AnomalyDetectionTimeSeriesBuilder<?, ?> timeSeriesBuilder = AnomalyDetectionTimeSeries.builder();

    timeSeriesBuilder.accountId(timeSeriesSpec.getAccountId()).timeGranularity(timeSeriesSpec.getTimeGranularity());

    String entityId = "default";

    switch (timeSeriesSpec.getEntityType()) {
      case CLUSTER:
        entityId = resultSet.getString("CLUSTERID");
        timeSeriesBuilder.clusterId(entityId);
        timeSeriesBuilder.clusterName(resultSet.getString("CLUSTERNAME"));
        break;
      case NAMESPACE:
        entityId = resultSet.getString("NAMESPACE");
        timeSeriesBuilder.clusterId(resultSet.getString("CLUSTERID"));
        timeSeriesBuilder.namespace(entityId);
        break;
      case WORKLOAD:
        break;
      default:
        logger.error("entity type is undefined in timeseries spec");
        break;
    }
    timeSeriesBuilder.entityType(timeSeriesSpec.getEntityType()).entityId(entityId);

    AnomalyDetectionTimeSeries anomalyDetectionTimeSeries = timeSeriesBuilder.build();

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
    } while (resultSet.getString(timeSeriesSpec.getEntityIdentifier()).equals(entityId));
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
    statement.setDouble(11, anomaly.getAnomalyScore());
    statement.setString(12, anomaly.getAnomalyType().toString());
    statement.setString(13, anomaly.getReportedBy().toString());
    statement.setBoolean(14, anomaly.isAbsoluteThreshold());
    statement.setBoolean(15, anomaly.isRelativeThreshold());
    statement.setBoolean(16, anomaly.isProbabilisticThreshold());
  }
}
