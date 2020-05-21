package io.harness.batch.processing.service.impl;

import static io.harness.ccm.billing.GcpServiceAccountServiceImpl.getCredentials;
import static io.harness.ccm.billing.GcpServiceAccountServiceImpl.getImpersonatedCredentials;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
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

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.batch.processing.service.intfc.BillingDataPipelineService;
import io.harness.ccm.cluster.entities.BillingDataPipelineRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.wings.beans.Account;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class BillingDataPipelineServiceImpl implements BillingDataPipelineService {
  private static final String GOOGLE_CREDENTIALS_PATH = "GOOGLE_CREDENTIALS_PATH";

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
  private static final String AWS_PRE_AGG_QUERY_TEMPLATE = "awsPreAggQuery_%s";

  private static final String ACCOUNT_NAME_LABEL_KEY = "account_name";
  private static final String ACCOUNT_TYPE_LABEL_KEY = "account_type";
  public static final String scheduledQueryKey = "scheduledQuery";
  public static final String preAggQueryKey = "preAggQueryKey";
  private static final String PAID_ACCOUNT_TYPE = "PAID";
  public static final String DATA_SET_NAME_TEMPLATE = "BillingReport_%s";
  private static final String TEMP_TABLE_SCHEDULED_QUERY_TEMPLATE =
      "SELECT * FROM `%s.%s.awsCurTable_*` WHERE _TABLE_SUFFIX = CONCAT(CAST(EXTRACT(YEAR from "
      + "TIMESTAMP_TRUNC(TIMESTAMP_SUB(TIMESTAMP (@run_date), INTERVAL 1 DAY), DAY)) as string),'_' , LPAD(CAST(EXTRACT(MONTH from TIMESTAMP_TRUNC(TIMESTAMP_SUB(TIMESTAMP (@run_date), INTERVAL 1 DAY), DAY)) as string),2,'0'));";
  private static final String AWS_PRE_AGG_TABLE_SCHEDULED_QUERY_TEMPLATE =
      "SELECT TIMESTAMP_TRUNC(usagestartdate, DAY) as startTime, min(blendedrate) AS awsBlendedRate, sum(blendedcost)"
      + " AS awsBlendedCost, min(unblendedrate) AS awsUnblendedRate, sum(unblendedcost) AS awsUnblendedCost,"
      + " sum(unblendedcost) AS cost, productname AS awsServicecode, region, availabilityzone AS awsAvailabilityzone,"
      + " usageaccountid AS awsUsageaccountid, instancetype AS awsInstancetype, usagetype AS awsUsagetype,"
      + " \"AWS\" AS cloudProvider FROM `%s.%s.awscur_*` WHERE lineitemtype != 'Tax' AND _TABLE_SUFFIX ="
      + " CONCAT(CAST(EXTRACT(YEAR from TIMESTAMP_TRUNC(TIMESTAMP_SUB(TIMESTAMP (@run_date), INTERVAL 1 DAY), DAY)) as string),'_' , LPAD(CAST(EXTRACT(MONTH from TIMESTAMP_TRUNC(TIMESTAMP_SUB(TIMESTAMP (@run_date), INTERVAL 1 DAY), DAY))"
      + " as string),2,'0')) AND TIMESTAMP_TRUNC(usagestartdate, DAY) = TIMESTAMP_TRUNC(TIMESTAMP_SUB(TIMESTAMP (@run_date), INTERVAL 1 DAY), DAY) GROUP"
      + " BY awsServicecode, region, awsAvailabilityzone, awsUsageaccountid, awsInstancetype, awsUsagetype, startTime;";

  private static final String GCP_PRE_AGG_TABLE_SCHEDULED_QUERY_TEMPLATE =
      "SELECT SUM(cost) AS cost, service.description AS gcpProduct, sku.id AS gcpSkuId, "
      + "sku.description AS gcpSkuDescription, TIMESTAMP_TRUNC(usage_start_time, DAY) as startTime, "
      + "project.id AS gcpProjectId, location.region AS region, location.zone AS zone, "
      + "billing_account_id AS gcpBillingAccountId, \"GCP\" AS cloudProvider "
      + "FROM `%s.%s.gcp_billing_export*` "
      + "WHERE DATE(usage_start_time) = DATE_SUB(CAST(FORMAT_DATE('%%Y-%%m-%%d', @run_date) AS DATE), INTERVAL 1 DAY) "
      + "GROUP BY service.description, sku.id, sku.description, startTime, project.id, "
      + "location.region, location.zone, billing_account_id;";

  private BatchMainConfig mainConfig;
  private BillingDataPipelineRecordDao billingDataPipelineRecordDao;

  @Autowired
  public BillingDataPipelineServiceImpl(
      BatchMainConfig mainConfig, BillingDataPipelineRecordDao billingDataPipelineRecordDao) {
    this.mainConfig = mainConfig;
    this.billingDataPipelineRecordDao = billingDataPipelineRecordDao;
  }

  public static String getDataSetDescription(String harnessAccountId, String accountName) {
    return String.format(DATA_SET_DESCRIPTION_TEMPLATE, harnessAccountId, accountName);
  }

  public String createDataSet(Account account) {
    String accountId = account.getUuid();
    BillingDataPipelineRecord billingDataPipelineRecord = billingDataPipelineRecordDao.getByAccountId(accountId);
    if (billingDataPipelineRecord != null) {
      return billingDataPipelineRecord.getDataSetId();
    }
    String accountName = account.getAccountName();
    String dataSetName = String.format(DATA_SET_NAME_TEMPLATE, modifyStringToComplyRegex(accountId));
    String description = getDataSetDescription(accountId, accountName);
    Map<String, String> labelMap = getLabelMap(accountName, getAccountType(account));
    try {
      ServiceAccountCredentials credentials = getCredentials(GOOGLE_CREDENTIALS_PATH);
      BigQuery bigquery = BigQueryOptions.newBuilder().setCredentials(credentials).build().getService();
      DatasetInfo datasetInfo =
          DatasetInfo.newBuilder(dataSetName).setDescription(description).setLabels(labelMap).build();

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

  @Override
  public void createScheduledQueriesForGCP(String scheduledQueryName, String dstDataSetId) throws IOException {
    String gcpProjectId = mainConfig.getBillingDataPipelineConfig().getGcpProjectId();
    DataTransferServiceClient dataTransferServiceClient = getDataTransferClient();
    String query = String.format(GCP_PRE_AGG_TABLE_SCHEDULED_QUERY_TEMPLATE, gcpProjectId, dstDataSetId);
    CreateTransferConfigRequest scheduledTransferConfigRequest =
        getTransferConfigRequest(dstDataSetId, scheduledQueryName, true, query);
    executeDataTransferJobCreate(scheduledTransferConfigRequest, dataTransferServiceClient);
  }

  @Override
  public HashMap<String, String> createScheduledQueriesForAWS(String dstDataSetId, String accountId, String accountName)
      throws IOException {
    String gcpProjectId = mainConfig.getBillingDataPipelineConfig().getGcpProjectId();
    String uniqueSuffixFromAccountId = getUniqueSuffixFromAccountId(accountId, accountName);

    HashMap<String, String> scheduledQueriesMap = new HashMap<>();
    DataTransferServiceClient dataTransferServiceClient = getDataTransferClient();

    // Create Scheduled query for the Fallback Temp Table
    String scheduledQueryName1 = String.format(SCHEDULED_QUERY_TEMPLATE, uniqueSuffixFromAccountId);
    String query1 = String.format(TEMP_TABLE_SCHEDULED_QUERY_TEMPLATE, gcpProjectId, dstDataSetId);
    CreateTransferConfigRequest scheduledTransferConfigRequest =
        getTransferConfigRequest(dstDataSetId, scheduledQueryName1, false, query1);
    executeDataTransferJobCreate(scheduledTransferConfigRequest, dataTransferServiceClient);
    scheduledQueriesMap.put(scheduledQueryKey, scheduledQueryName1);

    // Create Scheduled query for the Pre-Aggregated Table
    String scheduledQueryName2 = String.format(AWS_PRE_AGG_QUERY_TEMPLATE, uniqueSuffixFromAccountId);
    String query2 = String.format(AWS_PRE_AGG_TABLE_SCHEDULED_QUERY_TEMPLATE, gcpProjectId, dstDataSetId);
    CreateTransferConfigRequest preAggTransferConfigRequest =
        getTransferConfigRequest(dstDataSetId, scheduledQueryName2, true, query2);
    executeDataTransferJobCreate(preAggTransferConfigRequest, dataTransferServiceClient);
    scheduledQueriesMap.put(preAggQueryKey, scheduledQueryName2);
    return scheduledQueriesMap;
  }

  private CreateTransferConfigRequest getTransferConfigRequest(
      String dstDataSetId, String scheduledQueryName, boolean isPreAggregateScheduledQuery, String query) {
    String gcpProjectId = mainConfig.getBillingDataPipelineConfig().getGcpProjectId();
    String parent = String.format(PARENT_TEMPLATE, gcpProjectId);
    String writeDisposition;
    String destinationTable;
    int numberOfHours;
    int numberOfMinutes;

    if (isPreAggregateScheduledQuery) {
      writeDisposition = WRITE_APPEND_VALUE;
      destinationTable = PRE_AGG_TABLE_NAME_VALUE;
      numberOfHours = 6;
      numberOfMinutes = 30;
    } else {
      writeDisposition = WRITE_TRUNCATE_VALUE;
      destinationTable = DEST_TABLE_NAME_VALUE;
      numberOfHours = 6;
      numberOfMinutes = 15;
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
            .setDestinationDatasetId(dstDataSetId)
            .setScheduleOptions(
                ScheduleOptions.newBuilder().setStartTime(getJobStartTimeStamp(numberOfHours, numberOfMinutes)).build())
            .setDataSourceId(SCHEDULED_QUERY_DATA_SOURCE_ID)
            .build();

    return CreateTransferConfigRequest.newBuilder().setTransferConfig(transferConfig).setParent(parent).build();
  }

  @Override
  public void createDataTransferJobFromBQ(String jobName, String srcProjectId, String srcDatasetId, String dstProjectId,
      String dstDatasetId, String impersonatedServiceAccount) throws IOException {
    ServiceAccountCredentials sourceCredentials = getCredentials(GOOGLE_CREDENTIALS_PATH);
    Credentials credentials = getImpersonatedCredentials(sourceCredentials, impersonatedServiceAccount);
    DataTransferServiceClient client = getDataTransferClient(credentials);

    String parent = String.format(PARENT_TEMPLATE, dstProjectId);
    TransferConfig transferConfig =
        TransferConfig.newBuilder()
            .setDisplayName(jobName)
            .setDataSourceId("cross_region_copy")
            .setDestinationDatasetId(dstDatasetId)
            .setParams(Struct.newBuilder()
                           .putFields("source_project_id", Value.newBuilder().setStringValue(srcProjectId).build())
                           .putFields("source_dataset_id", Value.newBuilder().setStringValue(srcDatasetId).build())
                           .build())
            .setScheduleOptions(ScheduleOptions.newBuilder().setStartTime(getJobStartTimeStamp(6, 0)).build())
            .build();
    CreateTransferConfigRequest request =
        CreateTransferConfigRequest.newBuilder().setTransferConfig(transferConfig).setParent(parent).build();
    executeDataTransferJobCreate(request, client);
  }

  public String createDataTransferJobFromGCS(
      String destinationDataSetId, String settingId, String accountId, String accountName) throws IOException {
    DataTransferServiceClient dataTransferServiceClient = getDataTransferClient();
    String gcpProjectId = mainConfig.getBillingDataPipelineConfig().getGcpProjectId();
    String parent = String.format(PARENT_TEMPLATE, gcpProjectId);
    String transferJobName =
        String.format(TRANSFER_JOB_NAME_TEMPLATE, getUniqueSuffixFromAccountId(accountId, accountName));

    TransferConfig transferConfig =
        TransferConfig.newBuilder()
            .setDisplayName(transferJobName)
            .setParams(
                Struct.newBuilder()
                    .putFields(DATA_PATH_TEMPLATE_CONST,
                        Value.newBuilder()
                            .setStringValue(String.format(DATA_PATH_TEMPLATE,
                                mainConfig.getBillingDataPipelineConfig().getGcsBasePath(), accountId, settingId))
                            .build())
                    .putFields(FILE_FORMAT_CONST, Value.newBuilder().setStringValue("CSV").build())
                    .putFields(IGNORE_UNKNOWN_VALUES_CONST, Value.newBuilder().setBoolValue(true).build())
                    .putFields(FIELD_DELIMITER_CONST, Value.newBuilder().setStringValue(",").build())
                    .putFields(SKIP_LEADING_ROWS_CONST, Value.newBuilder().setStringValue("1").build())
                    .putFields(ALLOWS_QUOTED_NEWLINES_CONST, Value.newBuilder().setBoolValue(true).build())
                    .putFields(ALLOWS_JAGGED_ROWS_CONST, Value.newBuilder().setBoolValue(true).build())
                    .putFields(DELETE_SOURCE_FILES_CONST, Value.newBuilder().setBoolValue(true).build())
                    .putFields(
                        DEST_TABLE_NAME_CONST, Value.newBuilder().setStringValue(TEMP_DEST_TABLE_NAME_VALUE).build())
                    .build())
            .setDestinationDatasetId(destinationDataSetId)
            .setScheduleOptions(ScheduleOptions.newBuilder().setStartTime(getJobStartTimeStamp(6, 0)).build())
            .setDataSourceId(GCS_DATA_SOURCE_ID)
            .build();

    CreateTransferConfigRequest createTransferConfigRequest =
        CreateTransferConfigRequest.newBuilder().setTransferConfig(transferConfig).setParent(parent).build();
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

  private String getUniqueSuffixFromAccountId(String harnessAccountId, String accountName) {
    return modifyStringToComplyRegex(accountName) + "_" + modifyStringToComplyRegex(harnessAccountId);
  }

  public String modifyStringToComplyRegex(String accountInfo) {
    return accountInfo.toLowerCase().replaceAll("[^a-z0-9]", "_");
  }

  public Map<String, String> getLabelMap(String accountName, String accountType) {
    Map<String, String> labelMap = new HashMap<>();
    labelMap.put(ACCOUNT_NAME_LABEL_KEY, modifyStringToComplyRegex(accountName));
    labelMap.put(ACCOUNT_TYPE_LABEL_KEY, modifyStringToComplyRegex(accountType));
    return labelMap;
  }

  public String getAccountType(Account accountInfo) {
    if (accountInfo.getLicenseInfo() != null) {
      return accountInfo.getLicenseInfo().getAccountType();
    }
    return PAID_ACCOUNT_TYPE;
  }

  @VisibleForTesting
  void executeDataTransferJobCreate(
      CreateTransferConfigRequest request, DataTransferServiceClient dataTransferServiceClient) {
    dataTransferServiceClient.createTransferConfig(request);
  }

  DataTransferServiceClient getDataTransferClient() throws IOException {
    ServiceAccountCredentials sourceCredentials = getCredentials(GOOGLE_CREDENTIALS_PATH);
    Credentials credentials = getImpersonatedCredentials(sourceCredentials, null);
    return getDataTransferClient(credentials);
  }

  @VisibleForTesting
  DataTransferServiceClient getDataTransferClient(Credentials credentials) throws IOException {
    DataTransferServiceSettings dataTransferServiceSettings =
        DataTransferServiceSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
            .build();
    return DataTransferServiceClient.create(dataTransferServiceSettings);
  }
}
