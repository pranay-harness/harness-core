/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.secret;

import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SecretSanitizerThreadLocal {
  public static final ThreadLocal<Set<String>> triggeredByThreadLocal = new ThreadLocal<>();

  /**
   *
   * @param secrets
   */
  public static void set(final Set<String> secrets) {
    triggeredByThreadLocal.set(secrets);
  }

  /**
   * Unset.
   */
  public static void unset() {
    triggeredByThreadLocal.remove();
  }

  /**
   *
   * @return
   */
  public static Set<String> get() {
    return triggeredByThreadLocal.get();
  }
}
