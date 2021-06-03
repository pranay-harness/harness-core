package io.harness.ccm.setup.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.commons.entities.billing.CECloudAccount.AccountStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public class QLLinkedAccount {
  private String id;
  private String name;
  private String arn;
  private String masterAccountId;
  private AccountStatus accountStatus;
}
