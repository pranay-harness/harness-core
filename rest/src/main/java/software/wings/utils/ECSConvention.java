package software.wings.utils;

/**
 * Created by rishi on 2/7/17.
 */
public class ECSConvention {
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
}
