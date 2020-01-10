package io.harness.batch.processing.billing.timeseries.data;

import io.harness.batch.processing.ccm.Resource;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PrunedInstanceData {
  String instanceId;
  Resource totalResource;
}
