/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.persistence.UserProvider;

import software.wings.beans.User;

@OwnedBy(PL)
public class ThreadLocalUserProvider implements UserProvider {
  public static EmbeddedUser populateEmbeddedUser(User user) {
    return EmbeddedUser.builder()
        .uuid(UserThreadLocal.get().getUuid())
        .email(UserThreadLocal.get().getEmail())
        .name(UserThreadLocal.get().getName())
        .build();
  }

  public static EmbeddedUser threadLocalUser() {
    User user = UserThreadLocal.get();
    if (user == null) {
      return null;
    }

    return populateEmbeddedUser(UserThreadLocal.get());
  }

  @Override
  public EmbeddedUser activeUser() {
    return threadLocalUser();
  }
}
