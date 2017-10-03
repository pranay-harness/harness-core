package software.wings.helpers.ext.artifactory;

import static com.google.common.collect.Iterables.isEmpty;
import static java.util.stream.Collectors.toList;
import static org.jfrog.artifactory.client.ArtifactoryRequest.ContentType.JSON;
import static org.jfrog.artifactory.client.ArtifactoryRequest.ContentType.TEXT;
import static org.jfrog.artifactory.client.ArtifactoryRequest.Method.GET;
import static org.jfrog.artifactory.client.ArtifactoryRequest.Method.POST;
import static org.jfrog.artifactory.client.model.PackageType.docker;
import static org.jfrog.artifactory.client.model.PackageType.maven;
import static org.jfrog.artifactory.client.model.PackageType.rpm;
import static org.jfrog.artifactory.client.model.PackageType.yum;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import groovyx.net.http.HttpResponseException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.ArtifactoryRequest;
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl;
import org.jfrog.artifactory.client.model.PackageType;
import org.jfrog.artifactory.client.model.RepoPath;
import org.jfrog.artifactory.client.model.Repository;
import org.jfrog.artifactory.client.model.repository.settings.RepositorySettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.delegatetasks.collect.artifacts.ArtifactCollectionTaskHelper;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.utils.ArtifactType;
import software.wings.waitnotify.ListNotifyResponseData;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

/**
 * Created by sgurubelli on 6/27/17.
 */
