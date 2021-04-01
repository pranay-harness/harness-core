package io.harness.ccm.setup.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CE)
public class QLEKSCluster {
  private String id;
  private String name;
  private String region;
  private String infraAccountId;
  private String infraMasterAccountId;
  private String parentAccountSettingId;
}
