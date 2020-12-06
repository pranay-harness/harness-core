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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OverviewAnomaliesDataFetcher extends AbstractAnomalyDataFetcher<QLBillingDataFilter, QLCCMGroupBy> {
  @Inject private TimeScaleDBService dbService;

  private static int ANOMALIES_LIMIT_PER_DAY = 5;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLAnomalyDataList fetch(String accountId, List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy) {
    QLAnomalyDataListBuilder anomaiesList = QLAnomalyDataList.builder();

    if (!dbService.isValid()) {
      throw new InvalidRequestException(
          "Cannot process request in overviewAnomaliesDataFetcher since timescaleDBService is invalid");
    }
    try {
      anomaiesList.data(getData(accountId, filters, groupBy));
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching overviewAnomalies data : {}", e);
    }
    return anomaiesList.build();
  }

  List<QLAnomalyData> getData(String accountId, List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy) {
    List<QLAnomalyData> listAnomalies = new ArrayList<>();
    String queryStatement = "";
    try {
      log.info("Query step 1/3 : Constructing SQL Query");
      queryStatement = new AnomalyDataQueryBuilder().overviewQuery(accountId, filters);
    } catch (InvalidArgumentsException | InvalidRequestException e) {
      log.error("Error while constructing query for OverviewAnomaliesDataFetcher, Exception : {} ", e.toString());
      return listAnomalies;
    }

    boolean successfulRead = false;
    ResultSet resultSet = null;
    int retryCount = 0;
    while (!successfulRead && retryCount < MAX_RETRY) {
      try (Connection dbConnection = dbService.getDBConnection();
           Statement statement = dbConnection.createStatement()) {
        log.info("Query step 2/3 : Constructed Query in OverviewAnomalies Data Fetcher: {} ", queryStatement);
        resultSet = statement.executeQuery(queryStatement);
        listAnomalies = extractAnomaliesFromResultSet(resultSet);
        successfulRead = true;
      } catch (SQLException e) {
        retryCount++;
        log.info("Query failed after retry count : {} , Exception : {} ", retryCount, e);
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return listAnomalies;
  }

  protected List<QLAnomalyData> extractAnomaliesFromResultSet(ResultSet resultSet) throws SQLException {
    List<QLAnomalyData> qlAnomalyDataList = new ArrayList<>();
    log.info("Query Step 3/3 : conversion from resultset to anomalies");
    while (null != resultSet && resultSet.next()) {
      QLAnomalyDataBuilder anomalyBuilder = QLAnomalyData.builder();
      QLEntityInfoBuilder entityDataBuilder = QLEntityInfo.builder();
      for (AnomaliesDataTableSchema.fields field : AnomaliesDataTableSchema.getFields()) {
        switch (field) {
          case ID:
            anomalyBuilder.id(resultSet.getString(field.getFieldName()));
            break;
          case FEED_BACK:
            if (resultSet.getString(field.getFieldName()) != null) {
              anomalyBuilder.userFeedback(QLAnomalyFeedback.valueOf(resultSet.getString(field.getFieldName())));
            }
            break;
          case ANOMALY_SCORE:
            anomalyBuilder.anomalyScore(
                AnomalyDataHelper.getRoundedDoubleValue(resultSet.getDouble(field.getFieldName())));
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
          case REGION:
          case REPORTED_BY:
          case TIME_GRANULARITY:
          case AWS_USAGE_TYPE:
          case ACCOUNT_ID:
          case AWS_INSTANCE_TYPE:
            break;
          default:
            log.error("Unknown field [{}] converstion unecountered in Overview AnoamliesDataFetecher", field);
        }
      }
      anomalyBuilder.entity(entityDataBuilder.build());
      qlAnomalyDataList.add(anomalyBuilder.build());
    }

    Iterator<QLAnomalyData> iter = qlAnomalyDataList.iterator();
    int counter = 0;
    QLAnomalyData current;
    Long currentDate = null;
    while (iter.hasNext()) {
      current = iter.next();
      if (currentDate == null || current.getTime() > currentDate) {
        counter = 1;
        currentDate = current.getTime();
      } else if (counter < ANOMALIES_LIMIT_PER_DAY) {
        counter++;
      } else {
        iter.remove();
      }
    }

    return qlAnomalyDataList;
  }
}
