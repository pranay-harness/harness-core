package software.wings.utils;

/**
 * Created by rishi on 2/7/17.
 */
public class EcsConvention {
  private static final String DELIMITER = "__";

  public static String getTaskFamily(String appName, String serviceName, String envName) {
    return Misc.normalizeExpression(appName + DELIMITER + serviceName + DELIMITER + envName);
  }

  public static String getServiceName(String family, Integer revision) {
    return Misc.normalizeExpression(family + DELIMITER + revision);
  }

  public static String getServiceNamePrefix(String family) {
    return Misc.normalizeExpression(family + DELIMITER);
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
