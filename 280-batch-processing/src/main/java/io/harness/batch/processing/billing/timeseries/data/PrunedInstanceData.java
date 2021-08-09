package io.harness.batch.processing.billing.timeseries.data;

import io.harness.ccm.commons.beans.Resource;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PrunedInstanceData {
  String instanceId;
  String cloudProviderInstanceId;
  Resource totalResource;
  Map<String, String> metaData;
}
