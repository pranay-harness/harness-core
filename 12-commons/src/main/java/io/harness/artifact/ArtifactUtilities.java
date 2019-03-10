package io.harness.artifact;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;

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

  public static String getNexusRegistryUrl(String url, String dockerPort) {
    String registryUrl;
    int firstDotIndex = url.indexOf('.');
    int colonIndex = url.indexOf(':', firstDotIndex);
    int endIndex = colonIndex > 0 ? colonIndex : url.length();
    registryUrl = url.substring(0, endIndex);
    if (isNotEmpty(dockerPort)) {
      registryUrl = registryUrl + ":" + dockerPort;
    }
    return registryUrl;
  }

  public static String getNexusRepositoryName(String url, String dockerPort, String imageName) {
    String registryUrl = getNexusRegistryUrl(url, dockerPort);
    String namePrefix = registryUrl.substring(registryUrl.indexOf("://") + 3);
    return namePrefix + "/" + imageName;
  }
}
