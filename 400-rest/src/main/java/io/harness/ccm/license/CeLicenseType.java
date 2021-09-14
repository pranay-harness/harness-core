/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.license;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import lombok.Getter;

@Getter
@OwnedBy(CE)
public enum CeLicenseType {
  FULL_TRIAL(30, ChronoUnit.DAYS),
  LIMITED_TRIAL(Constants.CE_TRIAL_PERIOD_DAYS, ChronoUnit.DAYS),
  PAID(1, ChronoUnit.YEARS);

  private final int defaultPeriod;
  private final ChronoUnit defaultPeriodUnit;

  public static class Constants {
    private Constants() {}
    public static final int CE_TRIAL_PERIOD_DAYS = 15;
  }

  CeLicenseType(int period, ChronoUnit periodUnit) {
    this.defaultPeriod = period;
    this.defaultPeriodUnit = periodUnit;
  }

  public long getDefaultExpiryTime() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, defaultPeriod);
    calendar.set(Calendar.HOUR, calendar.getMaximum(Calendar.HOUR));
    calendar.set(Calendar.MINUTE, calendar.getMaximum(Calendar.MINUTE));
    calendar.set(Calendar.SECOND, calendar.getMaximum(Calendar.SECOND));
    calendar.set(Calendar.MILLISECOND, calendar.getMaximum(Calendar.MILLISECOND));

    return calendar.getTimeInMillis();
  }

  public static long getEndOfYearAsMillis(int year) {
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.YEAR, year);
    calendar.set(Calendar.MONTH, calendar.getMaximum(Calendar.MONTH));
    calendar.set(Calendar.DATE, calendar.getMaximum(Calendar.DATE));
    calendar.set(Calendar.HOUR, calendar.getMaximum(Calendar.HOUR));
    calendar.set(Calendar.MINUTE, calendar.getMaximum(Calendar.MINUTE));
    calendar.set(Calendar.SECOND, calendar.getMaximum(Calendar.SECOND));
    calendar.set(Calendar.MILLISECOND, calendar.getMaximum(Calendar.MILLISECOND));

    return calendar.getTimeInMillis();
  }
}
