package software.wings.helpers.ext.artifactory;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.quietSleep;
import static java.lang.String.format;
import static java.time.Duration.ofMillis;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.jfrog.artifactory.client.ArtifactoryRequest.ContentType.JSON;
import static org.jfrog.artifactory.client.ArtifactoryRequest.ContentType.TEXT;
import static org.jfrog.artifactory.client.ArtifactoryRequest.Method.GET;
import static org.jfrog.artifactory.client.ArtifactoryRequest.Method.POST;
import static org.jfrog.artifactory.client.model.PackageType.docker;
import static org.jfrog.artifactory.client.model.PackageType.generic;
import static org.jfrog.artifactory.client.model.PackageType.maven;
import static org.jfrog.artifactory.client.model.PackageType.rpm;
import static org.jfrog.artifactory.client.model.PackageType.yum;
import static software.wings.beans.ErrorCode.ARTIFACT_SERVER_ERROR;
import static software.wings.beans.ErrorCode.INVALID_ARTIFACT_SERVER;
import static software.wings.common.Constants.ARTIFACT_FILE_NAME;
import static software.wings.common.Constants.ARTIFACT_PATH;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.exception.WingsException.ADMIN;
import static software.wings.exception.WingsException.USER;
import static software.wings.exception.WingsException.USER_ADMIN;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovyx.net.http.HttpResponseException;
import io.harness.network.Http;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.HttpHost;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClient.ProxyConfig;
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
import software.wings.common.AlphanumComparator;
import software.wings.delegatetasks.collect.artifacts.ArtifactCollectionTaskHelper;
import software.wings.exception.WingsException;
import software.wings.exception.WingsException.ReportTarget;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.ArtifactType;
import software.wings.waitnotify.ListNotifyResponseData;

import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Created by sgurubelli on 6/27/17.
 */
