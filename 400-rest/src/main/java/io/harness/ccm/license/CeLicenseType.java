/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
