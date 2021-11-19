package io.harness.cdng.usage.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDP)
@UtilityClass
public class CDLicenseUsageConstants {
  public static final String DISPLAY_NAME = "Last 30 Days";
  public static final int TIME_PERIOD_IN_DAYS = 30;
  public static final double PERCENTILE = 0.95;
  public static final int LICENSE_INSTANCE_LIMIT = 20;
}
