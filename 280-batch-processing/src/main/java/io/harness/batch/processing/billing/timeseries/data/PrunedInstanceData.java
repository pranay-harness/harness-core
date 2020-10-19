package io.harness.batch.processing.billing.timeseries.data;

import io.harness.ccm.commons.beans.Resource;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class PrunedInstanceData {
  String instanceId;
  String cloudProviderInstanceId;
  Resource totalResource;
  Map<String, String> metaData;
}
