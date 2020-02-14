package software.wings.beans.instance;

import lombok.Value;

@Value
public class HarnessServiceInfo {
  private String serviceId;
  private String appId;
  private String cloudProviderId;
  private String envId;
  private String infraMappingId;
  private String deploymentSummaryId;
}
