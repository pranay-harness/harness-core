package io.harness.cdng.common;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MiscUtils {
  // TODO @rk: 12/07/20 : remove when cdng runs inside ng manager
  public static boolean isNextGenApplication() {
    try {
      Class.forName("io.harness.ng.NextGenApplication");
    } catch (ClassNotFoundException e) {
      return false;
    }
    return true;
  }
}
