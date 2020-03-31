package io.harness.ccm.setup.dao;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLLinkedAccount {
  private String id;
  private String name;
  private String arn;
  private String masterAccountId;
}
