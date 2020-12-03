package software.wings.graphql.datafetcher.anomaly;

import io.harness.timescaledb.TimeScaleDBService;

import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyData;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyData.QLAnomalyDataBuilder;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyFeedback;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyInput;
import software.wings.graphql.schema.type.aggregation.anomaly.QLUpdateAnomalyPayLoad;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateAnomalyDataFetcher extends BaseMutatorDataFetcher<QLAnomalyInput, QLUpdateAnomalyPayLoad> {
  private TimeScaleDBService dbService;

  @Inject
  public UpdateAnomalyDataFetcher(TimeScaleDBService timeScaleDBService) {
    super(QLAnomalyInput.class, QLUpdateAnomalyPayLoad.class);
    dbService = timeScaleDBService;
  }

  int MAX_RETRY_COUNT = 3;
  AnomalyDataQueryBuilder queryBuilder = new AnomalyDataQueryBuilder();

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLUpdateAnomalyPayLoad mutateAndFetch(QLAnomalyInput input, MutationContext mutationContext) {
    if (!dbService.isValid()) {
      log.error("Timescale Db is not valid cannot mutation cannot be proceeded ");
      return null;
    }
    QLUpdateAnomalyPayLoad payLoad;
    updateAnomaly(mutationContext.getAccountId(), input);
    payLoad = getUpdatedAnomaly(mutationContext.getAccountId(), input);
    return payLoad;
  }

  private void updateAnomaly(String accountId, QLAnomalyInput input) {
    String query = queryBuilder.formAnomalyUpdateQuery(accountId, input);

    int retryCount = 0;
    int count = 0;
    boolean successfulRead = false;
    while (!successfulRead && retryCount < MAX_RETRY_COUNT) {
      try (Connection dbConnection = dbService.getDBConnection();
           Statement statement = dbConnection.createStatement()) {
        log.info("Mutation step 1/2 : Prepared Statement in updateAnomalyDataFetcher: {} ", statement);
        statement.addBatch(query);
        count = statement.executeUpdate(query);
        log.info(" Update Query status : {} , after retry count : {}", count, retryCount + 1);
        if (count > 0) {
          successfulRead = true;
        }
      } catch (SQLException e) {
        retryCount++;
      }
    }
  }

  private QLUpdateAnomalyPayLoad getUpdatedAnomaly(String accountId, QLAnomalyInput input) {
    QLAnomalyData anomalyData = null;
    String query = queryBuilder.formAnomalyFetchQuery(accountId, input);
    ResultSet resultSet;

    int retryCount = 0;
    boolean successfulRead = false;
    while (!successfulRead && retryCount < MAX_RETRY_COUNT) {
      try (Connection dbConnection = dbService.getDBConnection();
           Statement statement = dbConnection.createStatement()) {
        log.info("Mutation step 2/2 :Prepared Statement in updateAnomalyDataFetcher: {} ", statement);
        resultSet = statement.executeQuery(query);
        anomalyData = extractAnomalyData(resultSet);
        successfulRead = true;
      } catch (SQLException e) {
        retryCount++;
        log.info(" Select Query status failed after retry count : {} , Exception {}", retryCount, e);
      }
    }
    return QLUpdateAnomalyPayLoad.builder().anomaly(anomalyData).build();
  }

  private QLAnomalyData extractAnomalyData(ResultSet resultSet) throws SQLException {
    QLAnomalyDataBuilder anomalyDataBuilder = QLAnomalyData.builder();

    if (!resultSet.next()) {
      log.error("No anomalies found while fetching from db after mutation changes");
      return anomalyDataBuilder.build();
    }
    anomalyDataBuilder.id(resultSet.getString(AnomaliesDataTableSchema.fields.ID.getFieldName()));
    anomalyDataBuilder.comment(resultSet.getString(AnomaliesDataTableSchema.fields.NOTE.getFieldName()));
    String feedback = resultSet.getString(AnomaliesDataTableSchema.fields.FEED_BACK.getFieldName());
    if (feedback != null) {
      anomalyDataBuilder.userFeedback(QLAnomalyFeedback.valueOf(feedback));
    }
    return anomalyDataBuilder.build();
  }
}
