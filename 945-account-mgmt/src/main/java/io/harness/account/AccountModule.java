/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.account;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;

@OwnedBy(HarnessTeam.GTM)
public class AccountModule extends AbstractModule {
  private static AccountModule instance;

  private AccountModule() {}

  static AccountModule getInstance() {
    if (instance == null) {
      instance = new AccountModule();
    }
    return instance;
  }
}
