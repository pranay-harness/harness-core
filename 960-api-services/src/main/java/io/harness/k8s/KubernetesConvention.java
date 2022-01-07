/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.k8s;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trim;

import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;
import io.harness.k8s.model.ImageDetails;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by brett on 3/8/17
 */
@Slf4j
public class KubernetesConvention {
  public static final String DOT = ".";
  public static final String DASH = "-";

  public static final String ReleaseHistoryKeyName = "releaseHistory";
  public static final String CompressedReleaseHistoryFlag = "isReleaseHistoryCompressed";

  private static final String VOLUME_PREFIX = "vol-";
  private static final String VOLUME_SUFFIX = "-vol";
  private static final String SECRET_PREFIX = "hs-";
  private static final String SECRET_SUFFIX = "-hs";
  private static final String CONTAINER_PREFIX = "hs-";
  private static final String CONTAINER_SUFFIX = "-hs";
  private static final int HELM_RELEASE_VERSION_LENGTH = 15;
  private static final int MAX_REVISIONS = 100000;
  private static Pattern wildCharPattern = Pattern.compile("[_+*/\\\\ &@$|\"':]");
  private static final String HARNESS_INTERNAL = "harness-internal-";
  private static final String GIT_REPO_URL_REGEX = "^(https|git)(:\\/\\/|@)([^\\/:]+)[\\/:]([^\\/:]+)\\/(.+).git$";

  public static String getInternalHarnessConfigName(String infraMappingId) {
    return HARNESS_INTERNAL + getNormalizedInfraMappingIdLabelValue(infraMappingId);
  }

  public static String getControllerName(String prefix, int revision) {
    return normalize(prefix) + DASH + revision;
  }

  public static String getControllerNamePrefix(String appName, String serviceName, String envName) {
    return normalize(appName + DASH + serviceName + DASH + envName);
  }

  public static String getPrefixFromControllerName(String controllerName) {
    int index = controllerName.lastIndexOf(DASH);
    if (index > 0) {
      try {
        Integer.parseInt(controllerName.substring(index + DASH.length()));
        return controllerName.substring(0, index);
      } catch (NumberFormatException e) {
        // Not versioned
      }
    }
    return controllerName;
  }

  public static String getServiceNameFromControllerName(String controllerName) {
    int index = controllerName.lastIndexOf(DASH);
    if (index > 0) {
      try {
        Integer.parseInt(controllerName.substring(index + DASH.length()));
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

  public static String getKubernetesGitSecretName(String repoUrl) {
    Pattern gitUrlPattern = Pattern.compile(GIT_REPO_URL_REGEX);
    Matcher matcher = gitUrlPattern.matcher(repoUrl);
    if (!matcher.find()) {
      throw new InvalidArgumentsException(format("Unrecognized GIT repository URL: %s", repoUrl), WingsException.USER);
    }

    String gitRepoName = matcher.group(5);
    String gitRepoUsername = matcher.group(4);

    return SECRET_PREFIX + gitRepoUsername + "-" + gitRepoName + SECRET_SUFFIX;
  }

  public static Optional<Integer> getRevisionFromControllerName(String name) {
    if (name != null) {
      int index = name.lastIndexOf(DASH);
      if (index >= 0) {
        String version = "";
        try {
          version = name.substring(index + DASH.length());
          Optional<Integer> revision =
              version.contains(DASH) ? Optional.empty() : Optional.of(Integer.parseInt(version));

          // ToDo(anshul) needs better logic here rather than using MAX_REVISIONS
          return (revision.isPresent() && revision.get() < MAX_REVISIONS) ? revision : Optional.empty();
        } catch (NumberFormatException e) {
          log.warn("Couldn't get version from controller name {}. {} is not a valid version", name, version, e);
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
          log.error("Couldn't get version from controller name {}", name, e);
          log.warn("Couldn't get version from controller name {}. {} is not a valid version", name, version, e);
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

  public static String getHelmReleaseName(String helmReleasePrefix, String infraMappingId) {
    String infraMappingIdPrefix = infraMappingId.substring(0, 7).toLowerCase().replace('-', 'z').replace('_', 'z');
    String revision = "harness" + DASH + infraMappingIdPrefix;
    return normalize(helmReleasePrefix) + DASH + revision;
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
