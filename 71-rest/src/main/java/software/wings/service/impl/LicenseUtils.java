package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import lombok.experimental.UtilityClass;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;

import java.nio.charset.Charset;
import java.util.Calendar;

/**
 * Utility class that has object to string and string to object representations.
 * @author rktummala on 09/10/2018
 */
@UtilityClass
public class LicenseUtils {
  /**
   * Trial expires end of day - 14 days from the date of creation.
   */
  private static final int TRIAL_PERIOD = 14;

  private static final int PAID_PERIOD_IN_YEARS = 1;

  private static final int ESSENTIALS_PERIOD_IN_YEARS = 1;

  public static String convertToString(LicenseInfo licenseInfo) {
    StringBuilder builder = new StringBuilder();

    if (licenseInfo == null) {
      licenseInfo = new LicenseInfo();
    }

    String accountType = licenseInfo.getAccountType();
    if (isEmpty(accountType)) {
      builder.append("NONE");
    } else {
      builder.append(accountType);
    }
    builder.append('_');

    String accountStatus = licenseInfo.getAccountStatus();
    if (isEmpty(accountStatus)) {
      builder.append("NONE");
    } else {
      builder.append(accountStatus);
    }
    builder.append('_');

    long expiryTime = licenseInfo.getExpiryTime();
    builder.append(expiryTime).append('_');

    int licenseUnits = licenseInfo.getLicenseUnits();
    builder.append(licenseUnits);

    return builder.toString();
  }

  public static LicenseInfo convertToObject(byte[] decryptedBytes, boolean checkAndSetDefaultExpiry) {
    if (isEmpty(decryptedBytes)) {
      return null;
    }

    String licenseInfoStr = new String(decryptedBytes, Charset.forName("UTF-8"));

    String[] segments = licenseInfoStr.split("_");
    if (isEmpty(segments)) {
      return null;
    }

    boolean newFormat = segments.length == 4;
    boolean oldFormat = segments.length == 3;

    // The old format didn't have license units, some on-prem accounts might be using old license.
    if (!(oldFormat || newFormat)) {
      return null;
    }

    LicenseInfo licenseInfo = new LicenseInfo();
    String accountType = segments[0];
    String accountStatus = segments[1];
    String expiryTimeString = segments[2];

    if (AccountType.isValid(accountType)) {
      licenseInfo.setAccountType(accountType);
    }

    if (AccountStatus.isValid(accountStatus)) {
      licenseInfo.setAccountStatus(accountStatus);
    }

    long defaultExpiryTime = -1L;
    if (AccountType.TRIAL.equals(accountType)) {
      defaultExpiryTime = getDefaultTrialExpiryTime();
    } else if (AccountType.PAID.equals(accountType)) {
      defaultExpiryTime = getDefaultPaidExpiryTime();
    } else if (AccountType.ESSENTIALS.equals(accountType)) {
      defaultExpiryTime = getDefaultEssentialsExpiryTime();
    }

    long expiryTime;
    if ("DEFAULT".equals(expiryTimeString)) {
      expiryTime = defaultExpiryTime;
    } else {
      try {
        expiryTime = Long.parseLong(expiryTimeString);
        if (checkAndSetDefaultExpiry) {
          if (expiryTime < System.currentTimeMillis()) {
            expiryTime = defaultExpiryTime;
          }
        }
      } catch (NumberFormatException ex) {
        expiryTime = defaultExpiryTime;
      }
    }

    licenseInfo.setExpiryTime(expiryTime);

    int licenseUnits = 0;
    if (newFormat) {
      String licenseUnitsString = segments[3];
      try {
        licenseUnits = Integer.parseInt(licenseUnitsString);
      } catch (NumberFormatException ex) {
        licenseUnits = 0;
      }
    }

    licenseInfo.setLicenseUnits(licenseUnits);

    return licenseInfo;
  }

  public static long getDefaultTrialExpiryTime() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, TRIAL_PERIOD);
    calendar.set(Calendar.HOUR, 11);
    calendar.set(Calendar.MINUTE, 59);
    calendar.set(Calendar.SECOND, 59);
    calendar.set(Calendar.MILLISECOND, 0);
    calendar.set(Calendar.AM_PM, Calendar.PM);
    return calendar.getTimeInMillis();
  }

  public static long getDefaultPaidExpiryTime() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.YEAR, PAID_PERIOD_IN_YEARS);
    calendar.set(Calendar.HOUR, 11);
    calendar.set(Calendar.MINUTE, 59);
    calendar.set(Calendar.SECOND, 59);
    calendar.set(Calendar.MILLISECOND, 0);
    calendar.set(Calendar.AM_PM, Calendar.PM);
    return calendar.getTimeInMillis();
  }

  public static long getDefaultEssentialsExpiryTime() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.YEAR, ESSENTIALS_PERIOD_IN_YEARS);
    calendar.set(Calendar.HOUR, 11);
    calendar.set(Calendar.MINUTE, 59);
    calendar.set(Calendar.SECOND, 59);
    calendar.set(Calendar.MILLISECOND, 0);
    calendar.set(Calendar.AM_PM, Calendar.PM);
    return calendar.getTimeInMillis();
  }
}
