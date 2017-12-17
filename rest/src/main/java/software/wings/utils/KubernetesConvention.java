package software.wings.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Created by brett on 3/8/17
 */
public class KubernetesConvention {
  private static Logger logger = LoggerFactory.getLogger(KubernetesConvention.class);

  public static final String DOT = ".";
  private static final String DASH = "-";
  private static final String VOLUME_PREFIX = "vol-";
  private static final String VOLUME_SUFFIX = "-vol";
  private static Pattern wildCharPattern = Pattern.compile("[_+*/\\\\ &$|\"':]");

  public static String getReplicationControllerName(String prefix, int revision) {
    return normalize(prefix) + DOT + revision;
  }

  public static String getReplicationControllerNamePrefix(String appName, String serviceName, String envName) {
    return normalize(appName + DOT + serviceName + DOT + envName);
  }

  public static String getPrefixFromControllerName(String controllerName) {
    return controllerName.substring(0, controllerName.lastIndexOf(DOT) + DOT.length());
  }

  public static String getKubernetesServiceName(String rcNamePrefix) {
    return noDot(normalize(rcNamePrefix));
  }

  public static String getKubernetesSecretName(String serviceName, String imageSource) {
    String name = normalize(serviceName + DASH + noDot(imageSource));
    return name.substring(0, Math.min(name.length(), 63));
  }

  public static Optional<Integer> getRevisionFromControllerName(String name) {
    if (name != null) {
      int index = name.lastIndexOf(DOT);
      if (index >= 0) {
        try {
          String version = name.substring(index + DOT.length());
          return version.contains(DASH) ? Optional.empty() : Optional.of(Integer.parseInt(version));
        } catch (NumberFormatException e) {
          logger.error("Couldn't get version from controller name {}", name, e);
        }
      }
    }
    return Optional.empty();
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
