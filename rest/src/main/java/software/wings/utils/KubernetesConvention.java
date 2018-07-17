package software.wings.utils;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
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
  public static final String DASH = "-";

  private static final String VOLUME_PREFIX = "vol-";
  private static final String VOLUME_SUFFIX = "-vol";
  private static final String SECRET_PREFIX = "hs-";
  private static final String SECRET_SUFFIX = "-hs";
  private static final String CONTAINER_PREFIX = "hs-";
  private static final String CONTAINER_SUFFIX = "-hs";
  private static final int HELM_RELEASE_VERSION_LENGTH = 15;
  private static final int MAX_REVISIONS = 100000;
  private static Pattern wildCharPattern = Pattern.compile("[_+*/\\\\ &@$|\"':]");

  public static String getControllerName(String prefix, int revision, boolean useDashInHostname) {
    String separator = useDashInHostname ? DASH : DOT;
    return normalize(prefix) + separator + revision;
  }

  public static String getControllerNamePrefix(
      String appName, String serviceName, String envName, boolean useDashInHostname) {
    String separator = useDashInHostname ? DASH : DOT;
    return normalize(appName + separator + serviceName + separator + envName);
  }

  public static String getPrefixFromControllerName(String controllerName, boolean useDashInHostname) {
    String separator = useDashInHostname ? DASH : DOT;
    int index = controllerName.lastIndexOf(separator);
    if (index > 0) {
      try {
        Integer.parseInt(controllerName.substring(index + separator.length()));
        return controllerName.substring(0, index);
      } catch (NumberFormatException e) {
        // Not versioned
      }
    }
    return controllerName;
  }

  public static String getServiceNameFromControllerName(String controllerName, boolean useDashInHostname) {
    String separator = useDashInHostname ? DASH : DOT;
    int index = controllerName.lastIndexOf(separator);
    if (index > 0) {
      try {
        Integer.parseInt(controllerName.substring(index + separator.length()));
        return noDot(controllerName.substring(0, index));
      } catch (NumberFormatException e) {
        // Not versioned
      }
    }
    return noDot(controllerName);
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

  public static Optional<Integer> getRevisionFromControllerName(String name, boolean useDashInHostname) {
    if (useDashInHostname) {
      return getRevisionFromControllerName(name);
    }
    String versionSeparator = DOT;
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

  private static Optional<Integer> getRevisionFromControllerName(String name) {
    String versionSeparator = DASH;
    if (name != null) {
      int index = name.lastIndexOf(versionSeparator);
      if (index >= 0) {
        String version = "";
        try {
          version = name.substring(index + versionSeparator.length());
          Optional<Integer> revision =
              version.contains(DASH) ? Optional.empty() : Optional.of(Integer.parseInt(version));

          // ToDo(anshul) needs better logic here rather than using MAX_REVISIONS
          return (revision.isPresent() && revision.get() < MAX_REVISIONS) ? revision : Optional.empty();
        } catch (NumberFormatException e) {
          logger.warn("Couldn't get version from controller name {}. {} is not a valid version", name, version, e);
        }
      }
    }

    return getRevisionFromControllerNameWithDot(name);
  }

  // ToDo(anshul) method needs to be deprecated once we completely remove the DOT.
  private static Optional<Integer> getRevisionFromControllerNameWithDot(String name) {
    String versionSeparator = DOT;
    if (name != null) {
      int index = name.lastIndexOf(versionSeparator);
      if (index >= 0) {
        String version = "";
        try {
          version = name.substring(index + versionSeparator.length());
          Optional<Integer> revision =
              version.contains(DASH) ? Optional.empty() : Optional.of(Integer.parseInt(version));

          // ToDo(anshul) needs better logic here rather than using MAX_REVISIONS
          return (revision.isPresent() && revision.get() < MAX_REVISIONS) ? revision : Optional.empty();
        } catch (NumberFormatException e) {
          logger.error("Couldn't get version from controller name {}", name, e);
          logger.warn("Couldn't get version from controller name {}. {} is not a valid version", name, version, e);
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

  public static String getNormalizedInfraMappingIdLabelValue(String infraMppingId) {
    return convertBase64UuidToCanonicalForm(infraMppingId);
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
