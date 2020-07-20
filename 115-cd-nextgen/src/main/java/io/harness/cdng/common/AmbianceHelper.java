package io.harness.cdng.common;

import io.harness.ambiance.Ambiance;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AmbianceHelper {
  public String getAccountId(Ambiance ambiance) {
    return ambiance.getSetupAbstractions().get("accountId");
  }

  public String getProjectIdentifier(Ambiance ambiance) {
    return ambiance.getSetupAbstractions().get("projectIdentifier");
  }

  public String getOrgIdentifier(Ambiance ambiance) {
    return ambiance.getSetupAbstractions().get("orgIdentifier");
  }
}
