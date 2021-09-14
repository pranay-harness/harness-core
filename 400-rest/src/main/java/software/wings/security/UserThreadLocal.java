/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.User;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class UserThreadLocal {
  public static class Guard implements AutoCloseable {
    private User old;
    Guard(User user) {
      old = get();
      set(user);
    }

    @Override
    public void close() {
      set(old);
    }
  }

  public static Guard userGuard(User user) {
    return new Guard(user);
  }

  /**
   * The constant userThreadLocal.
   */
  public static final ThreadLocal<User> userThreadLocal = new ThreadLocal<>();

  /**
   * Sets the.
   *
   * @param user the user
   */
  public static void set(User user) {
    userThreadLocal.set(user);
  }

  /**
   * Unset.
   */
  public static void unset() {
    userThreadLocal.remove();
  }

  /**
   * Gets the.
   *
   * @return the user
   */
  public static User get() {
    return userThreadLocal.get();
  }
}
