package software.wings.utils;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.container.ImageDetails;

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
  private static final String SECRET_PREFIX = "hs-";
  private static final String SECRET_SUFFIX = "-hs";
  private static final String CONTAINER_PREFIX = "hs-";
  private static final String CONTAINER_SUFFIX = "-hs";
  private static Pattern wildCharPattern = Pattern.compile("[_+*/\\\\ &$|\"':]");

  public static String getControllerName(String prefix, int revision) {
    return normalize(prefix) + DOT + revision;
  }

  public static String getControllerNamePrefix(String appName, String serviceName, String envName) {
    return normalize(appName + DOT + serviceName + DOT + envName);
  }

  public static String getPrefixFromControllerName(String controllerName) {
    return controllerName.substring(0, controllerName.lastIndexOf(DOT) + DOT.length());
  }

  public static String getKubernetesServiceName(String rcNamePrefix) {
    return noDot(normalize(rcNamePrefix));
  }

  public static String getKubernetesRegistrySecretName(ImageDetails imageDetails) {
    String regName = imageDetails.getRegistryUrl().substring(imageDetails.getRegistryUrl().indexOf("://") + 3);
    if (regName.endsWith("/")) {
      regName = regName.substring(0, regName.length() - 1);
    }
    String name =
        normalize(noDot(regName + (isNotBlank(imageDetails.getUsername()) ? "-" + imageDetails.getUsername() : "")));
    int maxLength = 63 - (SECRET_PREFIX.length() + SECRET_SUFFIX.length());
    if (name.length() > maxLength) {
      name = name.substring(0, maxLength);
    }
    return SECRET_PREFIX + name + SECRET_SUFFIX;
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
    String name = normalize(noDot(imageName));
    int maxLength = 63 - (CONTAINER_PREFIX.length() + CONTAINER_SUFFIX.length());
    if (name.length() > maxLength) {
      name = name.substring(0, maxLength);
    }
    return CONTAINER_PREFIX + name + CONTAINER_SUFFIX;
  }

  public static String getVolumeName(String path) {
    return VOLUME_PREFIX + normalize(path) + VOLUME_SUFFIX;
  }

  public static String getLabelValue(String value) {
    return normalize(value);
  }

  public static String normalize(String expression) {
    return wildCharPattern.matcher(trim(expression)).replaceAll(DASH).toLowerCase();
  }

  private static String noDot(String str) {
    return str != null ? str.replaceAll("\\.", DASH) : "null";
  }
}
