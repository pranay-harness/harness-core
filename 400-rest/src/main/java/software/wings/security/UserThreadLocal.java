/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
