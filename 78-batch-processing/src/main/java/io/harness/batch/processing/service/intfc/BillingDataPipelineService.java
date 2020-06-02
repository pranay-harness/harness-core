package io.harness.batch.processing.service.intfc;

import com.google.cloud.bigquery.datatransfer.v1.DataTransferServiceClient;

import software.wings.beans.Account;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public interface BillingDataPipelineService {
  String createDataTransferJobFromGCS(
      String destinationDataSetId, String settingId, String accountId, String accountName) throws IOException;
  void createDataTransferJobFromBQ(String jobName, String srcProjectId, String srcDatasetId, String dstProjectId,
      String dstDatasetId, String impersonatedServiceAccount) throws IOException;
  void createScheduledQueriesForGCP(String scheduledQueryName, String dstDataSetId) throws IOException;
  HashMap<String, String> createScheduledQueriesForAWS(String dstDataSetId, String accountId, String accountName)
      throws IOException;
  String modifyStringToComplyRegex(String accountInfo);
  Map<String, String> getLabelMap(String accountName, String accountType);
  String getAccountType(Account accountInfo);
  String createDataSet(Account account);
  DataTransferServiceClient getDataTransferClient() throws IOException;
}
