package io.harness.batch.processing.pricing.aws.athena.impl;

import static io.harness.batch.processing.pricing.aws.athena.AthenaQueryConstants.ATHENA_COMPUTE_INSTANCE_PRICE_QUERY;
import static io.harness.batch.processing.pricing.aws.athena.AthenaQueryConstants.ATHENA_DEFAULT_DATABASE;
import static io.harness.batch.processing.pricing.aws.athena.AthenaQueryConstants.ATHENA_ECS_FARGATE_PRICE_QUERY;
import static io.harness.batch.processing.pricing.aws.athena.AthenaQueryConstants.ATHENA_OUTPUT_BUCKET;
import static io.harness.batch.processing.pricing.aws.athena.AthenaQueryConstants.SLEEP_AMOUNT_IN_MS;

import com.google.common.annotations.VisibleForTesting;

import io.harness.batch.processing.BatchProcessingException;
import io.harness.batch.processing.pricing.aws.athena.AwsAthenaQueryHelperService;
import io.harness.batch.processing.pricing.data.AccountComputePricingData;
import io.harness.batch.processing.pricing.data.AccountFargatePricingData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.AthenaClientBuilder;
import software.amazon.awssdk.services.athena.model.Datum;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionResponse;
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest;
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.athena.model.QueryExecutionContext;
import software.amazon.awssdk.services.athena.model.QueryExecutionState;
import software.amazon.awssdk.services.athena.model.ResultConfiguration;
import software.amazon.awssdk.services.athena.model.Row;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse;
import software.amazon.awssdk.services.athena.paginators.GetQueryResultsIterable;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Service
public class AwsAthenaQueryHelperServiceImpl implements AwsAthenaQueryHelperService {
  private final AthenaClientBuilder builder =
      AthenaClient.builder().region(Region.US_EAST_2).credentialsProvider(ProfileCredentialsProvider.create());

  @Override
  public List<AccountComputePricingData> fetchComputePriceRate(
      String settingId, String billingAccountId, Instant startInstant) throws InterruptedException {
    AthenaClient athenaClient = createAthenaClient();
    String computeInstanceQuery =
        String.format(ATHENA_COMPUTE_INSTANCE_PRICE_QUERY, billingAccountId, getFormattedDate(startInstant));
    String bucketPath = String.format(ATHENA_OUTPUT_BUCKET, billingAccountId, settingId);
    String queryExecutionId =
        submitAthenaQuery(ATHENA_DEFAULT_DATABASE, bucketPath, computeInstanceQuery, athenaClient);
    waitForQueryToComplete(athenaClient, queryExecutionId);
    return processComputeResultRows(athenaClient, queryExecutionId);
  }

  @Override
  public List<AccountFargatePricingData> fetchEcsFargatePriceRate(
      String settingId, String billingAccountId, Instant startInstant) throws InterruptedException {
    AthenaClient athenaClient = createAthenaClient();
    String computeInstanceQuery =
        String.format(ATHENA_ECS_FARGATE_PRICE_QUERY, billingAccountId, getFormattedDate(startInstant));
    String bucketPath = String.format(ATHENA_OUTPUT_BUCKET, billingAccountId, settingId);
    String queryExecutionId =
        submitAthenaQuery(ATHENA_DEFAULT_DATABASE, bucketPath, computeInstanceQuery, athenaClient);
    waitForQueryToComplete(athenaClient, queryExecutionId);
    return processFargateResultRows(athenaClient, queryExecutionId);
  }

  private String submitAthenaQuery(
      String database, String outputBucket, String athenaQuery, AthenaClient athenaClient) {
    QueryExecutionContext queryExecutionContext = QueryExecutionContext.builder().database(database).build();
    ResultConfiguration resultConfiguration = ResultConfiguration.builder().outputLocation(outputBucket).build();
    logger.info("Athena Query {}", athenaQuery);
    StartQueryExecutionRequest startQueryExecutionRequest = StartQueryExecutionRequest.builder()
                                                                .queryString(athenaQuery)
                                                                .queryExecutionContext(queryExecutionContext)
                                                                .resultConfiguration(resultConfiguration)
                                                                .build();
    StartQueryExecutionResponse startQueryExecutionResponse =
        athenaClient.startQueryExecution(startQueryExecutionRequest);
    return startQueryExecutionResponse.queryExecutionId();
  }

  @VisibleForTesting
  AthenaClient createAthenaClient() {
    return builder.build();
  }

  private static void waitForQueryToComplete(AthenaClient athenaClient, String queryExecutionId)
      throws InterruptedException {
    GetQueryExecutionRequest getQueryExecutionRequest =
        GetQueryExecutionRequest.builder().queryExecutionId(queryExecutionId).build();

    GetQueryExecutionResponse getQueryExecutionResponse;
    boolean isQueryStillRunning = true;
    while (isQueryStillRunning) {
      getQueryExecutionResponse = athenaClient.getQueryExecution(getQueryExecutionRequest);
      String queryState = getQueryExecutionResponse.queryExecution().status().state().toString();
      if (queryState.equals(QueryExecutionState.FAILED.toString())) {
        throw new BatchProcessingException("Query Failed to run with Error Message: "
                + getQueryExecutionResponse.queryExecution().status().stateChangeReason(),
            null);
      } else if (queryState.equals(QueryExecutionState.CANCELLED.toString())) {
        throw new BatchProcessingException("Query was cancelled.", null);
      } else if (queryState.equals(QueryExecutionState.SUCCEEDED.toString())) {
        isQueryStillRunning = false;
      } else {
        Thread.sleep(SLEEP_AMOUNT_IN_MS);
      }
      logger.info("Current Status is: {} ", queryState);
    }
  }

