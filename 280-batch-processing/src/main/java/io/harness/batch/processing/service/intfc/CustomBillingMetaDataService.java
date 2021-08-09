package io.harness.batch.processing.service.intfc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.time.Instant;

@OwnedBy(HarnessTeam.CE)
public interface CustomBillingMetaDataService {
  String getAwsDataSetId(String accountId);
  String getAzureDataSetId(String accountId);
  Boolean checkPipelineJobFinished(String accountId, Instant startTime, Instant endTime);
}
