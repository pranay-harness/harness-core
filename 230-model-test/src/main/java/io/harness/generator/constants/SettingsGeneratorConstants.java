package io.harness.generator.constants;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CDC)
public class SettingsGeneratorConstants {
  // PCF Settings Constants
  public static final String PCF_KEY = "pcf_cp_password";
  public static final String PCF_USERNAME = "admin";
  public static final String PCF_END_POINT = "api.system.pcf-harness.com";
}
