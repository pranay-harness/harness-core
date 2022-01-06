/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
