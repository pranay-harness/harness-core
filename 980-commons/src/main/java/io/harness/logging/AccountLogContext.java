/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.logging;

public class AccountLogContext extends AutoLogContext {
  public static final String ID = "accountId";

  public AccountLogContext(String accountId, OverrideBehavior behavior) {
    super(ID, accountId, behavior);
  }
}
