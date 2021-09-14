/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
