package io.harness.azure.utility;

import static io.harness.azure.model.AzureConstants.DOCKER_CUSTOM_IMAGE_NAME_PROPERTY_NAME;
import static io.harness.azure.model.AzureConstants.DOCKER_IMAGE_FULL_PATH_PATTERN;
import static io.harness.azure.model.AzureConstants.DOCKER_IMAGE_AND_TAG_PATH_PATTERN;
import static io.harness.azure.model.AzureConstants.DOCKER_REGISTRY_SERVER_SECRET_PROPERTY_NAME;
import static io.harness.azure.model.AzureConstants.DOCKER_REGISTRY_SERVER_URL_PROPERTY_NAME;
import static io.harness.azure.model.AzureConstants.DOCKER_REGISTRY_SERVER_USERNAME_PROPERTY_NAME;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.microsoft.azure.CloudException;
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
  public final List<String> DOCKER_REGISTRY_PROPERTY_NAMES =
      Arrays.asList(DOCKER_REGISTRY_SERVER_URL_PROPERTY_NAME, DOCKER_REGISTRY_SERVER_USERNAME_PROPERTY_NAME,
          DOCKER_REGISTRY_SERVER_SECRET_PROPERTY_NAME, DOCKER_CUSTOM_IMAGE_NAME_PROPERTY_NAME);

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

  public String getDockerImageAndTagFullPath(String imageAndTag) {
    return String.format(DOCKER_IMAGE_FULL_PATH_PATTERN, imageAndTag);
  }

  public String getDockerImageAndTagPath(String imageName, String imageTag) {
    return String.format(DOCKER_IMAGE_AND_TAG_PATH_PATTERN, imageName, imageTag);
  }

  public String getAzureCloudExceptionMessage(Exception ex) {
    String message = ex.getMessage();
    if (ex.getCause() instanceof CloudException) {
      CloudException cloudException = (CloudException) ex.getCause();
      String cloudExMsg = cloudException.getMessage();
      message = format("%s, %nAzure Cloud Exception Message: %s", message, cloudExMsg);
    }
    return message;
  }
}
