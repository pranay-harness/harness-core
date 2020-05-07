package io.harness.batch.processing.service.impl;

import static com.hazelcast.util.Preconditions.checkFalse;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetInfo;
import com.google.cloud.bigquery.datatransfer.v1.CreateTransferConfigRequest;
import com.google.cloud.bigquery.datatransfer.v1.DataTransferServiceClient;
import com.google.cloud.bigquery.datatransfer.v1.DataTransferServiceSettings;
import com.google.cloud.bigquery.datatransfer.v1.ScheduleOptions;
import com.google.cloud.bigquery.datatransfer.v1.TransferConfig;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Value;

import io.harness.batch.processing.config.AwsDataPipelineConfig;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.service.intfc.BillingDataPipelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class BillingDataPipelineServiceImpl implements BillingDataPipelineService {
  @Autowired BatchMainConfig mainConfig;
  private static final String GOOGLE_CREDENTIALS_PATH = "GOOGLE_CREDENTIALS_PATH";

  private static final String ACCOUNT_NAME_LABEL_KEY = "account_name";
  private static final String ACCOUNT_TYPE_LABEL_KEY = "account_type";
  private static final String DATA_SET_DESCRIPTION_TEMPLATE = "Data set for [ AccountId: %s ], [ AccountName: %s ]";
  private static final String FILE_FORMAT_CONST = "file_format";
  private static final String DATA_PATH_TEMPLATE_CONST = "data_path_template";
  private static final String IGNORE_UNKNOWN_VALUES_CONST = "ignore_unknown_values";
  private static final String FIELD_DELIMITER_CONST = "field_delimiter";
  private static final String SKIP_LEADING_ROWS_CONST = "skip_leading_rows";
  private static final String ALLOWS_QUOTED_NEWLINES_CONST = "allow_quoted_newlines";
  private static final String ALLOWS_JAGGED_ROWS_CONST = "allow_jagged_rows";
  private static final String DELETE_SOURCE_FILES_CONST = "delete_source_files";
  private static final String DEST_TABLE_NAME_CONST = "destination_table_name_template";
  private static final String QUERY_CONST = "query";
  private static final String WRITE_DISPOSITION_CONST = "write_disposition";
  private static final String TEMP_DEST_TABLE_NAME_VALUE = "awsCurTable_{run_time|\"%Y_%m\"}";
  private static final String DEST_TABLE_NAME_VALUE = "awscur_{run_time|\"%Y_%m\"}";
  private static final String PRE_AGG_TABLE_NAME_VALUE = "preAggregated";
  private static final String GCS_DATA_SOURCE_ID = "google_cloud_storage";
  private static final String SCHEDULED_QUERY_DATA_SOURCE_ID = "scheduled_query";
  private static final String WRITE_TRUNCATE_VALUE = "WRITE_TRUNCATE";
  private static final String WRITE_APPEND_VALUE = "WRITE_APPEND";
  private static final String PARENT_TEMPLATE = "projects/%s";
  private static final String DATA_PATH_TEMPLATE = "%s/%s/%s/*/*/*/*/*.csv.gz";
  private static final String TRANSFER_JOB_NAME_TEMPLATE = "gcsToBigQueryTransferJob_%s";
  private static final String SCHEDULED_QUERY_TEMPLATE = "scheduledQuery_%s";
  private static final String DATA_SET_NAME_TEMPLATE = "BillingReport_%s";
  private static final String PRE_AGG_QUERY_TEMPLATE = "preAggQuery_%s";
  public static final String scheduledQueryKey = "scheduledQuery";
  public static final String preAggQueryKey = "preAggQueryKey";
  private static final String TEMP_TABLE_SCHEDULED_QUERY_TEMPLATE =
      "SELECT * FROM `%s.%s.awsCurTable_*` WHERE _TABLE_SUFFIX = CONCAT(CAST(EXTRACT(YEAR from "
      + "TIMESTAMP_TRUNC(TIMESTAMP_SUB(TIMESTAMP (@run_date), INTERVAL 1 DAY), DAY)) as string),'_' , LPAD(CAST(EXTRACT(MONTH from TIMESTAMP_TRUNC(TIMESTAMP_SUB(TIMESTAMP (@run_date), INTERVAL 1 DAY), DAY)) as string),2,'0'));";
  private static final String PRE_AGG_TABLE_SCHEDULED_QUERY_TEMPLATE =
      "SELECT TIMESTAMP_TRUNC(usagestartdate, DAY) as startTime, min(blendedrate) AS awsBlendedRate, sum(blendedcost)"
      + " AS awsBlendedCost, min(unblendedrate) AS awsUnblendedRate, sum(unblendedcost) AS awsUnblendedCost,"
      + " sum(unblendedcost) AS cost, productname AS awsServicecode, region, availabilityzone AS awsAvailabilityzone,"
      + " usageaccountid AS awsUsageaccountid, instancetype AS awsInstancetype, usagetype AS awsUsagetype,"
      + " \"AWS\" AS cloudProvider FROM `%s.%s.awsCurTable_*` WHERE lineitemtype != 'Tax' AND _TABLE_SUFFIX ="
      + " CONCAT(CAST(EXTRACT(YEAR from TIMESTAMP_TRUNC(TIMESTAMP_SUB(TIMESTAMP (@run_date), INTERVAL 1 DAY), DAY)) as string),'_' , LPAD(CAST(EXTRACT(MONTH from TIMESTAMP_TRUNC(TIMESTAMP_SUB(TIMESTAMP (@run_date), INTERVAL 1 DAY), DAY))"
      + " as string),2,'0')) AND TIMESTAMP_TRUNC(usagestartdate, DAY) = TIMESTAMP_TRUNC(TIMESTAMP_SUB(TIMESTAMP (@run_date), INTERVAL 1 DAY), DAY) GROUP"
      + " BY awsServicecode, region, awsAvailabilityzone, awsUsageaccountid, awsInstancetype, awsUsagetype, usagestartdate;";

  public HashMap<String, String> createScheduledQueries(
      String destinationDataSetId, String accountId, String accountName) throws IOException {
    HashMap<String, String> scheduledQueriesMap = new HashMap<>();
    DataTransferServiceClient dataTransferServiceClient = getDataTransferClient();
    AwsDataPipelineConfig awsDataPipelineConfig = mainConfig.getAwsDataPipelineConfig();
    String uniqueSuffixFromAccountId = getUniqueSuffixFromAccountId(accountId, accountName);

    // Create Scheduled query for the Fallback Temp Table
    String scheduledQueryName = String.format(SCHEDULED_QUERY_TEMPLATE, uniqueSuffixFromAccountId);
    scheduledQueriesMap.put(scheduledQueryKey, scheduledQueryName);
    CreateTransferConfigRequest scheduledTransferConfigRequest =
        getTransferConfigRequest(destinationDataSetId, scheduledQueryName, awsDataPipelineConfig, Boolean.FALSE);
    executeDataTransferJobCreate(scheduledTransferConfigRequest, dataTransferServiceClient);

    // Create Scheduled for the Pre-Aggregated Table
    scheduledQueryName = String.format(PRE_AGG_QUERY_TEMPLATE, uniqueSuffixFromAccountId);
    scheduledQueriesMap.put(preAggQueryKey, scheduledQueryName);
    CreateTransferConfigRequest preAggTransferConfigRequest =
        getTransferConfigRequest(destinationDataSetId, scheduledQueryName, awsDataPipelineConfig, Boolean.TRUE);
    executeDataTransferJobCreate(preAggTransferConfigRequest, dataTransferServiceClient);
    return scheduledQueriesMap;
  }

  private CreateTransferConfigRequest getTransferConfigRequest(String destinationDataSetId, String scheduledQueryName,
      AwsDataPipelineConfig awsDataPipelineConfig, Boolean isPreAggregateScheduledQuery) {
    String parent = String.format(PARENT_TEMPLATE, awsDataPipelineConfig.getGcpProjectId());
    String query;
    String writeDisposition;
    String destinationTable;
    int numberOfHours;
    int numberOfMinutes;

    if (isPreAggregateScheduledQuery) {
      query = String.format(
          PRE_AGG_TABLE_SCHEDULED_QUERY_TEMPLATE, awsDataPipelineConfig.getGcpProjectId(), destinationDataSetId);
      writeDisposition = WRITE_APPEND_VALUE;
      destinationTable = PRE_AGG_TABLE_NAME_VALUE;
      numberOfHours = 6;
      numberOfMinutes = 0;
    } else {
      query = String.format(
          TEMP_TABLE_SCHEDULED_QUERY_TEMPLATE, awsDataPipelineConfig.getGcpProjectId(), destinationDataSetId);
      writeDisposition = WRITE_TRUNCATE_VALUE;
      destinationTable = DEST_TABLE_NAME_VALUE;
      numberOfHours = 5;
      numberOfMinutes = 30;
    }

    TransferConfig transferConfig =
        TransferConfig.newBuilder()
            .setDisplayName(scheduledQueryName)
            .setParams(
                Struct.newBuilder()
                    .putFields(QUERY_CONST, Value.newBuilder().setStringValue(query).build())
                    .putFields(WRITE_DISPOSITION_CONST, Value.newBuilder().setStringValue(writeDisposition).build())
                    .putFields(DEST_TABLE_NAME_CONST, Value.newBuilder().setStringValue(destinationTable).build())
                    .build())
            .setDestinationDatasetId(destinationDataSetId)
            .setScheduleOptions(
                ScheduleOptions.newBuilder().setStartTime(getJobStartTimeStamp(numberOfHours, numberOfMinutes)).build())
            .setDataSourceId(SCHEDULED_QUERY_DATA_SOURCE_ID)
            .build();

    return CreateTransferConfigRequest.newBuilder()
        .setServiceAccountName(awsDataPipelineConfig.getGcpServiceAccount())
        .setTransferConfig(transferConfig)
        .setParent(parent)
        .build();
  }

  public String createDataTransferJob(
      String destinationDataSetId, String settingId, String accountId, String accountName) throws IOException {
    DataTransferServiceClient dataTransferServiceClient = getDataTransferClient();
    AwsDataPipelineConfig awsDataPipelineConfig = mainConfig.getAwsDataPipelineConfig();

    String parent = String.format(PARENT_TEMPLATE, awsDataPipelineConfig.getGcpProjectId());
    String transferJobName =
        String.format(TRANSFER_JOB_NAME_TEMPLATE, getUniqueSuffixFromAccountId(accountId, accountName));

    TransferConfig transferConfig =
        TransferConfig.newBuilder()
            .setDisplayName(transferJobName)
            .setParams(Struct.newBuilder()
                           .putFields(DATA_PATH_TEMPLATE_CONST,
                               Value.newBuilder()
                                   .setStringValue(String.format(DATA_PATH_TEMPLATE,
                                       awsDataPipelineConfig.getGcsBasePath(), accountId, settingId))
                                   .build())
                           .putFields(FILE_FORMAT_CONST, Value.newBuilder().setStringValue("CSV").build())
                           .putFields(IGNORE_UNKNOWN_VALUES_CONST, Value.newBuilder().setBoolValue(true).build())
                           .putFields(FIELD_DELIMITER_CONST, Value.newBuilder().setStringValue(",").build())
                           .putFields(SKIP_LEADING_ROWS_CONST, Value.newBuilder().setStringValue("1").build())
                           .putFields(ALLOWS_QUOTED_NEWLINES_CONST, Value.newBuilder().setBoolValue(true).build())
                           .putFields(ALLOWS_JAGGED_ROWS_CONST, Value.newBuilder().setBoolValue(true).build())
                           .putFields(DELETE_SOURCE_FILES_CONST, Value.newBuilder().setBoolValue(true).build())
                           .putFields(DEST_TABLE_NAME_CONST,
                               Value.newBuilder().setStringValue(TEMP_DEST_TABLE_NAME_VALUE).build())
                           .build())
            .setDestinationDatasetId(destinationDataSetId)
            .setScheduleOptions(ScheduleOptions.newBuilder().setStartTime(getJobStartTimeStamp(5, 0)).build())
            .setDataSourceId(GCS_DATA_SOURCE_ID)
            .build();

    CreateTransferConfigRequest createTransferConfigRequest =
        CreateTransferConfigRequest.newBuilder()
            .setServiceAccountName(awsDataPipelineConfig.getGcpServiceAccount())
            .setTransferConfig(transferConfig)
            .setParent(parent)
            .build();
    executeDataTransferJobCreate(createTransferConfigRequest, dataTransferServiceClient);
    return transferJobName;
  }

  private Timestamp getJobStartTimeStamp(int numberOfHours, int numberOfMinutes) {
    long timeStampInSeconds = Instant.now()
                                  .truncatedTo(ChronoUnit.DAYS)
                                  .plus(1, ChronoUnit.DAYS)
                                  .plus(numberOfHours, ChronoUnit.HOURS)
                                  .plus(numberOfMinutes, ChronoUnit.MINUTES)
                                  .getEpochSecond();
    return Timestamp.newBuilder().setSeconds(timeStampInSeconds).build();
  }

  public String createDataSet(String harnessAccountId, String accountName, String masterAccountId, String accountType) {
    String dataSetName = String.format(DATA_SET_NAME_TEMPLATE, masterAccountId);
    try {
      Map<String, String> labelsMap = new HashMap<>();
      labelsMap.put(ACCOUNT_NAME_LABEL_KEY, modifyStringToComplyRegex(accountName));
      labelsMap.put(ACCOUNT_TYPE_LABEL_KEY, modifyStringToComplyRegex(accountType));
      ServiceAccountCredentials credentials = getCredentials();
      BigQuery bigquery = BigQueryOptions.newBuilder().setCredentials(credentials).build().getService();
      DatasetInfo datasetInfo =
          DatasetInfo.newBuilder(dataSetName)
              .setDescription(String.format(DATA_SET_DESCRIPTION_TEMPLATE, harnessAccountId, accountName))
              .setLabels(labelsMap)
              .build();

      Dataset createdDataSet = bigquery.create(datasetInfo);
      return createdDataSet.getDatasetId().getDataset();
    } catch (BigQueryException bigQueryEx) {
      // data set name already exists.
      if (bigQueryEx.getCode() == 409) {
        return dataSetName;
      }
      logger.error("BQ Data Set was not created {} " + bigQueryEx);
    }
    return null;
  }

  private String getUniqueSuffixFromAccountId(String harnessAccountId, String accountName) {
    return modifyStringToComplyRegex(accountName) + "_" + modifyStringToComplyRegex(harnessAccountId);
  }

  private String modifyStringToComplyRegex(String accountInfo) {
    return accountInfo.toLowerCase().replaceAll("[^a-z0-9]", "_");
  }

  @VisibleForTesting
  void executeDataTransferJobCreate(
      CreateTransferConfigRequest request, DataTransferServiceClient dataTransferServiceClient) {
    dataTransferServiceClient.createTransferConfig(request);
  }

  @VisibleForTesting
  ServiceAccountCredentials getCredentials() {
    ServiceAccountCredentials credentials = null;
    String googleCredentialsPath = System.getenv(GOOGLE_CREDENTIALS_PATH);
    checkFalse(isEmpty(googleCredentialsPath), "Missing environment variable for GCP credentials.");
    File credentialsFile = new File(googleCredentialsPath);
    try (FileInputStream serviceAccountStream = new FileInputStream(credentialsFile)) {
      credentials = ServiceAccountCredentials.fromStream(serviceAccountStream);
    } catch (FileNotFoundException e) {
      logger.error("Failed to find Google credential file for the CE service account in the specified path.", e);
    } catch (IOException e) {
      logger.error("Failed to get Google credential file for the CE service account.", e);
    }
    return credentials;
  }

  @VisibleForTesting
  DataTransferServiceClient getDataTransferClient() throws IOException {
    ServiceAccountCredentials credentials = getCredentials();
    DataTransferServiceSettings dataTransferServiceSettings;
    dataTransferServiceSettings = DataTransferServiceSettings.newBuilder()
                                      .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                                      .build();
    return DataTransferServiceClient.create(dataTransferServiceSettings);
  }
}
