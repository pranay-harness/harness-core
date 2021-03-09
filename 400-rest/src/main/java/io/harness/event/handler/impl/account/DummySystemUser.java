package io.harness.event.handler.impl.account;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Value;

@OwnedBy(PL)
@Value
public class DummySystemUser {
  private String userName;
  private String email;
  private String id;

  public DummySystemUser(String accountId, String accountName) {
    this.id = systemUserId(accountId);
    this.email = this.id + "@harness.io";
    this.userName = "System User | " + accountName;
  }

  private static String systemUserId(String accountId) {
    return "system-" + accountId;
  }
}
