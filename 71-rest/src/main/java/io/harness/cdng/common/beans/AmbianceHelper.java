package io.harness.cdng.common.beans;

import io.harness.ambiance.Ambiance;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AmbianceHelper {
  public static String getAccountId(Ambiance ambiance) {
    return (String) ambiance.getInputArgs().get("accountId");
  }
}
