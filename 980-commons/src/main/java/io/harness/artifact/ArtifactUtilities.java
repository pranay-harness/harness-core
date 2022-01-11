/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.artifact;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.net.MalformedURLException;
import java.net.URL;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CDC)
public class ArtifactUtilities {
  public static String getArtifactoryRegistryUrl(String url, String dockerRepositoryServer, String jobName) {
    String registryUrl;
    if (dockerRepositoryServer != null) {
      registryUrl = format("http%s://%s", url.startsWith("https") ? "s" : "", dockerRepositoryServer);
    } else {
      int firstDotIndex = url.indexOf('.');
      int slashAfterDomain = url.indexOf('/', firstDotIndex);
      registryUrl = url.substring(0, firstDotIndex) + "-" + jobName
          + url.substring(firstDotIndex, slashAfterDomain > 0 ? slashAfterDomain : url.length());
    }
    return registryUrl;
  }

  public static String getArtifactoryRepositoryName(
      String url, String dockerRepositoryServer, String jobName, String imageName) {
    String registryName;
    if (dockerRepositoryServer != null) {
      registryName = dockerRepositoryServer + "/" + imageName;
    } else {
      String registryUrl = getArtifactoryRegistryUrl(url, null, jobName);
      String namePrefix = registryUrl.substring(registryUrl.indexOf("://") + 3);
      registryName = namePrefix + "/" + imageName;
    }
    return registryName;
  }

  public static String getNexusRegistryUrl(String nexusUrl, String dockerPort, String dockerRegistryUrl) {
    if (isEmpty(dockerRegistryUrl)) {
      String registryUrl = extractNexusDockerRegistryUrl(nexusUrl);
      if (isNotEmpty(dockerPort)) {
        registryUrl = registryUrl + ":" + dockerPort;
      }
      return registryUrl;
    }
    if (dockerRegistryUrl.startsWith("http") || dockerRegistryUrl.startsWith("https")) {
      // User can input the docker registry with real http or https
      return dockerRegistryUrl;
    }
    return format("http%s://%s", nexusUrl.startsWith("https") ? "s" : "", extractUrl(dockerRegistryUrl));
  }

  private static String extractNexusDockerRegistryUrl(String url) {
    int firstDotIndex = url.indexOf('.');
    int colonIndex = url.indexOf(':', firstDotIndex);
    int endIndex = colonIndex > 0 ? colonIndex : url.length();
    return url.substring(0, endIndex);
  }

  public static String getNexusRepositoryName(
      String nexusUrl, String dockerPort, String dockerRegistryUrl, String imageName) {
    if (isEmpty(dockerRegistryUrl)) {
      String registryUrl = getNexusRegistryUrl(nexusUrl, dockerPort, dockerRegistryUrl);
      String namePrefix = registryUrl.substring(registryUrl.indexOf("://") + 3);
      return namePrefix + "/" + imageName;
    } else {
      return extractUrl(dockerRegistryUrl) + "/" + imageName;
    }
  }

  private static String extractUrl(String dockerRegistryUrl) {
    try {
      URL url = new URL(dockerRegistryUrl);
      if (url.getPort() != -1) {
        return url.getHost() + ":" + url.getPort();
      }
      return url.getHost();
    } catch (MalformedURLException e) {
      return dockerRegistryUrl;
    }
  }

  public static String getFileSearchPattern(String artifactPath) {
    int index = artifactPath.lastIndexOf('/');
    if (index == -1) {
      index = artifactPath.lastIndexOf('\\');
    }

    if (index == artifactPath.length() - 1) {
      return "*";
    } else {
      return artifactPath.substring(index + 1);
    }
  }

  public static String getFileParentPath(String artifactPath) {
    int index = artifactPath.lastIndexOf('/');
    if (index == -1) {
      index = artifactPath.lastIndexOf('\\');
    }

    if (index == -1) {
      return "";
    } else {
      return artifactPath.substring(0, index);
    }
  }
}
