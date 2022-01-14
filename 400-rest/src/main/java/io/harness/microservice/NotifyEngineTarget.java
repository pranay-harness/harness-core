package io.harness.microservice;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CE)
@TargetModule(HarnessModule._960_PERSISTENCE)
@UtilityClass
public class NotifyEngineTarget {
  public static final String GENERAL = "general";
}
