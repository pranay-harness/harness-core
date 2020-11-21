package io.harness.batch.processing.anomalydetection.service.impl;

import io.harness.batch.processing.anomalydetection.Anomaly;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionConstants;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries.AnomalyDetectionTimeSeriesBuilder;
import io.harness.batch.processing.anomalydetection.TimeSeriesSpec;
import io.harness.batch.processing.anomalydetection.TimeSeriesUtils;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.graphql.datafetcher.anomaly.AnomaliesDataTableSchema;

import com.google.inject.Singleton;
import com.healthmarketscience.sqlbuilder.InsertQuery;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Singleton
@Slf4j
public class AnomalyDetectionTimescaleDataServiceImpl {
  @Autowired private TimeScaleDBService timeScaleDBService;
  static final String CLUSTER_STATEMENT =
      "SELECT STARTTIME AS STARTTIME ,  sum(BILLINGAMOUNT) AS COST ,CLUSTERID ,CLUSTERNAME from billing_data where ACCOUNTID = ? and STARTTIME >= ? and STARTTIME <= ? and instancetype IN ('ECS_TASK_FARGATE','ECS_CONTAINER_INSTANCE','K8S_NODE') group by CLUSTERID , STARTTIME , CLUSTERNAME  order by  CLUSTERID ,STARTTIME , CLUSTERNAME";

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
        log.error("entity type is undefined in timeseries spec");
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
          log.debug("Prepared Statement in AnomalyDetectionTimescaleDataServiceImpl: {} ", statement);
          resultSet = statement.executeQuery();
          if (resultSet.next()) {
            listClusterAnomalyDetectionTimeSeries = readTimeSeriesFromResultSet(resultSet, timeSeriesSpec);
          }
          successfulRead = true;
        } catch (SQLException e) {
          log.error("Failed to fetch cluster time series for accountId ,[{}],retryCount=[{}], Exception: ",
              timeSeriesSpec.getAccountId(), retryCount, e);
          retryCount++;
        } finally {
          DBUtils.close(resultSet);
        }
      }
    }
    if (listClusterAnomalyDetectionTimeSeries.isEmpty()) {
      log.error("No TimeSeries Data Present");
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
        log.info("Invalid time series data of {}:{} ", currentAnomalyDetectionTimeSeries.getEntityType(),
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
        log.error("entity type is undefined in timeseries spec");
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
      String insertStatement;
      int retryCount = 0;
      int index = 0;
      while (!successfulInsert && retryCount < MAX_RETRY_COUNT) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             Statement statement = dbConnection.createStatement()) {
          index = 0;
          for (Anomaly anomaly : anomaliesList) {
            insertStatement = getInsertQuery(anomaly);
            statement.addBatch(insertStatement);
            index++;
            if (index % AnomalyDetectionConstants.BATCH_SIZE == 0 || index == anomaliesList.size()) {
              log.debug("Prepared Statement in AnomalyDetectionTimescaleDataServiceImpl: {} ", statement);
              int[] count = statement.executeBatch();
              log.debug("Successfully inserted {} anomalies into timescaledb", IntStream.of(count).sum());
            }
          }
          successfulInsert = true;
        } catch (SQLException e) {
          log.error(
              "Failed to save anomalies data,[{}],retryCount=[{}], Exception: ", anomaliesList.size(), retryCount, e);
          retryCount++;
        }
      }
    } else {
      log.warn("Not able to write {} anomalies to timescale db(validity:{}) for account", anomaliesList.size(),
          timeScaleDBService.isValid());
    }
    return successfulInsert;
  }

  private String getInsertQuery(Anomaly anomaly) throws SQLException {
    return new InsertQuery(AnomaliesDataTableSchema.table)
        .addColumn(AnomaliesDataTableSchema.id, anomaly.getId())
        .addColumn(AnomaliesDataTableSchema.accountId, anomaly.getAccountId())
        .addColumn(AnomaliesDataTableSchema.actualCost, anomaly.getActualCost())
        .addColumn(AnomaliesDataTableSchema.expectedCost, anomaly.getExpectedCost())
        .addColumn(AnomaliesDataTableSchema.anomalyTime, Timestamp.from(anomaly.getTime()))
        .addColumn(AnomaliesDataTableSchema.timeGranularity, anomaly.getTimeGranularity().toString())
        .addColumn(AnomaliesDataTableSchema.clusterId, anomaly.getClusterId())
        .addColumn(AnomaliesDataTableSchema.clusterName, anomaly.getClusterName())
        .addColumn(AnomaliesDataTableSchema.namespace, anomaly.getNamespace())
        .addColumn(AnomaliesDataTableSchema.workloadType, anomaly.getWorkloadType())
        .addColumn(AnomaliesDataTableSchema.workloadName, anomaly.getWorkloadName())
        .addColumn(AnomaliesDataTableSchema.region, anomaly.getRegion())
        .addColumn(AnomaliesDataTableSchema.gcpProduct, anomaly.getGcpProduct())
        .addColumn(AnomaliesDataTableSchema.gcpProject, anomaly.getGcpProject())
        .addColumn(AnomaliesDataTableSchema.gcpSkuId, anomaly.getGcpSKUId())
        .addColumn(AnomaliesDataTableSchema.gcpSkuDescription, anomaly.getGcpSKUDescription())
        .addColumn(AnomaliesDataTableSchema.awsAccount, anomaly.getAwsAccount())
        .addColumn(AnomaliesDataTableSchema.awsInstanceType, anomaly.getAwsInstanceType())
        .addColumn(AnomaliesDataTableSchema.awsService, anomaly.getAwsService())
        .addColumn(AnomaliesDataTableSchema.awsUsageType, anomaly.getAwsUsageType())
        .addColumn(AnomaliesDataTableSchema.anomalyScore, anomaly.getAnomalyScore())
        .addColumn(AnomaliesDataTableSchema.reportedBy, anomaly.getReportedBy())
        .validate()
        .toString();
  }
}
