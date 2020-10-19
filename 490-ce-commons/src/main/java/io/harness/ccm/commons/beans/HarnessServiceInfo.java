package io.harness.ccm.commons.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class HarnessServiceInfo {
  String serviceId;
  String appId;
  String cloudProviderId;
  String envId;
  String infraMappingId;
  String deploymentSummaryId;
}
