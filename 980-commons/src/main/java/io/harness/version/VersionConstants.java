package io.harness.version;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class VersionConstants {
  public static final String VERSION_KEY = "versionKey";
  public static final String MAJOR_VERSION_KEY = "majorVersionKey";
}
