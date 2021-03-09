package io.harness.ccm.setup.graphql;

import lombok.Builder;
import lombok.Value;
import software.wings.beans.ce.CECloudAccount.AccountStatus;

@Value
@Builder
public class QLLinkedAccount {
  private String id;
  private String name;
  private String arn;
  private String masterAccountId;
  private AccountStatus accountStatus;
}
