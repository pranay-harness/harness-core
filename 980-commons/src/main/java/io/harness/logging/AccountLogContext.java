/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logging;

public class AccountLogContext extends AutoLogContext {
  public static final String ID = "accountId";

  public AccountLogContext(String accountId, OverrideBehavior behavior) {
    super(ID, accountId, behavior);
  }
}
