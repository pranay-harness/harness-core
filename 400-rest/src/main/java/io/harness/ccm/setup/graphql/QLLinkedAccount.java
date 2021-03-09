package io.harness.ccm.setup.graphql;

import software.wings.beans.ce.CECloudAccount.AccountStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLLinkedAccount {
  private String id;
  private String name;
  private String arn;
  private String masterAccountId;
  private AccountStatus accountStatus;
}
