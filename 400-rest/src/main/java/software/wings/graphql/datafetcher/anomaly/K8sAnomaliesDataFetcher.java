package software.wings.graphql.datafetcher.anomaly;

import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.graphql.datafetcher.AbstractAnomalyDataFetcher;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyData;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyData.QLAnomalyDataBuilder;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyDataList;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyDataList.QLAnomalyDataListBuilder;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyFeedback;
import software.wings.graphql.schema.type.aggregation.anomaly.QLEntityInfo;
import software.wings.graphql.schema.type.aggregation.anomaly.QLEntityInfo.QLEntityInfoBuilder;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
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
public class K8sAnomaliesDataFetcher extends AbstractAnomalyDataFetcher<QLBillingDataFilter, QLCCMGroupBy> {
  @Inject private TimeScaleDBService timeScaleDBService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLAnomalyDataList fetch(String accountId, List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy) {
    QLAnomalyDataListBuilder anomaiesList = QLAnomalyDataList.builder();

    if (!timeScaleDBService.isValid()) {
      throw new InvalidRequestException(
          "Cannot process request in K8SAnomaliesDataFetcher since timescaleDBService is invalid");
    }
    try {
      anomaiesList.data(getData(accountId, filters, groupBy));
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching K8SAnomalies data, Exception: {}", e);
    }
    return anomaiesList.build();
  }

  List<QLAnomalyData> getData(String accountId, List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy) {
    List<QLAnomalyData> listAnomalies = new ArrayList<>();
    String queryStatement = "";
    try {
      log.info("Query Step 1/3 : Constructing Query");
      queryStatement = new AnomalyDataQueryBuilder().formK8SQuery(accountId, filters, groupBy);
    } catch (InvalidArgumentsException | InvalidRequestException e) {
      log.error("Error while constructing query for K8SAnomaliesDataFetcher, Exception : {} ", e.toString());
      return listAnomalies;
    }

    boolean successfulRead = false;
    ResultSet resultSet = null;
    int retryCount = 0;
    while (!successfulRead && retryCount < MAX_RETRY) {
      try (Connection dbConnection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = dbConnection.prepareStatement(queryStatement)) {
        log.info("Query Step 2/3 : Statement in k8sAnomaliesDataFetcher: {} ", queryStatement);
        resultSet = statement.executeQuery();
        listAnomalies = extractAnomaliesFromResultSet(resultSet);
        successfulRead = true;
      } catch (SQLException e) {
        retryCount++;
        log.info("Select Query failed after retry count {} , Exception {}", retryCount, e);
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return listAnomalies;
  }

  protected List<QLAnomalyData> extractAnomaliesFromResultSet(ResultSet resultSet) throws SQLException {
    List<QLAnomalyData> qlAnomalyDataList = new ArrayList<>();
    log.info("Query Step 3/3 : conversion of resultset into anomalies");
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
            anomalyBuilder.actualAmount(
                AnomalyDataHelper.getRoundedDoubleValue(resultSet.getDouble(field.getFieldName())));
            break;
          case EXPECTED_COST:
            anomalyBuilder.expectedAmount(
                AnomalyDataHelper.getRoundedDoubleValue(resultSet.getDouble(field.getFieldName())));
            break;
          case NOTE:
            anomalyBuilder.comment(resultSet.getString(field.getFieldName()));
            break;
          case CLUSTER_ID:
            entityDataBuilder.clusterId(resultSet.getString(field.getFieldName()));
            break;
          case NAMESPACE:
            entityDataBuilder.namespace(resultSet.getString(field.getFieldName()));
            break;
          case CLUSTER_NAME:
            entityDataBuilder.clusterName(resultSet.getString(field.getFieldName()));
            break;
          case WORKLOAD_NAME:
            entityDataBuilder.workloadName(resultSet.getString(field.getFieldName()));
            break;
          case WORKLOAD_TYPE:
            entityDataBuilder.workloadType(resultSet.getString(field.getFieldName()));
            break;
          case ANOMALY_SCORE:
            anomalyBuilder.anomalyScore(
                AnomalyDataHelper.getRoundedDoubleValue(resultSet.getDouble(field.getFieldName())));
            break;
          case FEED_BACK:
            if (resultSet.getString(field.getFieldName()) != null) {
              anomalyBuilder.userFeedback(QLAnomalyFeedback.valueOf(resultSet.getString(field.getFieldName())));
            }
            break;
          case ACCOUNT_ID:
          case REPORTED_BY:
          case REGION:
          case GCP_PRODUCT:
          case GCP_PROJECT:
          case GCP_SKU_ID:
          case GCP_SKU_DESCRIPTION:
          case AWS_ACCOUNT:
          case AWS_SERVICE:
          case AWS_INSTANCE_TYPE:
          case AWS_USAGE_TYPE:
          case TIME_GRANULARITY:
            break;
          default:
            log.error("Unknown field : {} encountered while Resultset conversion in K8SAnoamliesDataFetecher", field);
        }
      }
      anomalyBuilder.entity(entityDataBuilder.build());
      qlAnomalyDataList.add(anomalyBuilder.build());
    }

    return qlAnomalyDataList;
  }
}