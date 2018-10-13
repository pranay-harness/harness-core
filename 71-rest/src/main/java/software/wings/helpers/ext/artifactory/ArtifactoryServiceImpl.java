package software.wings.helpers.ext.artifactory;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.ARTIFACT_SERVER_ERROR;
import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static io.harness.exception.WingsException.ADMIN;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_ADMIN;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.jfrog.artifactory.client.ArtifactoryRequest.ContentType.JSON;
import static org.jfrog.artifactory.client.ArtifactoryRequest.ContentType.TEXT;
import static org.jfrog.artifactory.client.ArtifactoryRequest.Method.GET;
import static org.jfrog.artifactory.client.ArtifactoryRequest.Method.POST;
import static org.jfrog.artifactory.client.model.PackageType.docker;
import static software.wings.common.Constants.ARTIFACT_FILE_NAME;
import static software.wings.common.Constants.ARTIFACT_PATH;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovyx.net.http.HttpResponseException;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.exception.WingsException.ReportTarget;
import io.harness.network.Http;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.common.AlphanumComparator;
import software.wings.common.Constants;
import software.wings.delegatetasks.collect.artifacts.ArtifactCollectionTaskHelper;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.ArtifactType;
import software.wings.utils.Misc;
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
import java.util.Set;
import java.util.regex.Pattern;

