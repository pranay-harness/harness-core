package software.wings.utils;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
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
  public static final int HELM_RELEASE_VERSION_LENGTH = 15;
  private static Logger logger = LoggerFactory.getLogger(KubernetesConvention.class);

  public static final String DOT = ".";
  public static final String DASH = "-";
  private static final String VOLUME_PREFIX = "vol-";
  private static final String VOLUME_SUFFIX = "-vol";
  private static final String SECRET_PREFIX = "hs-";
  private static final String SECRET_SUFFIX = "-hs";
  private static final String CONTAINER_PREFIX = "hs-";
  private static final String CONTAINER_SUFFIX = "-hs";
  private static Pattern wildCharPattern = Pattern.compile("[_+*/\\\\ &@$|\"':]");

  // TODO(brett) Stateful Sets are no longer versioned. Remove statefulSet param after 6/1/18
  public static String getControllerName(String prefix, int revision, boolean isStatefulSet) {
    return normalize(prefix) + (isStatefulSet ? DASH : DOT) + revision;
  }

  // TODO(brett) Stateful Sets are no longer versioned. Remove statefulSet param after 6/1/18
  public static String getControllerNamePrefix(
      String appName, String serviceName, String envName, boolean isStatefulSet) {
    String separator = isStatefulSet ? DASH : DOT;
    return normalize(appName + separator + serviceName + separator + envName);
  }

  // TODO(brett) Stateful Sets are no longer versioned. Remove statefulSet param after 6/1/18
  public static String getPrefixFromControllerName(String controllerName, boolean isStatefulSet) {
    String versionSeparator = isStatefulSet ? DASH : DOT;
    return controllerName.substring(0, controllerName.lastIndexOf(versionSeparator) + versionSeparator.length());
  }

  // TODO(brett) Stateful Sets are no longer versioned. Remove statefulSet param after 6/1/18
  public static String getServiceNameFromControllerName(String controllerName, boolean isStatefulSet) {
    String versionSeparator = isStatefulSet ? DASH : DOT;
    return noDot(controllerName.substring(0, controllerName.lastIndexOf(versionSeparator)));
  }

  public static String getKubernetesServiceName(String controllerNamePrefix) {
    return noDot(normalize(controllerNamePrefix));
  }

  public static String getPrimaryServiceName(String baseServiceName) {
    return baseServiceName + "-primary";
  }

  public static String getStageServiceName(String baseServiceName) {
    return baseServiceName + "-stage";
  }

  public static String getBlueGreenIngressName(String baseServiceName) {
    return baseServiceName + ".bluegreen";
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

  // TODO(brett) Stateful Sets are no longer versioned. Remove statefulSet param after 6/1/18
  public static Optional<Integer> getRevisionFromControllerName(String name, boolean isStatefulSet) {
    String versionSeparator = isStatefulSet ? DASH : DOT;
    if (name != null) {
      int index = name.lastIndexOf(versionSeparator);
      if (index >= 0) {
        try {
          String version = name.substring(index + versionSeparator.length());
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

  public static String getHelmReleaseName(String appName, String serviceName, String envName, String infraMappingId) {
    String controllerNamePrefix = normalize(appName + DASH + serviceName + DASH + envName);
    String infraMappingIdPrefix = infraMappingId.substring(0, 7).toLowerCase().replace('-', 'z').replace('_', 'z');
    String revision = "harness" + DASH + infraMappingIdPrefix;
    return normalize(controllerNamePrefix) + DASH + revision;
  }

  public static String getHelmHarnessReleaseRevision(String releaseName) {
    return isNotEmpty(releaseName) && releaseName.length() > HELM_RELEASE_VERSION_LENGTH
        ? releaseName.substring(releaseName.length() - 15)
        : null;
  }

  public static String getAccountIdentifier(String accountId) {
    StringBuilder identifier = new StringBuilder();
    for (char c : accountId.toLowerCase().toCharArray()) {
      if (identifier.length() < 6 && c >= 'a' && c <= 'z') {
        identifier.append(c);
      }
    }
    return identifier.toString();
  }
}
