package io.harness.azure.utility;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import lombok.experimental.UtilityClass;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.SimpleTimeZone;

@UtilityClass
public class AzureResourceUtility {
  private final String DELIMITER = "__";
  private final String ISO_8601_BASIC_FORMAT = "yyyyMMdd'T'HHmmss'Z'";
  private final List<String> AUTO_SCALE_DEFAULT_PROFILE_NAMES =
      Arrays.asList("Profile1", "Auto created scale condition");

  public String dateToISO8601BasicStr(Date date) {
    SimpleDateFormat dateTimeFormat = new SimpleDateFormat(ISO_8601_BASIC_FORMAT);
    dateTimeFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
    return dateTimeFormat.format(date);
  }

  public Date iso8601BasicStrToDate(String strDate) {
    DateFormat format = new SimpleDateFormat(ISO_8601_BASIC_FORMAT);
    try {
      return format.parse(strDate);
    } catch (ParseException e) {
      throw new IllegalArgumentException(format("Unable to parse date: %s", strDate), e);
    }
  }

  public String getVMSSName(String scaleSetNamePrefix, Integer revision) {
    return scaleSetNamePrefix + DELIMITER + revision;
  }

  public String getRevisionTagValue(String tagValuePrefix, Integer revision) {
    return tagValuePrefix + DELIMITER + revision;
  }

  public int getRevisionFromTag(String tagValue) {
    if (tagValue != null) {
      int index = tagValue.lastIndexOf(DELIMITER);
      if (index >= 0) {
        try {
          return Integer.parseInt(tagValue.substring(index + DELIMITER.length()));
        } catch (NumberFormatException e) {
          // Ignore
        }
      }
    }
    return 0;
  }

  public boolean isDefaultAutoScaleProfile(String profileName) {
    return isNotBlank(profileName) && AUTO_SCALE_DEFAULT_PROFILE_NAMES.contains(profileName);
  }
}
