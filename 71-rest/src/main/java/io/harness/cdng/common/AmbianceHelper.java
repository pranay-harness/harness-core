package io.harness.cdng.common;

import io.harness.ambiance.Ambiance;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AmbianceHelper {
  public static String getAccountId(Ambiance ambiance) {
    return ambiance.getSetupAbstractions().get("accountId");
  }
}
