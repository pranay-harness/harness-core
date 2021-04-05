package io.harness.pms.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class PmsConstants {
  public final String INTERNAL_SERVICE_NAME = "pmsInternal";
  public final String QUEUING_RC_NAME = "Queuing";
}
