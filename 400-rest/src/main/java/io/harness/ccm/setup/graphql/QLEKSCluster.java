package io.harness.ccm.setup.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public class QLEKSCluster {
  private String id;
  private String name;
  private String region;
  private String infraAccountId;
  private String infraMasterAccountId;
  private String parentAccountSettingId;
}
