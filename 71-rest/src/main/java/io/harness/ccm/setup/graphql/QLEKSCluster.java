package io.harness.ccm.setup.graphql;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLEKSCluster {
  private String id;
  private String name;
  private String region;
  private String infraAccountId;
  private String cloudProviderId;
  private String infraMasterAccountId;
  private String parentAccountSettingId;
}
