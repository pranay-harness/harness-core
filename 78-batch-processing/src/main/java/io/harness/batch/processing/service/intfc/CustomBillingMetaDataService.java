package io.harness.batch.processing.service.intfc;

import java.time.Instant;

public interface CustomBillingMetaDataService {
  String getAwsDataSetId(String accountId);

  Boolean checkPipelineJobFinished(String accountId, Instant startTime, Instant endTime);
}
