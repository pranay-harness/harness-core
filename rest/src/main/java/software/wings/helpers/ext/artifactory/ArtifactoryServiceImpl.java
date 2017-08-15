package software.wings.helpers.ext.artifactory;

import static java.util.stream.Collectors.toList;
import static software.wings.beans.config.ArtifactoryConfig.Builder.anArtifactoryConfig;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import groovyx.net.http.HttpResponseException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.ArtifactoryRequest;
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl;
import org.jfrog.artifactory.client.model.PackageType;
import org.jfrog.artifactory.client.model.Repository;
import org.jfrog.artifactory.client.model.repository.settings.RepositorySettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.utils.ArtifactType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by sgurubelli on 6/27/17.
 */
public class ArtifactoryServiceImpl implements ArtifactoryService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public List<BuildDetails> getBuilds(
      ArtifactoryConfig artifactoryConfig, String repoKey, String imageName, int maxNumberOfBuilds) {
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig);
    String apiUrl = "api/docker/" + repoKey + "/v2/" + imageName + "/tags/list";
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
      return tags.stream().map(s -> aBuildDetails().withNumber(s).build()).collect(toList());
    }
    return null;
  }

  @Override
  public List<BuildDetails> getVersions(ArtifactoryConfig artifactoryConfig, String repoKey, String groupId,
      String artifactName, ArtifactType artifactType, int maxVersions) {
    return null;
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(ArtifactoryConfig artifactoryConfig, String repositoryPath) {
    return null;
  }

  @Override
  public Map<String, String> getRepositories(ArtifactoryConfig artifactoryConfig, ArtifactType artifactType) {
    switch (artifactType) {
      case DOCKER:
        return getRepositories(artifactoryConfig);
      case RPM:
        return getRepositories(artifactoryConfig, EnumSet.of(PackageType.rpm, PackageType.yum));
      default:
        return getRepositories(artifactoryConfig, EnumSet.of(PackageType.maven));
    }
  }

  private Map<String, String> getRepositories(ArtifactoryConfig artifactoryConfig, EnumSet<PackageType> packageTypes) {
    Map<String, String> repositories = new HashMap<>();
    try {
      String apiUrl = "api/repositories/";
      Artifactory artifactory = getArtifactoryClient(artifactoryConfig);
      ArtifactoryRequest repositoryRequest = new ArtifactoryRequestImpl()
                                                 .apiUrl(apiUrl)
                                                 .method(ArtifactoryRequest.Method.GET)
                                                 .responseType(ArtifactoryRequest.ContentType.JSON);
      List<Map> response = artifactory.restCall(repositoryRequest);
      for (Map repository : response) {
        String repoKey = repository.get("key").toString();
        Repository repo = artifactory.repository(repoKey).get();
        RepositorySettings settings = repo.getRepositorySettings();
        if (packageTypes.contains(settings.getPackageType())) {
          repositories.put(repository.get("key").toString(), repository.get("key").toString());
        }
      }
    } catch (IllegalArgumentException e) {
      logger.error("Error occurred while retrieving Repositories from Artifactory server "
              + artifactoryConfig.getArtifactoryUrl(),
          e);
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Invalid Artifactory credentials");
    } catch (Exception e) {
      logger.error("Error occurred while retrieving Repositories from Artifactory server "
              + artifactoryConfig.getArtifactoryUrl(),
          e);
      if (e instanceof HttpResponseException) {
        HttpResponseException httpResponseException = (HttpResponseException) e;
        if (httpResponseException.getStatusCode() == 401) {
          throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Invalid Artifactory credentials");
        } else if (httpResponseException.getStatusCode() == 403) {
          throw new WingsException(
              ErrorCode.INVALID_ARTIFACT_SERVER, "message", "User not authorized to access artifactory");
        }
      }
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", e.getMessage(), e);
    }
    if (repositories.size() == 0) {
      // Better way of handling Unauthorized access
      logger.info("Repositories are not available of package types {} or User not authorized to access artifactory",
          packageTypes);
      // throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "User not authorized to access
      // artifactory");
    }
    return repositories;
  }

  @Override
  public Map<String, String> getRepositories(ArtifactoryConfig artifactoryConfig) {
    return getRepositories(artifactoryConfig, EnumSet.of(PackageType.docker));
  }

  @Override
  public List<String> getRepoPaths(ArtifactoryConfig artifactoryConfig, String repoKey) {
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig);
    Repository repository = artifactory.repository(repoKey).get();
    String repoLayout = repository.getRepoLayoutRef();
    RepositorySettings settings = repository.getRepositorySettings();
    PackageType packageType = settings.getPackageType();
    if (packageType.equals(PackageType.docker)) {
      return listDockerImages(artifactory, repoKey);
    } else {
      return listArtifactPaths(artifactory, repoKey, packageType, repoLayout);
    }
  }

  @Override
  public List<BuildDetails> getFilePaths(ArtifactoryConfig artifactoryConfig, String repoKey, String groupId,
      String artifactPath, ArtifactType artifactType, int maxVersions) {
    List<String> artifactPaths = new ArrayList<>();
    try {
      String aclQuery = "api/search/aql";
      String requestBody = "items.find({\"repo\":\"" + repoKey + "\"}, {\"depth\": 1})";
      if (!StringUtils.isBlank(artifactPath)) {
        artifactPath = artifactPath + "*";
        if (artifactPath.contains("/")) {
          int index = artifactPath.lastIndexOf('/');
          String subPath = artifactPath.substring(0, index);
          String name = artifactPath.substring(index + 1);
          requestBody = "items.find({\"repo\":\"" + repoKey + "\"}, {\"path\": \"" + subPath
              + "\"}, {\"name\": {\"$match\": \"" + name + "\"}})";
        } else {
          requestBody = "items.find({\"repo\":\"" + repoKey + "\"}, {\"depth\": 1}, {\"name\": {\"$match\": \""
              + artifactPath + "\"}})";
        }
      }
      ArtifactoryRequest repositoryRequest = new ArtifactoryRequestImpl()
                                                 .apiUrl(aclQuery)
                                                 .method(ArtifactoryRequest.Method.POST)
                                                 .requestBody(requestBody)
                                                 .requestType(ArtifactoryRequest.ContentType.TEXT)
                                                 .responseType(ArtifactoryRequest.ContentType.JSON);
      Artifactory artifactory = getArtifactoryClient(artifactoryConfig);
      LinkedHashMap<String, List> response = artifactory.restCall(repositoryRequest);
      if (response != null) {
        List<LinkedHashMap<String, String>> results = response.get("results");
        for (LinkedHashMap<String, String> result : results) {
          if (!result.get("created_by").equals("_system_")) {
            String path = result.get("path");
            String name = result.get("name");
            if (!path.equals(".")) {
              artifactPaths.add(repoKey + "/" + path + "/" + name);
            } else {
              artifactPaths.add(repoKey + "/" + name);
            }
          }
        }
      }
    } catch (Exception e) {
      logger.error(
          "Error occurred while retrieving File Paths from Artifactory server " + artifactoryConfig.getArtifactoryUrl(),
          e);
      if (e instanceof HttpResponseException) {
        HttpResponseException httpResponseException = (HttpResponseException) e;
        if (httpResponseException.getStatusCode() == 401) {
          throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Invalid Artifactory credentials");
        } else if (httpResponseException.getStatusCode() == 403) {
          throw new WingsException(
              ErrorCode.INVALID_ARTIFACT_SERVER, "message", "User not authorized to access artifactory");
        }
      }
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", e.getMessage(), e);
    }
    logger.info("Artifact paths order from Artifactory Server" + artifactPaths);
    // artifactPaths = artifactPaths.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
    // logger.info("Artifact paths after reverse order sorting from Artifactory Server" + artifactPaths);
    return artifactPaths.stream()
        .map(s -> aBuildDetails().withNumber(s.substring(s.lastIndexOf('/') + 1)).withArtifactPath(s).build())
        .collect(toList());
  }

  @Override
  public BuildDetails getLatestFilePath(ArtifactoryConfig artifactoryConfig, String repoKey, String groupId,
      String artifactName, ArtifactType artifactType) {
    List<BuildDetails> buildDetails = getFilePaths(artifactoryConfig, repoKey, groupId, artifactName, artifactType, 1);
    if (!CollectionUtils.isEmpty(buildDetails) && buildDetails.size() > 0) {
      return buildDetails.get(0);
    }
    return null;
  }

  private List<String> listArtifactPaths(
      Artifactory artifactory, String repoKey, PackageType packageType, String repoLayout) {
    List<String> artifactNames = new ArrayList<>();
    try {
      String aclQuery = "api/search/aql";
      String requestBody = "items.find({\"repo\":{\"$eq\":\"" + repoKey + "\"}})";
      ArtifactoryRequest repositoryRequest = new ArtifactoryRequestImpl()
                                                 .apiUrl(aclQuery)
                                                 .method(ArtifactoryRequest.Method.POST)
                                                 .requestBody(requestBody)
                                                 .requestType(ArtifactoryRequest.ContentType.TEXT)
                                                 .responseType(ArtifactoryRequest.ContentType.JSON);
      LinkedHashMap<String, List> response = artifactory.restCall(repositoryRequest);
      Map<String, String> artifactPaths = new HashMap<>();
      String propertyUrl = "api/storage/" + repoKey + "/";
      if (response != null) {
        List<LinkedHashMap<String, String>> results = response.get("results");
        for (LinkedHashMap<String, String> result : results) {
          if (!result.get("created_by").equals("_system_")) {
            String path = result.get("path");
            String name = result.get("name");
            String artifactPath;
            if (!path.equals(".")) {
              artifactPath = path + "/" + name;
              artifactPaths.put(path + "/" + name, name);
            } else {
              artifactPath = name;
            }
            if (packageType.equals(PackageType.rpm)) {
              ArtifactoryRequest request = new ArtifactoryRequestImpl()
                                               .apiUrl(propertyUrl + artifactPath)
                                               .addQueryParam("properties", "rpm.metadata.name")
                                               .method(ArtifactoryRequest.Method.GET)
                                               .responseType(ArtifactoryRequest.ContentType.JSON);
              try {
                LinkedHashMap<String, LinkedHashMap<String, List<String>>> propertyResponse =
                    artifactory.restCall(request);
                if (propertyResponse != null) {
                  LinkedHashMap<String, List<String>> properties = propertyResponse.get("properties");
                  String artifactName = properties.get("rpm.metadata.name").get(0);
                  if (!path.equals(".")) {
                    artifactName = path + "/" + artifactName;
                  }
                  if (!artifactNames.contains(artifactName)) {
                    artifactNames.add(artifactName);
                  }
                }
              } catch (Exception e) {
                if (e instanceof HttpResponseException) {
                  HttpResponseException httpResponseException = (HttpResponseException) e;
                  if (httpResponseException.getStatusCode() == 404) {
                    logger.info(
                        "Property [rpm.metadata.name] is not set for artifact path {} of artifactory server {} ",
                        artifactory.getUri(), artifactPath);
                    artifactNames.add(artifactPath);
                  }
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      logger.error("Error occurred while retrieving artifact names from Artifactory server " + artifactory.getUri(), e);
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", e.getMessage(), e);
    }
    return artifactNames;
  }

  public List<String> listDockerImages(Artifactory artifactory, String repoKey) {
    try {
      String apiUrl = "api/docker/" + repoKey + "/v2"
          + "/_catalog";
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
    } catch (Exception e) {
      logger.error(String.format("Error occurred while listing docker images from artifactory %s for Repo %s",
                       artifactory, repoKey),
          e);
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", e.getMessage(), e);
    }
    return new ArrayList<>();
  }

  /**
   * Get Artifactory Client
   *
   * @param artifactoryConfig
   * @return Artifactory returns artifactory client
   */

  private Artifactory getArtifactoryClient(ArtifactoryConfig artifactoryConfig) {
    try {
      ArtifactoryClientBuilder builder = ArtifactoryClientBuilder.create();
      builder.setUrl(getBaseUrl(artifactoryConfig));
      if (StringUtils.isBlank(artifactoryConfig.getUsername())) {
        logger.info("Username is not for artifactory config {} . Will use anonymous access.",
            artifactoryConfig.getArtifactoryUrl());
      } else if (artifactoryConfig.getPassword() == null
          || StringUtils.isBlank(new String(artifactoryConfig.getPassword()))) {
        logger.info("Username is set. However no password set for artifactory config {}",
            artifactoryConfig.getArtifactoryUrl());
        builder.setUsername(artifactoryConfig.getUsername());
      } else {
        builder.setUsername(artifactoryConfig.getUsername());
        builder.setPassword(new String(artifactoryConfig.getPassword()));
      }
      // TODO Ignore SSL issues -
      return builder.build();
    } catch (Exception ex) {
      logger.error("Error occurred while trying to initialize artifactory", ex);
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Invalid Artifactory credentials");
    }
  }

  private String getBaseUrl(ArtifactoryConfig artifactoryConfig) {
    String baseUrl = artifactoryConfig.getArtifactoryUrl();
    if (!baseUrl.endsWith("/")) {
      baseUrl = baseUrl + "/";
    }
    return baseUrl;
  }

  public static void main(String... args) {
    String url = "https://artifactory.harness.io/artifactory";
    // url = "https://127.0.0.1:8000/";

    ArtifactoryServiceImpl artifactoryService = new ArtifactoryServiceImpl();
    System.out.println("Hello welcome to Artifactory");
    ArtifactoryConfig artifactoryConfig = anArtifactoryConfig()
                                              .withArtifactoryUrl(url)
                                              .withUsername("admin")
                                              .withPassword("harness123!".toCharArray())
                                              .build();

    // artifactoryConfig = anArtifactoryConfig().withArtifactoryUrl(url).build();
    /* List<BuildDetails> buildDetails = new ArtifactoryServiceImpl().getBuilds(artifactoryConfig,
     "docker-local/wingsplugins/todolist", 1); for (BuildDetails buildDetail : buildDetails) { System.out.println("Build
     Number" +  buildDetail.getNumber());
     }*/

    Map<String, String> repositories = artifactoryService.getRepositories(artifactoryConfig);
    System.out.println("Repositories" + repositories);

    // List<String> images = artifactoryService.listDockerImages(artifactoryConfig, "docker");
    // images.forEach(s -> System.out.println(s));

    // artifactoryService.getRepoPaths(artifactoryConfig, "harness-rpm");
    String str = "todolist-1.0-1.x86_64.rpm";
    str = str.replace("todolist", "");

    str = StringUtils.substringBetween("todolist-1.0-1.x86_64.rpm", "todolist", "x86_64");
    System.out.println("Substring: " + str.substring(1, str.length() - 1));
    str = "todolist-1.0-1.rpm";
    // int lastIndex = str.lastIndexOf(".");
    str = str.replace("todolist", "");
    int lastIndex = str.lastIndexOf(".");
    System.out.println("Substring: " + str.substring(1, lastIndex));
    // artifactoryService.doArtifactsSearch(artifactoryConfig, "harness-rpm", "todolist");

    // artifactoryService.getFilePaths(artifactoryConfig, "harness-rpm", null, "", ArtifactType.RPM, 50);

    /*List<BuildDetails> buildDetails =  artifactoryService.getFilePaths(artifactoryConfig, "harness-rpm", null,
     "todolist*", ArtifactType.RPM, 50);

     Comparator<BuildDetails> byFirst = Comparator.comparing(buildDetails1 -> buildDetails1.getNumber(),
     Comparator.reverseOrder()); List<BuildDetails> sortedList = buildDetails.stream()
         .sorted(byFirst).collect(toList());
     sortedList.forEach(buildDetails1 ->
     System.out.println("Build" +  buildDetails1.getNumber()));
     System.out.println("Comparison: " + "10".compareTo("1"));*/

    String artifactPathRegex = "a?todolist*";
    Pattern pattern = Pattern.compile(artifactPathRegex.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));

    String path = "abtodolist";
    if (pattern.matcher(path).matches()) {
      System.out.println("path = " + path);
    } else {
      System.out.println("Pattern not found");
    }
  }
}
