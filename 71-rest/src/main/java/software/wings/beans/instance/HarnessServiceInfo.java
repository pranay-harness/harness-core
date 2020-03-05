package software.wings.beans.instance;

import lombok.Value;

@Value
public class HarnessServiceInfo {
  String serviceId;
  String appId;
  String cloudProviderId;
  String envId;
  String infraMappingId;
  String deploymentSummaryId;
}
