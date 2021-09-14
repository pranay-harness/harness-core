/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.rule;

import io.harness.beans.EmbeddedUser;
import io.harness.persistence.UserProvider;

import lombok.Setter;

public class TestUserProvider implements UserProvider {
  public static final TestUserProvider testUserProvider = new TestUserProvider();

  @Setter private EmbeddedUser activeUser;

  @Override
  public EmbeddedUser activeUser() {
    return activeUser;
  }
}