@Singleton
public class ArtifactoryServiceImpl implements ArtifactoryService {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactoryServiceImpl.class);

  @Inject private ArtifactCollectionTaskHelper artifactCollectionTaskHelper;
  @Inject private EncryptionService encryptionService;

  @Override
  public Map<String, String> getRepositories(
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails) {
    return getRepositories(artifactoryConfig, encryptionDetails, Arrays.asList(PackageType.docker));
  }

  @Override
  public Map<String, String> getRepositories(
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails, ArtifactType artifactType) {
    switch (artifactType) {
      case DOCKER:
        return getRepositories(artifactoryConfig, encryptionDetails);
      default:
        return getRepositories(artifactoryConfig, encryptionDetails, "");
    }
  }

  @Override
  public Map<String, String> getRepositories(
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails, String packageType) {
    switch (packageType) {
      case "maven":
        return getRepositories(artifactoryConfig, encryptionDetails, Arrays.asList(PackageType.maven));
      default:
        return getRepositories(artifactoryConfig, encryptionDetails,
            Arrays.asList(PackageType.values()).stream().filter(type -> !docker.equals(type)).collect(toList()));
    }
  }

  private Map<String, String> getRepositories(ArtifactoryConfig artifactoryConfig,
      List<EncryptedDataDetail> encryptionDetails, List<PackageType> packageTypes) {
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
          logger.warn("Failed to get repository settings for repo {}, Reason {}", repoKey, Misc.getMessage(e));
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
      logger.error("Error occurred while retrieving repositories", e);
      prepareException(e, USER_ADMIN);
    }
    return repositories;
  }

  @Override
  public List<String> getRepoPaths(
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails, String repoKey) {
    return listDockerImages(getArtifactoryClient(artifactoryConfig, encryptionDetails), repoKey);
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
        logger.info("Retrieving images from artifactory url {} and repo key {} success. Images {}",
            artifactory.getUri(), repoKey, images);
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
        logger.info(
            "Retrieving docker tags for repoKey {} imageName {} success. Retrieved tags {}", repoKey, imageName, tags);
      }

    } catch (Exception e) {
      logger.info("Exception occurred while retrieving the docker docker tags for Image {}", imageName);
      prepareException(e, USER_ADMIN);
    }

    return buildDetails;
  }

  @Override
  public List<BuildDetails> getFilePaths(ArtifactoryConfig artifactoryConfig,
      List<EncryptedDataDetail> encryptionDetails, String repositoryName, String artifactPath, String repositoryType,
      int maxVersions) {
    logger.info("Retrieving file paths for repositoryName {} artifactPath {}", repositoryName, artifactPath);
    List<String> artifactPaths = new ArrayList<>();
    LinkedHashMap<String, String> map = new LinkedHashMap<>();
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
          requestBody = "items.find({\"repo\":\"" + repositoryName + "\"}, {\"path\": {\"$match\":\"" + subPath
              + "\"}}, {\"name\": {\"$match\": \"" + artifactName + "\"}}).sort({\"$desc\" : [\"created\"]}).limit("
              + maxVersions + ")";
        } else {
          artifactPath = artifactPath + "*";
          requestBody = "items.find({\"repo\":\"" + repositoryName + "\"}, {\"depth\": 1}, {\"name\": {\"$match\": \""
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
                String size = String.valueOf(result.get("size"));
                if (path != null && !path.equals(".")) {
                  artifactPaths.add(repositoryName + "/" + path + "/" + name);
                  map.put(repositoryName + "/" + path + "/" + name, size);
                } else {
                  artifactPaths.add(repositoryName + "/" + name);
                  map.put(repositoryName + "/" + name, size);
                }
              }
            }
          }
        }
        logger.info("Artifact paths order from Artifactory Server" + artifactPaths);
        Collections.reverse(artifactPaths);
        String finalArtifactPath = artifactPath;
        return artifactPaths.stream()
            .map(path
                -> aBuildDetails()
                       .withNumber(constructBuildNumber(finalArtifactPath, path.substring(path.indexOf('/') + 1)))
                       .withArtifactPath(path)
                       .withBuildUrl(getBaseUrl(artifactoryConfig) + path)
                       .withArtifactFileSize(map.get(path))
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
          artifactPaths = getFilePathsForAnonymousUser(artifactory, repositoryName, artifactPath, maxVersions);
          String finalArtifactPath = artifactPath;
          return artifactPaths.stream()
              .map(path
                  -> aBuildDetails()
                         .withNumber(constructBuildNumber(finalArtifactPath, path.substring(path.indexOf('/') + 1)))
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

  private String constructBuildNumber(String artifactPattern, String path) {
    String[] tokens = artifactPattern.split("/");
    for (String token : tokens) {
      if (token.contains("*") || token.contains("+")) {
        return path.substring(artifactPattern.indexOf(token));
      }
    }
    return path;
  }

  private List<String> getFilePathsForAnonymousUser(
      Artifactory artifactory, String repoKey, String artifactPath, int maxVersions) {
    logger.info("Retrieving file paths for repoKey {} artifactPath {}", repoKey, artifactPath);
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
      logger.error(
          format("Error occurred while retrieving File Paths from Artifactory server %s", artifactory.getUri()), e);
      prepareException(e, ADMIN);
    }
    return new ArrayList<>();
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

  @SuppressFBWarnings("REC_CATCH_EXCEPTION")
  @Override
  public ListNotifyResponseData downloadArtifacts(ArtifactoryConfig artifactoryConfig,
      List<EncryptedDataDetail> encryptionDetails, String repositoryName, Map<String, String> metadata,
      String delegateId, String taskId, String accountId) {
    ListNotifyResponseData res = new ListNotifyResponseData();
    String artifactPath = metadata.get(ARTIFACT_PATH).replaceFirst(repositoryName, "").substring(1);
    String artifactName = metadata.get(ARTIFACT_FILE_NAME);
    try {
      logger.info("Downloading the file for generic repo");
      InputStream inputStream = downloadArtifacts(artifactoryConfig, encryptionDetails, repositoryName, metadata);
      artifactCollectionTaskHelper.addDataToResponse(
          new ImmutablePair<>(artifactName, inputStream), artifactPath, res, delegateId, taskId, accountId);
      return res;
    } catch (Exception e) {
      String msg =
          "Failed to download the latest artifacts  of repo [" + repositoryName + "] artifactPath [" + artifactPath;
      prepareAndThrowException(msg + "Reason:" + ExceptionUtils.getRootCauseMessage(e), ADMIN, e);
    }
    return res;
  }

  @SuppressFBWarnings("REC_CATCH_EXCEPTION")
  @Override
  public Pair<String, InputStream> downloadArtifact(ArtifactoryConfig artifactoryConfig,
      List<EncryptedDataDetail> encryptionDetails, String repositoryName, Map<String, String> metadata) {
    Pair<String, InputStream> pair = null;
    String artifactPath = metadata.get(ARTIFACT_PATH).replaceFirst(repositoryName, "").substring(1);
    String artifactName = metadata.get(ARTIFACT_FILE_NAME);
    try {
      logger.info("Downloading the file for generic repo");
      InputStream inputStream = downloadArtifacts(artifactoryConfig, encryptionDetails, repositoryName, metadata);
      pair = new ImmutablePair<>(artifactName, inputStream);
    } catch (Exception e) {
      String msg =
          "Failed to download the latest artifacts  of repo [" + repositoryName + "] artifactPath [" + artifactPath;
      prepareAndThrowException(msg + "Reason:" + ExceptionUtils.getRootCauseMessage(e), ADMIN, e);
    }
    return pair;
  }

  @Override
  public boolean validateArtifactPath(ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails,
      String repositoryName, String artifactPath, String repositoryType) {
    logger.info("Validating artifact path {} for repository {} and repositoryType {}", artifactPath, repositoryName,
        repositoryType);
    if (isBlank(artifactPath)) {
      throw new WingsException(ARTIFACT_SERVER_ERROR, USER).addParam("message", "Artifact Pattern  can not be empty");
    }
    List<BuildDetails> filePaths = null;

    try {
      filePaths = getFilePaths(artifactoryConfig, encryptionDetails, repositoryName, artifactPath, repositoryType, 1);
    } catch (Exception e) {
      prepareAndThrowException("Invalid artifact path", USER, null);
    }
    if (isEmpty(filePaths)) {
      prepareAndThrowException("No artifact files matching with the artifact path [" + artifactPath + "]", USER, null);
    }
    logger.info("Validating whether directory exists or not for Generic repository type by fetching file paths");
    return true;
  }

  private void prepareAndThrowException(String message, EnumSet<ReportTarget> reportTargets, Exception e) {
    throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, reportTargets, e).addParam("message", message);
  }

  private InputStream downloadArtifacts(ArtifactoryConfig artifactoryConfig,
      List<EncryptedDataDetail> encryptionDetails, String repoKey, Map<String, String> metadata) {
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig, encryptionDetails);
    Set<String> artifactNames = new HashSet<>();
    String artifactPath = metadata.get(ARTIFACT_PATH).replaceFirst(repoKey, "").substring(1);
    String artifactName = metadata.get(ARTIFACT_FILE_NAME);

    try {
      logger.info("Artifact name {}", artifactName);
      if (artifactNames.add(artifactName)) {
        if (metadata.get(ARTIFACT_PATH) != null) {
          logger.info("Downloading the file for generic repo");
          logger.info("Downloading file {} ", artifactPath);
          InputStream inputStream = artifactory.repository(repoKey).download(artifactPath).doDownload();
          logger.info("Downloading file {} success", artifactPath);
          return inputStream;
        }
      }
    } catch (Exception e) {
      logger.error("Failed to download the artifact of repository {} from path {}", repoKey, artifactPath, e);
      String msg =
          "Failed to download the latest artifacts  of repository [" + repoKey + "] file path [" + artifactPath;
      throw new WingsException(ARTIFACT_SERVER_ERROR, ADMIN)
          .addParam("message", msg + "Reason:" + ExceptionUtils.getRootCauseMessage(e));
    }
    logger.info(
        "Downloading artifacts from artifactory for repository  {} and file path {} success", repoKey, artifactPath);
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
        builder.setProxy(new ProxyConfig(httpProxyHost.getHostName(), httpProxyHost.getPort(), Http.getProxyScheme(),
            Http.getProxyUserName(), Http.getProxyPassword()));
      }
      builder.setSocketTimeout(30000);
      builder.setConnectionTimeout(30000);
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

  private void prepareException(Exception e, EnumSet<ReportTarget> reportTargets) throws WingsException {
    if (e instanceof HttpResponseException) {
      HttpResponseException httpResponseException = (HttpResponseException) e;
      if (httpResponseException.getStatusCode() == 401) {
        throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, reportTargets)
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
                  + "SocketTimeout: Artifactory server may not be running");
    }
    if (e instanceof WingsException) {
      throw(WingsException) e;
    }
    throw new WingsException(ARTIFACT_SERVER_ERROR, reportTargets).addParam("message", Misc.getMessage(e));
  }

  public Long getFileSize(
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails, Map<String, String> metadata) {
    String artifactPath = metadata.get(Constants.ARTIFACT_PATH);
    logger.info("Retrieving file paths for artifactPath {}", artifactPath);
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig, encryptionDetails);
    try {
      String apiStorageQuery = "api/storage/" + artifactPath;

      ArtifactoryRequest repositoryRequest =
          new ArtifactoryRequestImpl().apiUrl(apiStorageQuery).method(GET).requestType(TEXT).responseType(JSON);
      LinkedHashMap<String, String> response = artifactory.restCall(repositoryRequest);
      if (response != null) {
        return Long.valueOf(response.get("size"));
      } else {
        throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
            .addParam("message", "Unable to get artifact file size ");
      }
    } catch (Exception e) {
      logger.error("Error occurred while retrieving File Paths from Artifactory server {}",
          artifactoryConfig.getArtifactoryUrl(), e);
      prepareException(e, ADMIN);
    }
    return 0L;
  }
}
