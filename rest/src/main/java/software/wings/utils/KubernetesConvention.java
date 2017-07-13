package software.wings.utils;

import java.util.regex.Pattern;

/**
 * Created by brett on 3/8/17
 */
public class KubernetesConvention {
  private static final String VOLUME_PREFIX = "vol-";
  private static final String VOLUME_SUFFIX = "-vol";
  private static final String DOT = ".";
  private static final String DASH = "-";
  private static Pattern wildCharPattern = Pattern.compile("[_+*/\\\\ &$|\"']");

  public static String getReplicationControllerName(String appName, String serviceName, String envName, int revision) {
    return getReplicationControllerNamePrefix(appName, serviceName, envName) + revision;
  }

  public static String getReplicationControllerNamePrefix(String appName, String serviceName, String envName) {
    return normalize(appName + DOT + serviceName + DOT + envName + DOT);
  }

  public static String getReplicationControllerNamePrefixFromControllerName(String controllerName) {
    return controllerName.substring(0, controllerName.lastIndexOf(DOT) + DOT.length());
  }

  public static String getKubernetesServiceName(String appName, String serviceName, String envName) {
    return normalize(appName + DASH + serviceName + DASH + envName);
  }

  public static String getKubernetesSecretName(String appName, String serviceName, String envName, String imageSource) {
    return normalize(appName + DASH + serviceName + DASH + envName + DASH + imageSource);
  }

  public static int getRevisionFromControllerName(String name) {
    if (name != null) {
      int index = name.lastIndexOf(DOT);
      if (index >= 0) {
        try {
          return Integer.parseInt(name.substring(index + DOT.length()));
        } catch (NumberFormatException e) {
          // Ignore
        }
      }
    }
    return -1;
  }

  public static String getContainerName(String imageName) {
    return normalize(imageName);
  }

  public static String getVolumeName(String path) {
    return VOLUME_PREFIX + normalize(path) + VOLUME_SUFFIX;
  }

  public static String getLabelValue(String value) {
    return normalize(value);
  }

  private static String normalize(String expression) {
    return wildCharPattern.matcher(expression).replaceAll(DASH).toLowerCase();
  }
}