public class ArtifactoryServiceImpl implements ArtifactoryService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private ArtifactCollectionTaskHelper artifactCollectionTaskHelper;

  @Inject ExecutorService executorService;

  @Override
  public Map<String, String> getRepositories(ArtifactoryConfig artifactoryConfig) {
    return getRepositories(artifactoryConfig, EnumSet.of(docker));
  }

  @Override
  public Map<String, String> getRepositories(ArtifactoryConfig artifactoryConfig, ArtifactType artifactType) {
    switch (artifactType) {
      case DOCKER:
        return getRepositories(artifactoryConfig);
      case RPM:
        return getRepositories(artifactoryConfig, EnumSet.of(rpm, yum));
      default:
        return getRepositories(artifactoryConfig, EnumSet.of(maven));
    }
  }

  private Map<String, String> getRepositories(ArtifactoryConfig artifactoryConfig, EnumSet<PackageType> packageTypes) {
    logger.info("Retrieving repositories for packages {}", packageTypes.toArray());
    Map<String, String> repositories = new HashMap<>();
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig);
    ArtifactoryRequest repositoryRequest =
        new ArtifactoryRequestImpl().apiUrl("api/repositories/").method(GET).responseType(JSON);
    try {
      List<Map> response = artifactory.restCall(repositoryRequest);
      for (Map repository : response) {
        String repoKey = repository.get("key").toString();
        try {
          Repository repo = artifactory.repository(repoKey).get();
          RepositorySettings settings = repo.getRepositorySettings();
          if (packageTypes.contains(settings.getPackageType())) {
            repositories.put(repository.get("key").toString(), repository.get("key").toString());
          }
        } catch (Exception e) {
          logger.warn("Failed to get repository settings for repo {}, Reason {}", repoKey, e.getMessage());
        }
      }
      if (repositories.size() == 0) {
        // Better way of handling Unauthorized access
        logger.info("Repositories are not available of package types {} or User not authorized to access artifactory",
            packageTypes);
      }
      logger.info("Retrieving repositories for packages {} success", packageTypes.toArray());
    } catch (Exception e) {
      logger.error("Error occurred while retrieving Repositories from Artifactory server " + artifactory.getUri(), e);
      if (e instanceof HttpResponseException) {
        HttpResponseException httpResponseException = (HttpResponseException) e;
        if (httpResponseException.getStatusCode() == 404) {
          logger.warn("User not authorized to perform deep level search. Trying with different search api");
          throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message",
              "Artifact server may not be running at " + artifactoryConfig.getArtifactoryUrl());
        }
      }
      handleException(e);
    }
    return repositories;
  }

  @Override
  public List<String> getRepoPaths(ArtifactoryConfig artifactoryConfig, String repoKey) {
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig);
    try {
      Repository repository = artifactory.repository(repoKey).get();
      RepositorySettings settings = repository.getRepositorySettings();
      PackageType packageType = settings.getPackageType();
      if (packageType.equals(docker)) {
        return listDockerImages(artifactory, repoKey);
      } else if (packageType.equals(maven)) {
        return getGroupIds(artifactory, repoKey);
      }
    } catch (Exception e) {
      logger.error("Error occurred while retrieving repository  from artifactory {} for Repo {}",
          artifactoryConfig.getArtifactoryUrl(), repoKey, e);
      handleException(e);
    }
    return new ArrayList<>();
  }

  private List<String> listDockerImages(Artifactory artifactory, String repoKey) {
    List<String> images = new ArrayList<>();
    try {
      logger.info("Retrieving docker images from artifactory url {} and repo key {}", artifactory.getUri(), repoKey);
      Map response = artifactory.restCall(new ArtifactoryRequestImpl()
                                              .apiUrl("api/docker/" + repoKey + "/v2"
                                                  + "/_catalog")
                                              .method(GET)
                                              .responseType(JSON));
      if (response != null) {
        images = (List<String>) response.get("repositories");
        if (CollectionUtils.isEmpty(images)) {
          logger.info("No docker images from artifactory url {} and repo key {}", artifactory.getUri(), repoKey);
          images = new ArrayList<>();
        }
        logger.info("Retrieving images from artifactory url {} and repo key {} success", artifactory.getUri(), repoKey);
      }
    } catch (Exception e) {
      logger.error(String.format("Error occurred while listing docker images from artifactory %s for Repo %s",
                       artifactory, repoKey),
          e);
      handleException(e);
    }
    return images;
  }

  @Override
  public List<BuildDetails> getBuilds(
      ArtifactoryConfig artifactoryConfig, String repoKey, String imageName, int maxNumberOfBuilds) {
    logger.info("Retrieving docker tags for repoKey {} imageName {} ", repoKey, imageName);
    List<BuildDetails> buildDetails = new ArrayList<>();
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig);
    try {
      ArtifactoryRequest repositoryRequest = new ArtifactoryRequestImpl()
                                                 .apiUrl("api/docker/" + repoKey + "/v2/" + imageName + "/tags/list")
                                                 .method(GET)
                                                 .responseType(JSON);
      Map response = artifactory.restCall(repositoryRequest);
      if (response != null) {
        List<String> tags = (List<String>) response.get("tags");
        if (CollectionUtils.isEmpty(tags)) {
          logger.info("No  docker tags for repoKey {} imageName {} success ", repoKey, imageName);
          return buildDetails;
        }
        buildDetails = tags.stream().map(s -> aBuildDetails().withNumber(s).build()).collect(toList());
      }
    } catch (Exception e) {
      logger.error("Error occurred while listing docker tags from artifactory {} for Repo {} for image {} ",
          artifactoryConfig.getArtifactoryUrl(), repoKey, imageName, e);
      handleException(e);
    }
    logger.info("Retrieving docker tags for repoKey {} imageName {} success ", repoKey, imageName);
    return buildDetails;
  }

  @Override
  public List<BuildDetails> getFilePaths(ArtifactoryConfig artifactoryConfig, String repoKey, String groupId,
      String artifactPath, ArtifactType artifactType, int maxVersions) {
    List<String> artifactPaths = new ArrayList<>();
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig);
    try {
      String aclQuery = "api/search/aql";
      String requestBody = "items.find({\"repo\":\"" + repoKey + "\"}, {\"depth\": 1})";
      if (!StringUtils.isBlank(artifactPath)) {
        if (artifactPath.startsWith("/")) {
          String subPath = artifactPath.substring(1, artifactPath.length());
          requestBody = "items.find({\"repo\":\"" + repoKey + "\"}, {\"path\": {\"$match\":\"" + subPath + "\"}})";
        } else {
          artifactPath = artifactPath + "*";
          requestBody = "items.find({\"repo\":\"" + repoKey + "\"}, {\"depth\": 1}, {\"name\": {\"$match\": \""
              + artifactPath + "\"}})";
        }
      }
      ArtifactoryRequest repositoryRequest = new ArtifactoryRequestImpl()
                                                 .apiUrl(aclQuery)
                                                 .method(POST)
                                                 .requestBody(requestBody)
                                                 .requestType(TEXT)
                                                 .responseType(JSON);
      LinkedHashMap<String, List> response = artifactory.restCall(repositoryRequest);
      if (response != null) {
        List<LinkedHashMap<String, String>> results = response.get("results");
        if (results != null) {
          for (LinkedHashMap<String, String> result : results) {
            String created_by = result.get("created_by");
            if (created_by == null || !created_by.equals("_system_")) {
              String path = result.get("path");
              String name = result.get("name");
              if (path != null && !path.equals(".")) {
                artifactPaths.add(repoKey + "/" + path + "/" + name);
              } else {
                artifactPaths.add(repoKey + "/" + name);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      logger.error("Error occurred while retrieving File Paths from Artifactory server {}",
          artifactoryConfig.getArtifactoryUrl(), e);
      handleException(e);
    }
    logger.info("Artifact paths order from Artifactory Server" + artifactPaths);
    return artifactPaths.stream()
        .map(s -> aBuildDetails().withNumber(s.substring(s.lastIndexOf('/') + 1)).withArtifactPath(s).build())
        .collect(toList());
  }

  public List<String> getGroupIds(Artifactory artifactory, String repoKey) {
    List<String> groupIdList = new ArrayList<>();
    try {
      String apiStorageQuery = "api/storage/";
      apiStorageQuery = apiStorageQuery + repoKey;
      ArtifactoryRequest repositoryRequest = new ArtifactoryRequestImpl()
                                                 .apiUrl(apiStorageQuery)
                                                 .method(GET)
                                                 .responseType(JSON)
                                                 .addQueryParam("list", "")
                                                 .addQueryParam("deep", "1");
      LinkedHashMap<String, Object> response = artifactory.restCall(repositoryRequest);
      Set<String> groupIds = new HashSet<>();
      if (response != null) {
        List<LinkedHashMap<String, String>> files = (List<LinkedHashMap<String, String>>) response.get("files");
        if (files != null) {
          for (LinkedHashMap<String, String> file : files) {
            String uri = file.get("uri");
            if (uri != null) {
              // strip out the file
              String groupId = uri.substring(0, uri.lastIndexOf("/"));
              // strip out the artifact id
              if (groupId.contains("/")) {
                groupId = uri.substring(0, groupId.lastIndexOf("/"));
              }
              if (groupId.contains("/")) {
                groupId = uri.substring(1, groupId.lastIndexOf("/"));
                // strip out the version
              }
              groupId = groupId.replace("/", ".");
              if (groupIds.add(groupId)) {
                groupIdList.add(groupId);
              }
            }
          }
        }
      }

    } catch (Exception e) {
      if (e instanceof HttpResponseException) {
        HttpResponseException httpResponseException = (HttpResponseException) e;
        if (httpResponseException.getStatusCode() == 403) {
          logger.warn("User not authorized to perform deep level search. Trying with different search api");
          return listGroupIds(artifactory, repoKey);
        }
      }
      logger.error("Error occurred while retrieving groupIds from artifactory server {} and repoKey {}",
          artifactory.getUri(), repoKey, e);
      handleException(e);
    }
    return groupIdList;
  }

  /**
   * Listing groupIds by recursively for non authenticated users
   * @param artifactory
   * @param repoKey
   * @return
   */
  private List<String> listGroupIds(Artifactory artifactory, String repoKey) {
    List<String> groupIdList = new ArrayList<>();
    logger.info("Retrieving groupId paths recursev");
    List<String> paths = getGroupIdPaths(artifactory, repoKey, "", new ArrayList<>());
    Set<String> groupIds = new HashSet<>();
    if (paths != null) {
      logger.info("Retrieved  groupId paths success. Size {}", paths.size());
      for (String path : paths) {
        // strip out the file
        logger.info("Repo path {}", path);
        if (path.length() == 1) {
          continue;
        }
        // Path must contain at least three elements: Group, Artifact, and Version
        String[] pathElems = path.substring(1).split("/");
        if (pathElems.length >= 3) {
          StringBuilder groupIdBuilder = new StringBuilder();
          for (int i = 0; i < pathElems.length - 2; i++) {
            groupIdBuilder.append(pathElems[i]);
            if (i != pathElems.length - 3) {
              groupIdBuilder.append(".");
            }
          }
          String groupId = groupIdBuilder.toString();
          if (groupIds.add(groupId)) {
            logger.info("Group Id {}", groupId);
            groupIdList.add(groupId);
          }
        }
      }
    }
    logger.info("Retrieved unique groupIds size {}", groupIdList.size());
    return groupIdList;
  }

  private List<String> getGroupIdPaths(
      Artifactory artifactory, String repoKey, String repoPath, List<String> groupIds) {
    try {
      String apiStorageQuery = "api/storage/";
      apiStorageQuery = apiStorageQuery + repoKey;
      if (repoPath != null && !repoPath.isEmpty()) {
        apiStorageQuery = apiStorageQuery + repoPath;
      }
      ArtifactoryRequest repositoryRequest =
          new ArtifactoryRequestImpl().apiUrl(apiStorageQuery).method(GET).responseType(JSON);
      LinkedHashMap<String, Object> response = artifactory.restCall(repositoryRequest);
      if (response == null) {
        return groupIds;
      }
      List<LinkedHashMap<String, Object>> results = (List<LinkedHashMap<String, Object>>) response.get("children");
      if (isEmpty(results)) {
        return groupIds;
      }
      String path = (String) response.get("path");
      for (LinkedHashMap<String, Object> result : results) {
        String uri = (String) result.get("uri");
        boolean folder = (boolean) result.get("folder");
        if (uri != null) {
          if (!folder) {
            groupIds.add(path.endsWith("/") ? path : path + "/");
            return groupIds;
          }
          if (path.endsWith("/")) {
            getGroupIdPaths(artifactory, repoKey, uri, groupIds);
          } else {
            getGroupIdPaths(artifactory, repoKey, path + uri, groupIds);
          }
        }
      }
    } catch (Exception e) {
      handleException(e);
    }
    return groupIds;
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

  /**
   * Gets the latest version of the given artifact
   *
   * @param artifactoryConfig
   * @param repoId
   * @param groupId
   * @param artifactId
   * @return
   */
  @Override
  public BuildDetails getLatestVersion(
      ArtifactoryConfig artifactoryConfig, String repoId, String groupId, String artifactId) {
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig);
    try {
      String msg = "latest version for artifactory server [" + artifactoryConfig.getArtifactoryUrl() + "] repoId ["
          + repoId + "] groupId [" + groupId + "] artifactId [" + artifactId + "]";
      logger.info("Fetching the {}", msg);
      String latestVersion = artifactory.searches()
                                 .artifactsLatestVersion()
                                 .groupId(groupId)
                                 .artifactId(artifactId)
                                 .repositories(repoId)
                                 .doRawSearch();
      if (StringUtils.isBlank(latestVersion)) {
        // Fetch all the versions
        logger.info("No latest release or integration version found for artifactory server ["
            + artifactoryConfig.getArtifactoryUrl() + "] repoId [" + repoId + "] groupId [" + groupId + "] artifactId ["
            + artifactId + "]");
        logger.info("Retrieving all versions to find the latest version ");
        BuildDetails buildDetails = getLatestSnapshotVersion(artifactory, repoId, groupId, artifactId);
        if (buildDetails == null || StringUtils.isBlank(buildDetails.getNumber())) {
          latestVersion = null;
        } else {
          return buildDetails;
        }
      }
      if (latestVersion == null || latestVersion.isEmpty()) {
        logger.error("Failed to {}", msg);
        throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", msg);
      }
      logger.info("Latest version {} found", latestVersion);
      return aBuildDetails().withNumber(latestVersion).withRevision(latestVersion).build();
    } catch (Exception e) {
      logger.error("Failed to fetch the latest version for url {}  " + artifactoryConfig.getArtifactoryUrl(), e);
      handleException(e);
    }
    return null;
  }

  private BuildDetails getLatestSnapshotVersion(
      Artifactory artifactory, String repoId, String groupId, String artifactId) {
    try {
      String apiVersionQuery = "api/search/versions/";
      ArtifactoryRequest versionRequest = new ArtifactoryRequestImpl()
                                              .apiUrl(apiVersionQuery)
                                              .method(GET)
                                              .responseType(JSON)
                                              .addQueryParam("g", groupId)
                                              .addQueryParam("a", artifactId)
                                              .addQueryParam("repos", repoId);
      LinkedHashMap<String, Object> response = artifactory.restCall(versionRequest);
      if (response != null) {
        List<LinkedHashMap<String, String>> results = (List<LinkedHashMap<String, String>>) response.get("results");
        for (LinkedHashMap<String, String> result : results) {
          String version = result.get("version");
          if (version == null) {
            return null;
          }
          if (version.contains("-")) {
            version = version.split("-")[0] + "-SNAPSHOT";
          }
          return aBuildDetails().withNumber(version).withRevision(version).build();
        }
      }
    } catch (Exception e) {
      logger.error("Failed to fetch the latest version for url {}  " + artifactory.getUri(), e);
      handleException(e);
    }
    return null;
  }

  @Override
  public List<String> getArtifactIds(ArtifactoryConfig artifactoryConfig, String repoKey, String groupId) {
    logger.info("Retrieving Artifact Ids from artifactory url {} repoKey {} groupId {}",
        artifactoryConfig.getArtifactoryUrl(), repoKey, groupId);
    List<String> artifactIds = new ArrayList<>();
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig);
    try {
      List<RepoPath> results =
          artifactory.searches().artifactsByGavc().groupId(groupId).repositories(repoKey).doSearch();
      Set<String> artifactIdSet = new HashSet<>();
      String groupIdPath = getGroupId(groupId);
      if (results == null) {
        return artifactIds;
      }
      for (RepoPath searchItem : results) {
        String itemPath = searchItem.getItemPath();
        // strip out the file
        String artifactId = itemPath.replace(groupIdPath.substring(1), "");
        // strip out the artifact id
        if (artifactId.contains("/")) {
          artifactId = artifactId.split("/")[0];
        }
        if (artifactIdSet.add(artifactId)) {
          artifactIds.add(artifactId);
        }
      }
    } catch (Exception e) {
      logger.error("Error occurred while Retrieving Artifact Ids from artifactory url {} repoKey {} groupId {} ",
          artifactoryConfig.getArtifactoryUrl(), repoKey, groupId, e);
      handleException(e);
    }
    logger.info("Retrieving Artifact Ids from artifactory url {} repoKey {} groupId {} success",
        artifactoryConfig.getArtifactoryUrl(), repoKey, groupId);
    return artifactIds;
  }

  @Override
  public ListNotifyResponseData downloadArtifacts(ArtifactoryConfig artifactoryConfig, String repoType, String groupId,
      List<String> artifactPaths, String artifactPattern, Map<String, String> metadata, String delegateId,
      String taskId, String accountId) {
    ListNotifyResponseData res = new ListNotifyResponseData();
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig);
    try {
      if (artifactPaths != null) {
        logger.info("Downloading artifacts from artifactory for maven type");
        for (String artifactId : artifactPaths) {
          Set<String> artifactNames = new HashSet<>();
          String latestVersion;
          if (metadata.get("buildNo") == null) {
            latestVersion = artifactory.searches()
                                .artifactsLatestVersion()
                                .groupId(groupId)
                                .artifactId(artifactId)
                                .repositories(repoType)
                                .doRawSearch();
          } else {
            latestVersion = metadata.get("buildNo");
          }
          if (latestVersion != null) {
            List<RepoPath> results = artifactory.searches()
                                         .artifactsByGavc()
                                         .groupId(groupId)
                                         .artifactId(artifactId)
                                         .version(latestVersion)
                                         .repositories(repoType)
                                         .doSearch();
            for (RepoPath searchItem : results) {
              String repoKey = searchItem.getRepoKey();
              String itemPath = searchItem.getItemPath();
              String artifactName = itemPath.substring(itemPath.lastIndexOf("/") + 1);
              try {
                if (artifactName.endsWith("pom")) {
                  continue;
                }
                if (artifactNames.add(artifactName)) {
                  logger.info("Downloading file {} ", searchItem.getItemPath());
                  InputStream inputStream = artifactory.repository(repoKey).download(itemPath).doDownload();
                  logger.info("Downloading file {} success", searchItem.getItemPath());
                  artifactCollectionTaskHelper.addDataToResponse(
                      new ImmutablePair<>(artifactName, inputStream), artifactId, res, delegateId, taskId, accountId);
                }
              } catch (Exception e) {
                logger.error("Failed to download the artifact from path {}", itemPath, e);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      String msg = "Failed to download the latest artifacts  of repo [" + repoType + "] groupId [" + groupId;
      throw new WingsException(
          ErrorCode.ARTIFACT_SERVER_ERROR, "message", msg + "Reason:" + ExceptionUtils.getRootCauseMessage(e));
    }
    return res;
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(ArtifactoryConfig artifactoryConfig, String repositoryPath) {
    return null;
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
        logger.info("Username is not set for artifactory config {} . Will use anonymous access.",
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
    return artifactoryConfig.getArtifactoryUrl().endsWith("/") ? artifactoryConfig.getArtifactoryUrl()
                                                               : artifactoryConfig.getArtifactoryUrl() + "/";
  }

  private String getGroupId(String path) {
    return "/" + path.replace(".", "/") + "/";
  }

  private void handleException(Exception e) {
    if (e instanceof IllegalArgumentException) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Invalid Artifactory credentials");
    }
    if (e instanceof HttpResponseException) {
      HttpResponseException httpResponseException = (HttpResponseException) e;
      if (httpResponseException.getStatusCode() == 401) {
        throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Invalid Artifactory credentials");
      } else if (httpResponseException.getStatusCode() == 403) {
        throw new WingsException(
            ErrorCode.INVALID_ARTIFACT_SERVER, "message", "User not authorized to access artifactory");
      }
    }
    throw new WingsException(
        ErrorCode.ARTIFACT_SERVER_ERROR, "message", "Reason:" + ExceptionUtils.getRootCauseMessage(e));
  }
}
