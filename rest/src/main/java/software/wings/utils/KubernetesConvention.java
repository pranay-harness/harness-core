package software.wings.utils;

/**
 * Created by brett on 3/8/17
 */
public class KubernetesConvention {
  private static final String DELIMITER = "__";

  public static String getTaskFamily(String appName, String replicationControllerName, String envName) {
    return Misc.normalizeExpression(appName + DELIMITER + replicationControllerName + DELIMITER + envName);
  }

  public static String getReplicationControllerName(String family, Integer revision) {
    return Misc.normalizeExpression(family + DELIMITER + revision);
  }

  public static String getReplicationControllerNamePrefix(String family) {
    return Misc.normalizeExpression(family + DELIMITER);
  }
}
