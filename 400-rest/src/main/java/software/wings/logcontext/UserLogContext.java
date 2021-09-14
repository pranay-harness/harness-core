/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.logcontext;

import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.LogKeyUtils;

import software.wings.beans.User;

import com.google.common.collect.ImmutableMap;

public class UserLogContext extends AutoLogContext {
  public static final String ID = LogKeyUtils.calculateLogKeyForId(User.class);

  public UserLogContext(String accountId, String userId, OverrideBehavior behavior) {
    super(ImmutableMap.of(AccountLogContext.ID, accountId, ID, userId), behavior);
  }
}
