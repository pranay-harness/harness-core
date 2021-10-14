package io.harness.ngpipeline.common;

import io.harness.pms.contracts.ambiance.Ambiance;

import lombok.experimental.UtilityClass;

/*
 DEPRECATED : Please use Ambiance Utils and move this logic there
 */
@UtilityClass
@Deprecated
public class AmbianceHelper {
  public String getAccountId(Ambiance ambiance) {
    return ambiance.getSetupAbstractionsMap().get("accountId");
  }

  public String getProjectIdentifier(Ambiance ambiance) {
    return ambiance.getSetupAbstractionsMap().get("projectIdentifier");
  }

  public String getOrgIdentifier(Ambiance ambiance) {
    return ambiance.getSetupAbstractionsMap().get("orgIdentifier");
  }
}
