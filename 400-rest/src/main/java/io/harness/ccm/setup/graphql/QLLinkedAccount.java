package io.harness.ccm.setup.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.ce.CECloudAccount.AccountStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CE)
public class QLLinkedAccount {
  private String id;
  private String name;
  private String arn;
  private String masterAccountId;
  private AccountStatus accountStatus;
}