@Singleton
public class ArtifactoryServiceImpl implements ArtifactoryService {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactoryServiceImpl.class);

  @Inject private ArtifactCollectionTaskHelper artifactCollectionTaskHelper;
  @Inject private ExecutorService executorService;
  @Inject private EncryptionService encryptionService;
  @Inject private TimeLimiter timeLimiter;

  @Override
  public Map<String, String> getRepositories(
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails) {
    return getRepositories(artifactoryConfig, encryptionDetails, EnumSet.of(docker));
  }

  @Override
  public Map<String, String> getRepositories(
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails, ArtifactType artifactType) {
    switch (artifactType) {
      case DOCKER:
        return getRepositories(artifactoryConfig, encryptionDetails);
      case RPM:
        return getRepositories(artifactoryConfig, encryptionDetails, EnumSet.of(rpm, yum, maven, generic));
      default:
        return getRepositories(artifactoryConfig, encryptionDetails, EnumSet.of(maven, generic));
    }
  }

  @Override
  public Map<String, String> getRepositories(
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails, String packageType) {
    switch (packageType) {
      case "maven":
        return getRepositories(artifactoryConfig, encryptionDetails, EnumSet.of(maven));
      default:
        return getRepositories(artifactoryConfig, encryptionDetails, EnumSet.of(generic, rpm, maven, yum));
    }
  }

  private Map<String, String> getRepositories(ArtifactoryConfig artifactoryConfig,
      List<EncryptedDataDetail> encryptionDetails, EnumSet<PackageType> packageTypes) {
    logger.info("Retrieving repositories for packages {}", packageTypes.toArray());
    Map<String, String> repositories = new HashMap<>();
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig, encryptionDetails);
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
          // TODO : Get Settings api only works for Artifactory Pro
          repositories.put(repository.get("key").toString(), repository.get("key").toString());
        }
      }
      if (repositories.isEmpty()) {
        // Better way of handling Unauthorized access
        logger.info("Repositories are not available of package types {} or User not authorized to access artifactory",
            packageTypes);
      }
      logger.info("Retrieving repositories for packages {} success", packageTypes.toArray());
    } catch (RuntimeException e) {
      prepareException(e, USER_ADMIN);
    }
    return repositories;
  }

  @Override
  public List<String> getRepoPaths(
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails, String repoKey) {
    logger.info("Retrieving repo paths for repoKey {}", repoKey);
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig, encryptionDetails);
    PackageType packageType = null;
    try {
      Repository repository = artifactory.repository(repoKey).get();
      RepositorySettings settings = repository.getRepositorySettings();
      packageType = settings.getPackageType();
    } catch (Exception e) {
      logger.error("Error occurred while retrieving repository  from artifactory {} for Repo {}",
          artifactoryConfig.getArtifactoryUrl(), repoKey, e);
      prepareException(e, USER_ADMIN);
    }
    if (packageType.equals(docker)) {
      return listDockerImages(artifactory, repoKey);
    } else if (packageType.equals(maven)) {
      return getGroupIds(artifactory, repoKey);
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
        if (isEmpty(images)) {
          logger.info("No docker images from artifactory url {} and repo key {}", artifactory.getUri(), repoKey);
          images = new ArrayList<>();
        }
        logger.info("Retrieving images from artifactory url {} and repo key {} success", artifactory.getUri(), repoKey);
      }
    } catch (Exception e) {
      logger.error(
          format("Error occurred while listing docker images from artifactory %s for Repo %s", artifactory, repoKey),
          e);
      prepareException(e, USER_ADMIN);
    }
    return images;
  }

  @Override
  public List<BuildDetails> getBuilds(ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoKey, String imageName, int maxNumberOfBuilds) {
    logger.info("Retrieving docker tags for repoKey {} imageName {} ", repoKey, imageName);
    List<BuildDetails> buildDetails = new ArrayList<>();
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig, encryptionDetails);
    try {
      ArtifactoryRequest repositoryRequest = new ArtifactoryRequestImpl()
                                                 .apiUrl("api/docker/" + repoKey + "/v2/" + imageName + "/tags/list")
                                                 .method(GET)
                                                 .responseType(JSON);
      Map response = artifactory.restCall(repositoryRequest);
      if (response != null) {
        List<String> tags = (List<String>) response.get("tags");
        if (isEmpty(tags)) {
          logger.info("No  docker tags for repoKey {} imageName {} success ", repoKey, imageName);
          return buildDetails;
        }
        String tagUrl = getBaseUrl(artifactoryConfig) + repoKey + "/" + imageName + "/";
        buildDetails = tags.stream()
                           .map(tag -> aBuildDetails().withNumber(tag).withBuildUrl(tagUrl + tag).build())
                           .collect(toList());
      }
    } catch (Exception e) {
      prepareException(e, USER_ADMIN);
    }
    logger.info("Retrieving docker tags for repoKey {} imageName {} success ", repoKey, imageName);
    return buildDetails;
  }

  @Override
  public List<BuildDetails> getFilePaths(ArtifactoryConfig artifactoryConfig,
      List<EncryptedDataDetail> encryptionDetails, String repoKey, String artifactPath, String repositoryType,
      int maxVersions) {
    logger.info("Retrieving file paths for repoKey {} arthifactPath {}", repoKey, artifactPath);
    List<String> artifactPaths = new ArrayList<>();
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig, encryptionDetails);
    String artifactName;
    try {
      String aclQuery = "api/search/aql";
      String requestBody;
      if (isNotBlank(artifactPath)) {
        if (artifactPath.charAt(0) == '/') {
          artifactPath = artifactPath.substring(1);
        }
        String subPath;
        if (artifactPath.contains("/")) {
          String[] pathElems = artifactPath.split("/");
          subPath = getPath(Arrays.stream(pathElems).limit(pathElems.length - 1).collect(toList()));
          artifactName = pathElems[pathElems.length - 1];
          if (!artifactName.contains("?") && !artifactName.contains("*")) {
            artifactName = artifactName + "*";
          }
          requestBody = "items.find({\"repo\":\"" + repoKey + "\"}, {\"path\": {\"$match\":\"" + subPath
              + "\"}}, {\"name\": {\"$match\": \"" + artifactName + "\"}}).sort({\"$desc\" : [\"created\"]}).limit("
              + maxVersions + ")";
        } else {
          artifactPath = artifactPath + "*";
          requestBody = "items.find({\"repo\":\"" + repoKey + "\"}, {\"depth\": 1}, {\"name\": {\"$match\": \""
              + artifactPath + "\"}}).sort({\"$desc\" : [\"created\"]}).limit(" + maxVersions + ")";
        }
        ArtifactoryRequest repositoryRequest = new ArtifactoryRequestImpl()
                                                   .apiUrl(aclQuery)
                                                   .method(POST)
                                                   .requestBody(requestBody)
                                                   .requestType(TEXT)
                                                   .responseType(JSON);
        Map<String, List> response = artifactory.restCall(repositoryRequest);
        if (response != null) {
          List<Map<String, String>> results = response.get("results");
          if (results != null) {
            for (Map<String, String> result : results) {
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
        logger.info("Artifact paths order from Artifactory Server" + artifactPaths);
        Collections.reverse(artifactPaths);
        return artifactPaths.stream()
            .map(path
                -> aBuildDetails()
                       .withNumber(path.substring(path.lastIndexOf('/') + 1))
                       .withArtifactPath(path)
                       .withBuildUrl(getBaseUrl(artifactoryConfig) + path)
                       .build())
            .collect(toList());
      } else {
        throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", "Artifact path can not be empty");
      }
    } catch (Exception e) {
      if (e instanceof HttpResponseException) {
        HttpResponseException httpResponseException = (HttpResponseException) e;
        if (httpResponseException.getStatusCode() == 403) {
          logger.warn("User not authorized to perform deep level search. Trying with different search api");
          artifactPaths = getFilePathsForAnonymousUser(artifactory, repoKey, artifactPath, maxVersions);
          return artifactPaths.stream()
              .map(path
                  -> aBuildDetails()
                         .withNumber(path.substring(path.lastIndexOf('/') + 1))
                         .withArtifactPath(path)
                         .withBuildUrl(getBaseUrl(artifactoryConfig) + path)
                         .build())
              .collect(toList());
        }
      }
      logger.error("Error occurred while retrieving File Paths from Artifactory server {}",
          artifactoryConfig.getArtifactoryUrl(), e);
      prepareException(e, ADMIN);
    }
    return new ArrayList<>();
  }

  private List<String> getFilePathsForAnonymousUser(
      Artifactory artifactory, String repoKey, String artifactPath, int maxVersions) {
    logger.info("Retrieving file paths for repoKey {} arthifactPath {}", repoKey, artifactPath);
    List<String> artifactPaths = new ArrayList<>();

    List<FolderPath> folderPaths;
    try {
      if (isNotBlank(artifactPath)) {
        if (artifactPath.charAt(0) == '/') {
          artifactPath = artifactPath.substring(1);
        }
        if (artifactPath.endsWith("/")) {
          artifactPath = artifactPath.substring(0, artifactPath.length() - 1);
        }
        Pattern pattern = Pattern.compile(artifactPath.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));
        if (artifactPath.contains("/")) {
          String[] pathElems = artifactPath.split("/");
          String subPath = getPath(Arrays.stream(pathElems).limit(pathElems.length - 1).collect(toList()));
          if (!subPath.contains("?") && !subPath.contains("*")) {
            folderPaths = getFolderPaths(artifactory, repoKey, "/" + subPath);
            if (folderPaths != null) {
              for (FolderPath folderPath : folderPaths) {
                if (!folderPath.isFolder()) {
                  if (pattern.matcher(folderPath.getPath().substring(1) + folderPath.getUri()).find()) {
                    artifactPaths.add(repoKey + folderPath.getPath() + folderPath.getUri());
                  }
                }
              }
            }
          } else {
            String artifactName = pathElems[pathElems.length - 1];
            List<RepoPath> repoPaths =
                artifactory.searches().artifactsByName(artifactName).repositories(repoKey).doSearch();
            if (repoPaths != null) {
              for (RepoPath repoPath : repoPaths) {
                if (pattern.matcher(repoPath.getItemPath()).find()) {
                  artifactPaths.add(repoKey + "/" + repoPath.getItemPath());
                }
              }
            }
          }
        } else {
          folderPaths = getFolderPaths(artifactory, repoKey, "");
          if (folderPaths != null) {
            for (FolderPath folderPath : folderPaths) {
              if (!folderPath.isFolder()) {
                if (pattern.matcher(folderPath.getUri()).find()) {
                  artifactPaths.add(repoKey + folderPath.getUri());
                }
              }
            }
          }
        }
        // Sort the alphanumeric order
        artifactPaths = artifactPaths.stream().sorted(new AlphanumComparator()).collect(toList());
        Collections.reverse(artifactPaths);
        artifactPaths = artifactPaths.stream().limit(maxVersions).collect(toList());
        Collections.reverse(artifactPaths);
      } else {
        throw new WingsException(INVALID_ARTIFACT_SERVER).addParam("message", "Artifact path can not be empty");
      }
      logger.info("Artifact paths order from Artifactory Server" + artifactPaths);
      return artifactPaths;
    } catch (Exception e) {
      logger.error("Error occurred while retrieving File Paths from Artifactory server {}", artifactory.getUri(), e);
      prepareException(e, ADMIN);
    }
    return new ArrayList<>();
  }

  @SuppressFBWarnings("BC_IMPOSSIBLE_INSTANCEOF")
  public List<String> getGroupIds(Artifactory artifactory, String repoKey) {
    logger.info("Retrieving groupIds for repoKey {}", repoKey);
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
      Map<String, Object> response = artifactory.restCall(repositoryRequest);
      Set<String> groupIds = new HashSet<>();
      if (response != null) {
        List<Map<String, String>> files = (List<Map<String, String>>) response.get("files");
        if (files != null) {
          for (Map<String, String> file : files) {
            String uri = file.get("uri");
            if (uri != null) {
              // strip out the file
              String[] pathElems = uri.substring(1).split("/");
              if (pathElems.length >= 4) {
                String groupId = getGroupId(Arrays.stream(pathElems).limit(pathElems.length - 3).collect(toList()));
                if (groupIds.add(groupId)) {
                  groupIdList.add(groupId);
                }
              } else {
                logger.info("Ignoring uri {} as it is not valid GAVC format", uri);
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
      prepareException(e, USER);
    }
    logger.info("Retrieving groupIds for repoKey {} success", repoKey);
    return groupIdList;
  }

  /**
   * Listing groupIds by recursively for non authenticated users
   *
   * @param artifactory
   * @param repoKey
   * @return
   */
  private List<String> listGroupIds(Artifactory artifactory, String repoKey) {
    logger.info("Retrieving groupIds with anonymous user access");
    List<String> paths = getGroupIdPathsAsync(artifactory, repoKey);
    logger.info("Retrieved unique groupIds size {}", paths == null ? 0 : paths.size());
    return paths;
  }

  private List<String> getGroupIdPathsAsync(Artifactory artifactory, String repoKey) {
    Set<String> groupIds = new HashSet<>();
    try {
      return timeLimiter.callWithTimeout(() -> {
        Queue<Future> futures = new ConcurrentLinkedQueue<>();
        Stack<FolderPath> paths = new Stack<>();
        paths.addAll(getFolderPaths(artifactory, repoKey, ""));
        while (isNotEmpty(paths) || isNotEmpty(futures)) {
          while (isNotEmpty(paths)) {
            FolderPath folderPath = paths.pop();
            String path = folderPath.getPath();
            if (folderPath.isFolder()) {
              traverseInParallel(artifactory, repoKey, futures, paths, folderPath, path);
            } else {
              // Strip out the version
              String[] pathElems = path.substring(1).split("/");
              if (pathElems.length >= 3) {
                groupIds.add(getGroupId(Arrays.stream(pathElems).limit(pathElems.length - 2).collect(toList())));
              }
            }
          }
          while (isNotEmpty(futures) && futures.peek().isDone()) {
            futures.poll().get();
          }
          quietSleep(ofMillis(10)); // avoid busy wait
        }
        return new ArrayList<>(groupIds);
      }, 20L, TimeUnit.SECONDS, true);
    } catch (UncheckedTimeoutException e) {
      logger.warn("Failed to fetch all groupIds within 20 secs. Returning all groupIds collected so far", e);
    } catch (Exception e) {
      logger.warn("Error fetching all groupIds. Returning all groupIds collected so far", e);
    }
    return new ArrayList<>(groupIds);
  }

  private void traverseInParallel(Artifactory artifactory, String repoKey, Queue<Future> futures,
      List<FolderPath> paths, FolderPath folderPath, String path) {
    futures.add(
        executorService.submit(() -> paths.addAll(getFolderPaths(artifactory, repoKey, path + folderPath.getUri()))));
  }

  private String getGroupId(List<String> pathElems) {
    StringBuilder groupIdBuilder = new StringBuilder();
    for (int i = 0; i < pathElems.size(); i++) {
      groupIdBuilder.append(pathElems.get(i));
      if (i != pathElems.size() - 1) {
        groupIdBuilder.append('.');
      }
    }
    return groupIdBuilder.toString();
  }

  private String getPath(List<String> pathElems) {
    StringBuilder groupIdBuilder = new StringBuilder();
    for (int i = 0; i < pathElems.size(); i++) {
      groupIdBuilder.append(pathElems.get(i));
      if (i != pathElems.size() - 1) {
        groupIdBuilder.append('/');
      }
    }
    return groupIdBuilder.toString();
  }

  private List<FolderPath> getFolderPaths(Artifactory artifactory, String repoKey, String repoPath) {
    // Add first level paths
    List<FolderPath> folderPaths = new ArrayList<>();
    try {
      String apiStorageQuery = "api/storage/";
      apiStorageQuery = apiStorageQuery + repoKey + repoPath;
      ArtifactoryRequest repositoryRequest =
          new ArtifactoryRequestImpl().apiUrl(apiStorageQuery).method(GET).responseType(JSON);
      LinkedHashMap<String, Object> response = artifactory.restCall(repositoryRequest);
      if (response == null) {
        return folderPaths;
      }
      List<LinkedHashMap<String, Object>> results = (List<LinkedHashMap<String, Object>>) response.get("children");
      if (isEmpty(results)) {
        return folderPaths;
      }
      for (LinkedHashMap<String, Object> result : results) {
        folderPaths.add(FolderPath.builder()
                            .path((String) response.get("path"))
                            .uri((String) result.get("uri"))
                            .folder((boolean) result.get("folder"))
                            .build());
      }
    } catch (Exception e) {
      logger.error("Exception occurred in retrieving folder paths", e);
    }
    return folderPaths;
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
  public BuildDetails getLatestVersion(ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoId, String groupId, String artifactId) {
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig, encryptionDetails);
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
      if (isBlank(latestVersion)) {
        // Fetch all the versions
        logger.info("No latest release or integration version found for artifactory server ["
            + artifactoryConfig.getArtifactoryUrl() + "] repoId [" + repoId + "] groupId [" + groupId + "] artifactId ["
            + artifactId + "]");
        logger.info("Retrieving all versions to find the latest version ");
        BuildDetails buildDetails = getLatestSnapshotVersion(artifactory, repoId, groupId, artifactId);
        if (buildDetails == null || isBlank(buildDetails.getNumber())) {
          latestVersion = null;
        } else {
          logger.info("Latest integration version {} found", buildDetails.getNumber());
          return buildDetails;
        }
      }
      if (isEmpty(latestVersion)) {
        logger.error("Failed to {}", msg);
        return null;
      }
      logger.info("Latest version {} found", latestVersion);
      return aBuildDetails().withNumber(latestVersion).withRevision(latestVersion).build();
    } catch (Exception e) {
      prepareException(e, ADMIN);
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
      Map<String, Object> response = artifactory.restCall(versionRequest);
      if (response != null) {
        List<Map<String, String>> results = (List<Map<String, String>>) response.get("results");
        for (Map<String, String> result : results) {
          String version = result.get("version");
          logger.info("Latest version {}", version);
          if (version == null) {
            return null;
          }
          if (version.contains("-")) {
            version = version.split("-")[0] + "-SNAPSHOT";
          }
          logger.info("Appending snapshot to the latest version ");
          return aBuildDetails().withNumber(version).withRevision(version).build();
        }
      }
    } catch (Exception e) {
      throw new WingsException(
          format("Failed to fetch the latest snap version for url %s repoId %s groupId %s and artifactId %s",
              artifactory.getUri(), repoId, groupId, artifactId));
    }
    return null;
  }

  @Override
  public List<String> getArtifactIds(ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoKey, String groupId) {
    logger.info("Retrieving Artifact Ids from artifactory url {} repoKey {} groupId {}",
        artifactoryConfig.getArtifactoryUrl(), repoKey, groupId);
    List<String> artifactIds = new ArrayList<>();
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig, encryptionDetails);
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
      prepareException(e, USER);
    }
    logger.info("Retrieving Artifact Ids from artifactory url {} repoKey {} groupId {} success",
        artifactoryConfig.getArtifactoryUrl(), repoKey, groupId);
    return artifactIds;
  }

  @SuppressFBWarnings("REC_CATCH_EXCEPTION")
  @Override
  public ListNotifyResponseData downloadArtifacts(ArtifactoryConfig artifactoryConfig,
      List<EncryptedDataDetail> encryptionDetails, String repoType, String groupId, List<String> artifactPaths,
      String artifactPattern, Map<String, String> metadata, String delegateId, String taskId, String accountId) {
    ListNotifyResponseData res = new ListNotifyResponseData();
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig, encryptionDetails);
    try {
      if (metadata.get(ARTIFACT_PATH) != null) {
        logger.info("Downloading the file for generic repo");
        return downloadArtifacts(
            artifactoryConfig, encryptionDetails, repoType, metadata, delegateId, taskId, accountId);
      }
      if (artifactPattern == null) {
        throw new WingsException(ARTIFACT_SERVER_ERROR).addParam("message", "Artifact pattern is Empty");
      }
      logger.info("Downloading artifact file maven repo style");
      Pattern pattern = Pattern.compile(artifactPattern.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));
      String[] paths = artifactPattern.split("/");
      groupId = getGroupId(Arrays.stream(paths).limit(paths.length - 3).collect(toList()));
      String artifactId = paths[paths.length - 3];
      Set<String> artifactNames = new HashSet<>();
      String latestVersion;
      if (metadata.get(BUILD_NO) == null) {
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
        logger.info("Downloading artifacts from artifactory for repoType {}, groupId {}, artifactId {} and version {}",
            repoType, groupId, artifactId, latestVersion);
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
          String artifactName = itemPath.substring(itemPath.lastIndexOf('/') + 1);
          try {
            if (pattern.matcher(itemPath).find()) {
              logger.info("Artifact name {}", artifactName);
              if (artifactName.endsWith("pom") || artifactName.equals("maven-metadata.xml")) {
                continue;
              }

              if (artifactNames.add(artifactName)) {
                logger.info("Downloading file {} ", searchItem.getItemPath());
                InputStream inputStream = artifactory.repository(repoKey).download(itemPath).doDownload();
                logger.info("Downloading file {} success", searchItem.getItemPath());
                artifactCollectionTaskHelper.addDataToResponse(
                    new ImmutablePair<>(artifactName, inputStream), itemPath, res, delegateId, taskId, accountId);
              }
            } else {
              logger.info("Repo path {} not matching with the artifact pattern {}", itemPath, artifactPattern);
            }

          } catch (Exception e) {
            logger.error("Failed to download the artifact from path {}", itemPath, e);
          }
        }
        logger.info(
            "Downloading artifacts from artifactory for repoType {}, groupId {}, artifactId {} and version {} success",
            repoType, groupId, artifactId, latestVersion);
      } else {
        logger.error("No version found so not collecting any artifacts");
      }
    } catch (Exception e) {
      String msg = "Failed to download the latest artifacts  of repo [" + repoType + "] groupId [" + groupId;
      prepareAndThrowException(msg + "Reason:" + ExceptionUtils.getRootCauseMessage(e), ADMIN, e);
    }
    return res;
  }

  @Override
  public boolean validateArtifactPath(ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoType, String artifactPath, String repositoryType) {
    logger.info(
        "Validating artifact path {} for repository {} and repositoryType {}", artifactPath, repoType, repositoryType);
    if (isBlank(artifactPath)) {
      throw new WingsException(ARTIFACT_SERVER_ERROR, USER).addParam("message", "Artifact Pattern  can not be empty");
    }
    List<BuildDetails> filePaths = null;
    if (!repositoryType.equals(maven.name())) {
      try {
        filePaths = getFilePaths(artifactoryConfig, encryptionDetails, repoType, artifactPath, repositoryType, 1);
      } catch (Exception e) {
        prepareAndThrowException("Invalid artifact path", USER, null);
      }
      if (isEmpty(filePaths)) {
        prepareAndThrowException(
            "No artifact files matching with the artifact path [" + artifactPath + "]", USER, null);
      }
      logger.info("Validating whether directory exists or not for Generic repository type by fetching file paths");
    } else {
      // First get the groupId
      String[] artifactPaths = artifactPath.split("/");
      if (isEmpty(artifactPaths)) {
        prepareAndThrowException("Invalid artifact path", USER, null);
      }
      if (artifactPaths.length < 4) {
        prepareAndThrowException(
            "Not in maven style format. Sample format: com/mycompany/myservice/*/myservice*.war", USER, null);
        throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
            .addParam("message", "Not in maven style format. Sample format: com/mycompany/myservice/*/myservice*.war");
      }
      String groupId = getGroupId(Arrays.stream(artifactPaths).limit(artifactPaths.length - 3).collect(toList()));
      String artifactId = artifactPaths[artifactPaths.length - 3];
      if (groupId.contains("*") || groupId.contains("?")) {
        prepareAndThrowException(
            "GroupId path can not contain wild chars. Sample format. e.g. com/mycompany/myservice/*/myservice*.war",
            USER, null);
      }
      if (artifactId.contains("*") || artifactId.contains("?")) {
        prepareAndThrowException(
            "Artifact Id can not contain wild chars. Sample format. e.g. com/mycompany/myservice/*/myservice*.war",
            USER, null);
      }
      try {
        logger.info("Validating maven style repo by fetching the version");
        BuildDetails buildDetails =
            getLatestVersion(artifactoryConfig, encryptionDetails, repoType, groupId, artifactId);
        logger.info("Validation success. Version {}", buildDetails.getNumber());
      } catch (Exception e) {
        prepareAndThrowException(
            "Invalid artifact path. Please verify that artifact published as maven standard", ADMIN, e);
      }
    }
    return true;
  }

  private void prepareAndThrowException(String message, ReportTarget[] reportTargets, Exception e) {
    throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, reportTargets, e).addParam("message", message);
  }

  private ListNotifyResponseData downloadArtifacts(ArtifactoryConfig artifactoryConfig,
      List<EncryptedDataDetail> encryptionDetails, String repoKey, Map<String, String> metadata, String delegateId,
      String taskId, String accountId) {
    ListNotifyResponseData res = new ListNotifyResponseData();
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig, encryptionDetails);
    Set<String> artifactNames = new HashSet<>();
    String artifactPath = metadata.get(ARTIFACT_PATH).replace(repoKey, "").substring(1);
    String artifactName = metadata.get(ARTIFACT_FILE_NAME);

    try {
      logger.info("Artifact name {}", artifactName);
      if (artifactNames.add(artifactName)) {
        logger.info("Downloading file {} ", artifactPath);
        InputStream inputStream = artifactory.repository(repoKey).download(artifactPath).doDownload();
        logger.info("Downloading file {} success", artifactPath);
        artifactCollectionTaskHelper.addDataToResponse(
            new ImmutablePair<>(artifactName, inputStream), artifactPath, res, delegateId, taskId, accountId);
      }
    } catch (Exception e) {
      String msg =
          "Failed to download the latest artifacts  of repository [" + repoKey + "] file path [" + artifactPath;
      throw new WingsException(ARTIFACT_SERVER_ERROR, ADMIN)
          .addParam("message", msg + "Reason:" + ExceptionUtils.getRootCauseMessage(e));
    }
    logger.info(
        "Downloading artifacts from artifactory for repository  {} and file path {} success", repoKey, artifactPath);
    return res;
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails, String repositoryPath) {
    return null;
  }

  /**
   * Get Artifactory Client
   *
   * @param artifactoryConfig
   * @return Artifactory returns artifactory client
   */

  private Artifactory getArtifactoryClient(
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails) {
    encryptionService.decrypt(artifactoryConfig, encryptionDetails);
    try {
      ArtifactoryClientBuilder builder = ArtifactoryClientBuilder.create();
      builder.setUrl(getBaseUrl(artifactoryConfig));
      if (isBlank(artifactoryConfig.getUsername())) {
        logger.info("Username is not set for artifactory config {} . Will use anonymous access.",
            artifactoryConfig.getArtifactoryUrl());
      } else if (artifactoryConfig.getPassword() == null || isBlank(new String(artifactoryConfig.getPassword()))) {
        logger.info("Username is set. However no password set for artifactory config {}",
            artifactoryConfig.getArtifactoryUrl());
        builder.setUsername(artifactoryConfig.getUsername());
      } else {
        builder.setUsername(artifactoryConfig.getUsername());
        builder.setPassword(new String(artifactoryConfig.getPassword()));
      }
      HttpHost httpProxyHost = Http.getHttpProxyHost(artifactoryConfig.getArtifactoryUrl());
      if (httpProxyHost != null) {
        builder.setProxy(new ProxyConfig(httpProxyHost.getHostName(), httpProxyHost.getPort(), null, null, null));
      }
      builder.setSocketTimeout(15000);
      builder.setConnectionTimeout(15000);
      return builder.build();
    } catch (Exception ex) {
      prepareException(ex, USER_ADMIN);
    }
    return null;
  }

  private String getBaseUrl(ArtifactoryConfig artifactoryConfig) {
    return artifactoryConfig.getArtifactoryUrl().endsWith("/") ? artifactoryConfig.getArtifactoryUrl()
                                                               : artifactoryConfig.getArtifactoryUrl() + "/";
  }

  private String getGroupId(String path) {
    return "/" + path.replace(".", "/") + "/";
  }

  private void prepareException(Exception e, ReportTarget[] reportTargets) throws WingsException {
    if (e instanceof HttpResponseException) {
      HttpResponseException httpResponseException = (HttpResponseException) e;
      if (httpResponseException.getStatusCode() == 401) {
        throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, reportTargets, e)
            .addParam("message", "Invalid Artifactory credentials");
      } else if (httpResponseException.getStatusCode() == 403) {
        throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, reportTargets, e)
            .addParam("message", "User not authorized to access artifactory");
      }
    }
    if (e instanceof SocketTimeoutException) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, reportTargets, e)
          .addParam("message",
              e.getMessage() + "."
                  + " Artifactory server may not be running");
    }
    if (e instanceof WingsException) {
      throw(WingsException) e;
    }
    throw new WingsException(ARTIFACT_SERVER_ERROR, reportTargets, e).addParam("message", e.getMessage());
  }
}
