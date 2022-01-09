/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.utils;

import io.harness.logging.Misc;

import com.openpojo.validation.test.Tester;

/**
 * Created by peeyushaggarwal on 5/18/16.
 */
public abstract class BaseTester implements Tester {
  /**
   * Overrides method.
   *
   * @param cls    the cls
   * @param method the method
   * @return true, if successful
   */
  public static boolean overridesMethod(Class<?> cls, String method) {
    return Misc.ignoreException(() -> cls.getMethod("toString").getDeclaringClass() == cls, false);
  }
}
