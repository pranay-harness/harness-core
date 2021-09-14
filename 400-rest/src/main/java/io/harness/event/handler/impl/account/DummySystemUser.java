/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
