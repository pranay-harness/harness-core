package io.harness.ccm.anomaly.dao;

import io.harness.ccm.anomaly.entities.AnomalyDetectionModel;
import io.harness.ccm.anomaly.entities.AnomalyEntity;
import io.harness.ccm.anomaly.entities.AnomalyEntity.AnomalyEntityBuilder;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyFeedback;

import com.google.inject.Inject;
import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.DeleteQuery;
import com.healthmarketscience.sqlbuilder.InCondition;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class AnomalyEntityDao {
  static int MAX_RETRY = 3;

  @Inject @Autowired private TimeScaleDBService dbService;

  public List<AnomalyEntity> list(String queryStatement) {
    List<AnomalyEntity> listAnomalies = new ArrayList<>();
    boolean successfulRead = false;
    ResultSet resultSet = null;
    int retryCount = 0;
    while (!successfulRead && retryCount < MAX_RETRY) {
      try (Connection dbConnection = dbService.getDBConnection();
           PreparedStatement statement = dbConnection.prepareStatement(queryStatement)) {
        log.info("[RDA] Query Step 1/3 : Statement in AnomalyDao: {} ", queryStatement);
        resultSet = statement.executeQuery();
        listAnomalies = extractAnomaliesFromResultSet(resultSet);
        successfulRead = true;
      } catch (SQLException e) {
        retryCount++;
        log.info("[RDA] Query failed after retry count {} , Exception {}", retryCount, e);
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return listAnomalies;
  }

  private List<AnomalyEntity> extractAnomaliesFromResultSet(ResultSet resultSet) throws SQLException {
    List<AnomalyEntity> listAnomalies = new ArrayList<>();

    log.info("[RDA] Query Step 2/2 : conversion of resultset into anomalyEntities");

    while (null != resultSet && resultSet.next()) {
      AnomalyEntityBuilder anomalyBuilder = AnomalyEntity.builder();
      for (AnomalyEntity.AnomaliesDataTableSchema.fields field : AnomalyEntity.AnomaliesDataTableSchema.getFields()) {
        switch (field) {
          case ANOMALY_TIME:
            anomalyBuilder.anomalyTime(resultSet.getTimestamp(field.getFieldName()).toInstant());
            break;
          case REPORTED_BY:
            anomalyBuilder.reportedBy(AnomalyDetectionModel.valueOf(resultSet.getString(field.getFieldName())));
            break;
          case FEED_BACK:
            if (resultSet.getString(field.getFieldName()) != null) {
              anomalyBuilder.feedback(QLAnomalyFeedback.valueOf(resultSet.getString(field.getFieldName())));
            } else {
              anomalyBuilder.feedback(QLAnomalyFeedback.NOT_RESPONDED);
            }
            break;
          case ID:
            anomalyBuilder.id(resultSet.getString(field.getFieldName()));
            break;
          case ACCOUNT_ID:
            anomalyBuilder.accountId(resultSet.getString(field.getFieldName()));
            break;
          case ACTUAL_COST:
            anomalyBuilder.actualCost(resultSet.getDouble(field.getFieldName()));
            break;
          case EXPECTED_COST:
            anomalyBuilder.expectedCost(resultSet.getDouble(field.getFieldName()));
            break;
          case NOTE:
            anomalyBuilder.note(resultSet.getString(field.getFieldName()));
            break;
          case ANOMALY_SCORE:
            anomalyBuilder.anomalyScore(resultSet.getDouble(field.getFieldName()));
            break;
          case CLUSTER_ID:
            anomalyBuilder.clusterId(resultSet.getString(field.getFieldName()));
            break;
          case NAMESPACE:
            anomalyBuilder.namespace(resultSet.getString(field.getFieldName()));
            break;
          case CLUSTER_NAME:
            anomalyBuilder.clusterName(resultSet.getString(field.getFieldName()));
            break;
          case WORKLOAD_NAME:
            anomalyBuilder.workloadName(resultSet.getString(field.getFieldName()));
            break;
          case WORKLOAD_TYPE:
            anomalyBuilder.workloadType(resultSet.getString(field.getFieldName()));
            break;
          case GCP_PRODUCT:
            anomalyBuilder.gcpProduct(resultSet.getString(field.getFieldName()));
            break;
          case GCP_PROJECT:
            anomalyBuilder.gcpProject(resultSet.getString(field.getFieldName()));
            break;
          case GCP_SKU_ID:
            anomalyBuilder.gcpSKUId(resultSet.getString(field.getFieldName()));
            break;
          case GCP_SKU_DESCRIPTION:
            anomalyBuilder.gcpSKUDescription(resultSet.getString(field.getFieldName()));
            break;
          case AWS_ACCOUNT:
            anomalyBuilder.awsAccount(resultSet.getString(field.getFieldName()));
            break;
          case AWS_SERVICE:
            anomalyBuilder.awsService(resultSet.getString(field.getFieldName()));
            break;
          case AWS_INSTANCE_TYPE:
            anomalyBuilder.awsInstanceType(resultSet.getString(field.getFieldName()));
            break;
          case AWS_USAGE_TYPE:
            anomalyBuilder.awsUsageType(resultSet.getString(field.getFieldName()));
            break;
          default:
            log.error("Unknown field : {} encountered while Resultset conversion in AnomalyDao", field);
        }
      }
      listAnomalies.add(anomalyBuilder.build());
    }

    return listAnomalies;
  }

  public void delete(List<String> ids, Instant date) {
    boolean successfulDelete = false;
    if (dbService.isValid()) {
      String queryStatement = getDeleteQuery(ids, date);
      log.info("[RDA] Deleting anomalies with query : [{}]", queryStatement);
      int retryCount = 0;
      while (!successfulDelete && retryCount < MAX_RETRY) {
        try (Connection dbConnection = dbService.getDBConnection();
             Statement statement = dbConnection.createStatement()) {
          statement.execute(queryStatement);
          successfulDelete = true;
        } catch (SQLException e) {
          log.error("[RDA] Failed to delete anomalies , retryCount=[{}], Exception: ", retryCount, e);
          retryCount++;
        }
      }
    } else {
      log.error("[RDA] Not able to delete anomalies in timescale db(validity:{}) ", dbService.isValid());
    }
  }

  private String getDeleteQuery(List<String> ids, Instant date) {
    DeleteQuery query = new DeleteQuery(AnomalyEntity.AnomaliesDataTableSchema.table);
    query.addCondition(new InCondition(AnomalyEntity.AnomaliesDataTableSchema.id, ids));
    query.addCondition(
        BinaryCondition.equalTo(AnomalyEntity.AnomaliesDataTableSchema.anomalyTime, date.truncatedTo(ChronoUnit.DAYS)));
    return query.validate().toString();
  }
}
