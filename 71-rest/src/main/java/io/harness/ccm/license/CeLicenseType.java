package io.harness.ccm.license;

import lombok.Getter;

import java.time.temporal.ChronoUnit;
import java.util.Calendar;

@Getter
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
    calendar.set(Calendar.HOUR, 11);
    calendar.set(Calendar.MINUTE, 59);
    calendar.set(Calendar.SECOND, 59);
    calendar.set(Calendar.MILLISECOND, 0);
    calendar.set(Calendar.AM_PM, Calendar.PM);
    return calendar.getTimeInMillis();
  }
}
