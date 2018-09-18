package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import software.wings.beans.LicenseInfo;

import java.nio.charset.Charset;

/**
 * Utility class that has object to string and string to object representations.
 * @author rktummala on 09/10/2018
 */
public class LicenseUtil {
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
    builder.append(expiryTime);
    return builder.toString();
  }

  public static LicenseInfo convertToObject(
      byte[] decryptedBytes, long defaultExpiryTime, boolean checkAndSetDefaultExpiry) {
    if (isEmpty(decryptedBytes)) {
      return null;
    }

    String licenseInfoStr = new String(decryptedBytes, Charset.forName("UTF-8"));

    String[] segments = licenseInfoStr.split("_");
    if (isEmpty(segments)) {
      return null;
    }

    if (segments.length != 3) {
      return null;
    }

    LicenseInfo licenseInfo = new LicenseInfo();
    String accountType = segments[0];
    String accountStatus = segments[1];
    String expiryTimeString = segments[2];

    if (!accountType.equals("null")) {
      licenseInfo.setAccountType(accountType);
    }

    if (!accountStatus.equals("null")) {
      licenseInfo.setAccountStatus(accountStatus);
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

    return licenseInfo;
  }
}
