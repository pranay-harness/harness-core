package io.harness.pms.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class PmsConstants {
  public final String QUEUING_RC_NAME = "Queuing";
  public final String RELEASE_ENTITY_TYPE_PLAN = "PLAN";
}
