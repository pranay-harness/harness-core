package software.wings.graphql.datafetcher.anomaly;

import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingGroupBy;
import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.graphql.datafetcher.AbstractAnomalyDataFetcher;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyData;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyData.QLAnomalyDataBuilder;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyDataList;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyDataList.QLAnomalyDataListBuilder;
import software.wings.graphql.schema.type.aggregation.anomaly.QLEntityInfo;
import software.wings.graphql.schema.type.aggregation.anomaly.QLEntityInfo.QLEntityInfoBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CloudAnomaliesDataFetcher extends AbstractAnomalyDataFetcher<CloudBillingFilter, CloudBillingGroupBy> {
  @Inject private TimeScaleDBService timeScaleDBService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLAnomalyDataList fetch(
      String accountId, List<CloudBillingFilter> filters, List<CloudBillingGroupBy> groupBy) {
    QLAnomalyDataListBuilder anomaiesList = QLAnomalyDataList.builder();

    if (!timeScaleDBService.isValid()) {
      throw new InvalidRequestException(
          "Cannot process request in Cloud AnomaliesDataFetcher since timescaleDBService is invalid");
    }
    try {
      anomaiesList.data(getData(accountId, filters, groupBy));
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching Cloud Anomalies data fetcher {}", e);
    }
    return anomaiesList.build();
  }

  List<QLAnomalyData> getData(String accountId, List<CloudBillingFilter> filters, List<CloudBillingGroupBy> groupBy) {
    List<QLAnomalyData> listAnomalies = new ArrayList<>();
    String queryStatement = new AnomalyDataQueryBuilder().formCloudQuery(accountId, filters, groupBy);
    boolean successfulRead = false;
    ResultSet resultSet = null;
    int retryCount = 0;
    while (!successfulRead && retryCount < MAX_RETRY) {
      try (Connection dbConnection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = dbConnection.prepareStatement(queryStatement)) {
        log.debug("Prepared Statement in CloudAnomaliesDataFetcher: {} ", statement);
        resultSet = statement.executeQuery();
        listAnomalies = extractAnomaliesFromResultSet(resultSet);
        successfulRead = true;
      } catch (SQLException e) {
        retryCount++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return listAnomalies;
  }

  private List<QLAnomalyData> extractAnomaliesFromResultSet(ResultSet resultSet) throws SQLException {
    List<QLAnomalyData> qlAnomalyDataList = new ArrayList<>();
    while (null != resultSet && resultSet.next()) {
      QLAnomalyDataBuilder anomalyBuilder = QLAnomalyData.builder();
      QLEntityInfoBuilder entityDataBuilder = QLEntityInfo.builder();
      for (AnomaliesDataTableSchema.fields field : AnomaliesDataTableSchema.getFields()) {
        switch (field) {
          case ID:
            anomalyBuilder.id(resultSet.getString(field.getFieldName()));
            break;
          case ANOMALY_TIME:
            anomalyBuilder.time(resultSet.getTimestamp(field.getFieldName()).getTime());
            break;
          case ACTUAL_COST:
            anomalyBuilder.actualAmount(resultSet.getDouble(field.getFieldName()));
            break;
          case EXPECTED_COST:
            anomalyBuilder.expectedAmount(resultSet.getDouble(field.getFieldName()));
            break;
          case NOTE:
            anomalyBuilder.comment(resultSet.getString(field.getFieldName()));
            break;
          case ANOMALY_SCORE:
            anomalyBuilder.anomalyScore(resultSet.getDouble(field.getFieldName()));
            break;
          case GCP_PRODUCT:
            entityDataBuilder.gcpProduct(resultSet.getString(field.getFieldName()));
            break;
          case GCP_PROJECT:
            entityDataBuilder.gcpProject(resultSet.getString(field.getFieldName()));
            break;
          case GCP_SKU_ID:
            entityDataBuilder.gcpSKUId(resultSet.getString(field.getFieldName()));
            break;
          case GCP_SKU_DESCRIPTION:
            entityDataBuilder.gcpSKUDescription(resultSet.getString(field.getFieldName()));
            break;
          case AWS_ACCOUNT:
            entityDataBuilder.awsAccount(resultSet.getString(field.getFieldName()));
            break;
          case AWS_SERVICE:
            entityDataBuilder.awsService(resultSet.getString(field.getFieldName()));
            break;
          case TIME_GRANULARITY:
          case AWS_USAGE_TYPE:
          case AWS_INSTANCE_TYPE:
          case ACCOUNT_ID:
          case REPORTED_BY:
          case REGION:
          case CLUSTER_ID:
          case CLUSTER_NAME:
          case NAMESPACE:
          case WORKLOAD_NAME:
          case WORKLOAD_TYPE:
            break;
          default:
            log.error("Unknown field Resultset conversion encountered in CloudAnoamliesDataFetecher");
        }
      }
      anomalyBuilder.entity(entityDataBuilder.build());
      qlAnomalyDataList.add(anomalyBuilder.build());
    }
    return qlAnomalyDataList;
  }
}
