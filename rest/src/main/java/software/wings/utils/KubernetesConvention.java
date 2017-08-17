package software.wings.utils;

import java.util.regex.Pattern;

/**
 * Created by brett on 3/8/17
 */
public class KubernetesConvention {
  private static final String VOLUME_PREFIX = "vol-";
  private static final String VOLUME_SUFFIX = "-vol";
  public static final String DOT = ".";
  public static final String DASH = "-";
  private static Pattern wildCharPattern = Pattern.compile("[_+*/\\\\ &$|\"']");

  public static String getReplicationControllerName(String prefix, int revision) {
    return normalize(prefix) + DOT + revision;
  }

  public static String getReplicationControllerNamePrefix(String appName, String serviceName, String envName) {
    return normalize(appName + DOT + serviceName + DOT + envName);
  }

  public static String getReplicationControllerNamePrefixFromControllerName(String controllerName) {
    return controllerName.substring(0, controllerName.lastIndexOf(DOT) + DOT.length());
  }

  public static String getKubernetesServiceName(String rcNamePrefix) {
    return noDot(normalize(rcNamePrefix));
  }

  public static String getKubernetesSecretName(String serviceName, String imageSource) {
    return normalize(serviceName + DASH + noDot(imageSource));
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
    return normalize(noDot(imageName));
  }

  public static String getVolumeName(String path) {
    return VOLUME_PREFIX + normalize(path) + VOLUME_SUFFIX;
  }

  public static String getLabelValue(String value) {
    return normalize(value);
  }

  public static String normalize(String expression) {
    return wildCharPattern.matcher(expression).replaceAll(DASH).toLowerCase();
  }

  private static String noDot(String str) {
    return str != null ? str.replaceAll("\\.", DASH) : "null";
  }
}
