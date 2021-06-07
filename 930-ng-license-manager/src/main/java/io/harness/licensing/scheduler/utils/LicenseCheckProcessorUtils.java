package io.harness.licensing.scheduler.utils;

import static io.harness.licensing.LicenseConstant.UNLIMITED;

import lombok.experimental.UtilityClass;

@UtilityClass
public class LicenseCheckProcessorUtils {
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
