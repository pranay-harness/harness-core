package io.harness.cdng.usage.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDP)
@UtilityClass
public class CDLicenseUsageConstants {
  public static final String DISPLAY_NAME = "Last 60 Days";
  public static final int TIME_PERIOD_IN_DAYS = 60;
}
