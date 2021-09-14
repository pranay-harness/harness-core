/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.licensing.utils;

import static io.harness.licensing.LicenseConstant.UNLIMITED;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ModuleLicenseUtils {
  public int computeAdd(int total, int add) {
    if (total == UNLIMITED || add == UNLIMITED) {
      return UNLIMITED;
    } else {
      return total + add;
    }
  }

  public long computeAdd(long total, long add) {
    if (total == UNLIMITED || add == UNLIMITED) {
      return UNLIMITED;
    } else {
      return total + add;
    }
  }
}
