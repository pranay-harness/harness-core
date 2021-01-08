package io.harness.ngpipeline.common;

import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.pms.contracts.ambiance.Ambiance;

import lombok.experimental.UtilityClass;

@UtilityClass
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

  public static String getExpressionFunctorToken(Ambiance ambiance) {
    return ambiance.getSetupAbstractionsMap().get("expressionFunctorToken");
  }

  public NGAccess getNgAccess(Ambiance ambiance) {
    return BaseNGAccess.builder()
        .accountIdentifier(getAccountId(ambiance))
        .orgIdentifier(getOrgIdentifier(ambiance))
        .projectIdentifier(getProjectIdentifier(ambiance))
        .build();
  }

  public String getEventPayload(Ambiance ambiance) {
    return ambiance.getMetadata().getTriggerPayload().getJsonPayload();
  }
}
