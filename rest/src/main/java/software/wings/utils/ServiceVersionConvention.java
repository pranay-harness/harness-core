package software.wings.utils;

import static org.apache.commons.lang3.StringUtils.trim;

public class ServiceVersionConvention {
  public static final String DELIMITER = "__";

  public static String getPrefix(String appName, String serviceName, String envName) {
    return Misc.normalizeExpression(appName + DELIMITER + serviceName + DELIMITER + envName);
  }

  public static String getPrefix(String appName, String envName) {
    return Misc.normalizeExpression(appName + DELIMITER + envName);
  }

  public static String getServiceName(String prefix, Integer revision) {
    return trim(prefix) + DELIMITER + revision;
  }

  public static String getServiceNamePrefixFromServiceName(String serviceName) {
    return serviceName.substring(0, serviceName.lastIndexOf(DELIMITER) + DELIMITER.length());
  }

  public static int getRevisionFromServiceName(String name) {
    if (name != null) {
      int index = name.lastIndexOf(DELIMITER);
      if (index >= 0) {
        try {
          return Integer.parseInt(name.substring(index + DELIMITER.length()));
        } catch (NumberFormatException e) {
          // Ignore
        }
      }
    }
    return -1;
  }
}
