/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
