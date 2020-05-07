package io.harness.batch.processing.service.intfc;

import java.io.IOException;
import java.util.HashMap;

public interface BillingDataPipelineService {
  String createDataTransferJob(String destinationDataSetId, String settingId, String accountId, String accountName)
      throws IOException;
  HashMap<String, String> createScheduledQueries(String destinationDataSetId, String accountId, String accountName)
      throws IOException;
  String createDataSet(String harnessAccountId, String accountName, String masterAccountId, String accountType);
}
