package software.wings.helpers.ext.artifactory;

import static software.wings.beans.config.ArtifactoryConfig.Builder.anArtifactoryConfig;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import org.apache.commons.collections.CollectionUtils;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClient;
import org.jfrog.artifactory.client.ArtifactoryRequest;
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl;
import org.jfrog.artifactory.client.model.LightweightRepository;
import org.jfrog.artifactory.client.model.PackageType;
import org.jfrog.artifactory.client.model.Repository;
import org.jfrog.artifactory.client.model.impl.RepositoryTypeImpl;
import org.jfrog.artifactory.client.model.repository.settings.RepositorySettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by sgurubelli on 6/27/17.
 */
public class ArtifactoryServiceImpl implements ArtifactoryService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public List<BuildDetails> getBuilds(
      ArtifactoryConfig artifactoryConfig, String repoKey, String imageName, int maxNumberOfBuilds) {
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig);
    String apiUrl = "api/docker/" + repoKey + "/v2" + imageName + "/tags/list";
    ArtifactoryRequest repositoryRequest = new ArtifactoryRequestImpl()
                                               .apiUrl(apiUrl)
                                               .method(ArtifactoryRequest.Method.GET)
                                               .responseType(ArtifactoryRequest.ContentType.JSON);
    Map response = artifactory.restCall(repositoryRequest);
    if (response != null) {
      List<String> tags = (List<String>) response.get("tags");
      if (CollectionUtils.isEmpty(tags)) {
        return null;
      }
      return tags.stream().map(s -> aBuildDetails().withNumber(s).build()).collect(Collectors.toList());
    }
    return null;
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(ArtifactoryConfig artifactoryConfig, String repositoryPath) {
    return null;
  }

  @Override
  public Map<String, String> getRepositories(ArtifactoryConfig artifactoryConfig) {
    Map<String, String> repositories = new HashMap<>();
    List<LightweightRepository> repoList =
        getArtifactoryClient(artifactoryConfig).repositories().list(RepositoryTypeImpl.LOCAL);
    for (LightweightRepository lightweightRepository : repoList) {
      Repository repo = getArtifactoryClient(artifactoryConfig).repository(lightweightRepository.getKey()).get();
      RepositorySettings settings = repo.getRepositorySettings();
      PackageType packageType = settings.getPackageType();
      if (packageType.equals(PackageType.docker)) {
        repositories.put(lightweightRepository.getKey(), lightweightRepository.getKey());
      }
    }
    return repositories;
  }

  @Override
  public List<String> getRepoPaths(ArtifactoryConfig artifactoryConfig, String repoKey) {
    return listDockerImages(artifactoryConfig, repoKey);
  }

  public List<String> listDockerImages(ArtifactoryConfig artifactoryConfig, String repoKey) {
    String apiUrl = "api/docker/" + repoKey + "/v2"
        + "/_catalog";
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig);
    ArtifactoryRequest repositoryRequest = new ArtifactoryRequestImpl()
                                               .apiUrl(apiUrl)
                                               .method(ArtifactoryRequest.Method.GET)
                                               .responseType(ArtifactoryRequest.ContentType.JSON);
    Map response = artifactory.restCall(repositoryRequest);
    if (response != null) {
      List<String> repositories = (List<String>) response.get("repositories");
      if (CollectionUtils.isEmpty(repositories)) {
        return null;
      }
      return repositories;
    }
    return null;
  }

  /**
   * Get Artifactory Client
   * @param artifactoryConfig
   * @return Artifactory returns artifactory client
   */
  private Artifactory getArtifactoryClient(ArtifactoryConfig artifactoryConfig) {
    Artifactory artifactory = ArtifactoryClient.create(
        getBaseUrl(artifactoryConfig), artifactoryConfig.getUsername(), new String(artifactoryConfig.getPassword()));
    return artifactory;
  }

  private String getBaseUrl(ArtifactoryConfig artifactoryConfig) {
    String baseUrl = artifactoryConfig.getArtifactoryUrl();
    if (!baseUrl.endsWith("/")) {
      baseUrl = baseUrl + "/";
    }
    return baseUrl;
  }

  public static void main(String... args) {
    ArtifactoryServiceImpl artifactoryService = new ArtifactoryServiceImpl();
    System.out.println("Hello welcome to Artifactory");
    ArtifactoryConfig artifactoryConfig = anArtifactoryConfig()
                                              .withArtifactoryUrl("http://localhost/artifactory")
                                              .withUsername("admin")
                                              .withPassword("harness123!".toCharArray())
                                              .build();
    /* List<BuildDetails> buildDetails = new ArtifactoryServiceImpl().getBuilds(artifactoryConfig,
     "docker-local/wingsplugins/todolist", 1); for (BuildDetails buildDetail : buildDetails) { System.out.println("Build
     Number" +  buildDetail.getNumber());
     }*/

    // artifactoryService.getRepositories(artifactoryConfig);

    List<String> images = artifactoryService.listDockerImages(artifactoryConfig, "docker-local");
    images.forEach(s -> System.out.println(s));
  }
}
