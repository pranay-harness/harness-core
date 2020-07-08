package io.harness.batch.processing.pricing.gcp.bigquery;

import io.harness.batch.processing.pricing.data.VMInstanceBillingData;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface BigQueryHelperService {
  Map<String, VMInstanceBillingData> getAwsEC2BillingData(
      List<String> resourceId, Instant startTime, Instant endTime, String dataSetId);
}