  private List<AccountComputePricingData> processComputeResultRows(AthenaClient athenaClient, String queryExecutionId) {
    GetQueryResultsRequest getQueryResultsRequest =
        GetQueryResultsRequest.builder().queryExecutionId(queryExecutionId).build();
    GetQueryResultsIterable getQueryResultsIterable = athenaClient.getQueryResultsPaginator(getQueryResultsRequest);
    List<AccountComputePricingData> accountComputePricingDataList = new ArrayList<>();
    for (GetQueryResultsResponse queryResult : getQueryResultsIterable) {
      List<Row> results = queryResult.resultSet().rows();
      processComputeRow(results, accountComputePricingDataList);
    }
    return accountComputePricingDataList;
  }

  private List<AccountFargatePricingData> processFargateResultRows(AthenaClient athenaClient, String queryExecutionId) {
    GetQueryResultsRequest getQueryResultsRequest =
        GetQueryResultsRequest.builder().queryExecutionId(queryExecutionId).build();
    GetQueryResultsIterable getQueryResultsIterable = athenaClient.getQueryResultsPaginator(getQueryResultsRequest);
    List<AccountFargatePricingData> accountFargatePricingDataList = new ArrayList<>();
    for (GetQueryResultsResponse queryResult : getQueryResultsIterable) {
      List<Row> results = queryResult.resultSet().rows();
      processFargateRow(results, accountFargatePricingDataList);
    }
    return accountFargatePricingDataList;
  }

  private List<AccountComputePricingData> processComputeRow(
      List<Row> resultRows, List<AccountComputePricingData> accountComputePricingDataList) {
    Iterator<Row> iterator = resultRows.iterator();
    iterator.next();
    iterator.forEachRemaining(resultRow -> {
      List<Datum> resultData = resultRow.data();
      AccountComputePricingData accountComputePricingData = AccountComputePricingData.builder()
                                                                .blendedRate(getDoubleValue(resultData, 0))
                                                                .blendedCost(getDoubleValue(resultData, 1))
                                                                .unBlendedRate(getDoubleValue(resultData, 2))
                                                                .unBlendedCost(getDoubleValue(resultData, 3))
                                                                .availabilityZone(getStringValue(resultData, 4))
                                                                .instanceType(getStringValue(resultData, 5))
                                                                .operatingSystem(getStringValue(resultData, 6))
                                                                .region(getStringValue(resultData, 7))
                                                                .cpusPerVm(getDoubleValue(resultData, 8))
                                                                .memPerVm(getMemoryValue(resultData, 9))
                                                                .build();
      accountComputePricingDataList.add(accountComputePricingData);
      logger.info("Account pricing data {} ", accountComputePricingData.toString());
    });
    return accountComputePricingDataList;
  }

  private List<AccountFargatePricingData> processFargateRow(
      List<Row> resultRows, List<AccountFargatePricingData> accountFargatePricingDataList) {
    Iterator<Row> iterator = resultRows.iterator();
    iterator.next();
    iterator.forEachRemaining(resultRow -> {
      List<Datum> resultData = resultRow.data();
      AccountFargatePricingData accountFargatePricingData = AccountFargatePricingData.builder()
                                                                .blendedRate(getDoubleValue(resultData, 0))
                                                                .blendedCost(getDoubleValue(resultData, 1))
                                                                .unBlendedRate(getDoubleValue(resultData, 2))
                                                                .unBlendedCost(getDoubleValue(resultData, 3))
                                                                .region(getStringValue(resultData, 4))
                                                                .cpuPriceType(getBooleanValue(resultData, 5))
                                                                .memoryPriceType(getBooleanValue(resultData, 6))
                                                                .build();
      accountFargatePricingDataList.add(accountFargatePricingData);
      logger.info("Account pricing data {} ", accountFargatePricingData.toString());
    });
    return accountFargatePricingDataList;
  }

  private double getMemoryValue(List<Datum> resultData, int index) {
    String memoryString = getStringValue(resultData, index);
    return Double.parseDouble(memoryString.split(" ")[0]);
  }

  private boolean getBooleanValue(List<Datum> resultData, int index) {
    return !getStringValue(resultData, index).equals("");
  }

  private double getDoubleValue(List<Datum> resultData, int index) {
    return Double.parseDouble(resultData.get(index).varCharValue());
  }

  private String getStringValue(List<Datum> resultData, int index) {
    return resultData.get(index).varCharValue();
  }

  private String getFormattedDate(Instant instant) {
    Date date = Date.from(instant);
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
    return formatter.format(date);
  }
}
