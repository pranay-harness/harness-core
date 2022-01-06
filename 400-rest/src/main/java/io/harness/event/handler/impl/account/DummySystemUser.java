/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
