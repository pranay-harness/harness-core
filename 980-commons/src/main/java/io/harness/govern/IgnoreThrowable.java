/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.govern;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class IgnoreThrowable {
  public static void ignoredOnPurpose(Throwable exception) {
    // We would like to express explicitly that we are ignoring this exception
  }
}
