/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.experimental.UtilityClass;

@UtilityClass
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class AccountThreadLocal {
  public static final ThreadLocal<String> accountIdThreadLocal = new ThreadLocal<>();

  /**
   * Sets the.
   *
   * @param user the user
   */
  public static void set(String accountId) {
    accountIdThreadLocal.set(accountId);
  }

  /**
   * Unset.
   */
  public static void unset() {
    accountIdThreadLocal.remove();
  }

  /**
   * Gets the.
   *
   * @return the user
   */
  public static String get() {
    return accountIdThreadLocal.get();
  }
}
