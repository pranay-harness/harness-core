package software.wings.utils;

/**
 * Created by brett on 3/8/17
 */
public class KubernetesConvention {
  private static final String DELIMITER = "__";

  public static String getReplicationControllerName(String appName, String serviceName, String envName, int revision) {
    return Misc.normalizeExpression(appName + DELIMITER + serviceName + DELIMITER + envName + DELIMITER + revision);
  }

  public static String getReplicationControllerNamePrefix(String appName, String serviceName, String envName) {
    return Misc.normalizeExpression(appName + DELIMITER + serviceName + DELIMITER + envName + DELIMITER);
  }

  public static int getRevisionFromControllerName(String name) {
    int index = name.lastIndexOf(DELIMITER);
    if (index >= 0) {
      try {
        return Integer.parseInt(name.substring(index + DELIMITER.length()));
      } catch (NumberFormatException e) {
        // Ignore
      }
    }
    return -1;
  }
}
