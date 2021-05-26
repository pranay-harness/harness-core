package software.wings.helpers.ext.artifactory;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.ARTIFACT_SERVER_ERROR;
import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static io.harness.exception.WingsException.USER;

import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.jfrog.artifactory.client.ArtifactoryRequest.ContentType.JSON;
import static org.jfrog.artifactory.client.ArtifactoryRequest.ContentType.TEXT;
import static org.jfrog.artifactory.client.ArtifactoryRequest.Method.GET;
import static org.jfrog.artifactory.client.ArtifactoryRequest.Method.POST;
import static org.jfrog.artifactory.client.model.impl.PackageTypeImpl.docker;
import static org.jfrog.artifactory.client.model.impl.PackageTypeImpl.maven;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifact.ArtifactUtilities;
import io.harness.delegate.task.ListNotifyResponseData;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ArtifactoryServerException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.exception.WingsException.ReportTarget;
import io.harness.network.Http;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.common.AlphanumComparator;
import software.wings.common.BuildDetailsComparatorAscending;
import software.wings.delegatetasks.collect.artifacts.ArtifactCollectionTaskHelper;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.ArtifactType;
import software.wings.utils.RepositoryType;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpResponseException;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.ArtifactoryRequest;
import org.jfrog.artifactory.client.ArtifactoryResponse;
import org.jfrog.artifactory.client.ProxyConfig;
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl;
import org.jfrog.artifactory.client.model.RepoPath;
import org.jfrog.artifactory.client.model.Repository;
import org.jfrog.artifactory.client.model.impl.PackageTypeImpl;
import org.jfrog.artifactory.client.model.repository.settings.RepositorySettings;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class ArtifactoryServiceImpl implements ArtifactoryService {
  private static final String REASON = "Reason:";
  private static final String CREATED_BY = "created_by";
  private static final String SYSTEM = "_system_";
  private static final String RESULTS = "results";
  private static final String KEY = "key";

  private static final String DOWNLOAD_FILE_FOR_GENERIC_REPO = "Downloading the file for generic repo";

  @Inject private ArtifactCollectionTaskHelper artifactCollectionTaskHelper;
  @Inject private EncryptionService encryptionService;

  @Override
  public Map<String, String> getRepositories(
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails) {
    return getRepositories(artifactoryConfig, encryptionDetails, Collections.singletonList(docker));
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
        return getRepositories(artifactoryConfig, encryptionDetails, Collections.singletonList(maven));
      default:
        return getRepositories(artifactoryConfig, encryptionDetails,
            Arrays.stream(PackageTypeImpl.values()).filter(type -> docker != type).collect(toList()));
    }
  }

  @Override
  public Map<String, String> getRepositories(
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails, RepositoryType repositoryType) {
    switch (repositoryType) {
      case docker:
        return getRepositories(artifactoryConfig, encryptionDetails);
      case maven:
        return getRepositories(artifactoryConfig, encryptionDetails, Arrays.asList(maven));
      case any:
        return getRepositories(artifactoryConfig, encryptionDetails,
            Arrays.stream(PackageTypeImpl.values()).filter(type -> docker != type).collect(toList()));
      default:
        return getRepositories(artifactoryConfig, encryptionDetails, "");
    }
  }

  private Map<String, String> getRepositories(ArtifactoryConfig artifactoryConfig,
      List<EncryptedDataDetail> encryptionDetails, List<PackageTypeImpl> packageTypes) {
    log.info("Retrieving repositories for packages {}", packageTypes.toArray());
    Map<String, String> repositories = new HashMap<>();
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig, encryptionDetails);
    ArtifactoryRequest repositoryRequest =
        new ArtifactoryRequestImpl().apiUrl("api/repositories/").method(GET).responseType(JSON);
    String errorOccurredWhileRetrievingRepositories = "Error occurred while retrieving repositories";
    try {
      ArtifactoryResponse response = artifactory.restCall(repositoryRequest);
      handleErrorResponse(response);
      List<Map<Object, Object>> responseList = response.parseBody(List.class);
      for (Map<Object, Object> repository : responseList) {
        String repoKey = repository.get(KEY).toString();
        try {
          Repository repo = artifactory.repository(repoKey).get();
          RepositorySettings settings = repo.getRepositorySettings();
          if (packageTypes.contains(settings.getPackageType())) {
            repositories.put(repository.get(KEY).toString(), repository.get(KEY).toString());
          }
        } catch (Exception e) {
          log.warn("Failed to get repository settings for repo {}, Reason {}", repoKey, ExceptionUtils.getMessage(e));
          // TODO : Get Settings api only works for Artifactory Pro
          repositories.put(repository.get(KEY).toString(), repository.get(KEY).toString());
        }
      }
      if (repositories.isEmpty()) {
        // Better way of handling Unauthorized access
        log.info("Repositories are not available of package types {} or User not authorized to access artifactory",
            packageTypes);
      }
      log.info("Retrieving repositories for packages {} success", packageTypes.toArray());
    } catch (SocketTimeoutException e) {
      log.error(errorOccurredWhileRetrievingRepositories, e);
      return repositories;
    } catch (Exception e) {
      log.error(errorOccurredWhileRetrievingRepositories, e);
      handleAndRethrow(e, USER);
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
    String errorOnListingDockerimages = "Error occurred while listing docker images from artifactory %s for Repo %s";
    try {
      log.info("Retrieving docker images from artifactory url {} and repo key {}", artifactory.getUri(), repoKey);
      ArtifactoryResponse artifactoryResponse = artifactory.restCall(new ArtifactoryRequestImpl()
                                                                         .apiUrl("api/docker/" + repoKey + "/v2"
                                                                             + "/_catalog")
                                                                         .method(GET)
                                                                         .responseType(JSON));
      handleErrorResponse(artifactoryResponse);
      Map response = artifactoryResponse.parseBody(Map.class);
      if (response != null) {
        images = (List<String>) response.get("repositories");
        if (isEmpty(images)) {
          log.info("No docker images from artifactory url {} and repo key {}", artifactory.getUri(), repoKey);
          images = new ArrayList<>();
        }
        log.info("Retrieving images from artifactory url {} and repo key {} success. Images {}", artifactory.getUri(),
            repoKey, images);
      }
    } catch (SocketTimeoutException e) {
      log.error(format(errorOnListingDockerimages, artifactory, repoKey), e);
      return images;
    } catch (Exception e) {
      log.error(format(errorOnListingDockerimages, artifactory, repoKey), e);
      handleAndRethrow(e, USER);
    }
    return images;
  }

  @Override
  public List<BuildDetails> getBuilds(ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes, int maxNumberOfBuilds) {
    String repoKey = artifactStreamAttributes.getJobName();
    String imageName = artifactStreamAttributes.getImageName();
    log.info("Retrieving docker tags for repoKey {} imageName {} ", repoKey, imageName);
    List<BuildDetails> buildDetails = new ArrayList<>();
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig, encryptionDetails);
    try {
      ArtifactoryRequest repositoryRequest = new ArtifactoryRequestImpl()
                                                 .apiUrl("api/docker/" + repoKey + "/v2/" + imageName + "/tags/list")
                                                 .method(GET)
                                                 .responseType(JSON);
      ArtifactoryResponse artifactoryResponse = artifactory.restCall(repositoryRequest);
      handleErrorResponse(artifactoryResponse);
      Map response = artifactoryResponse.parseBody(Map.class);
      if (response != null) {
        List<String> tags = (List<String>) response.get("tags");
        if (isEmpty(tags)) {
          log.info("No  docker tags for repoKey {} imageName {} success ", repoKey, imageName);
          return buildDetails;
        }
        String tagUrl = getBaseUrl(artifactoryConfig) + repoKey + "/" + imageName + "/";
        String repoName = ArtifactUtilities.getArtifactoryRepositoryName(artifactoryConfig.getArtifactoryUrl(),
            artifactStreamAttributes.getArtifactoryDockerRepositoryServer(), artifactStreamAttributes.getJobName(),
            artifactStreamAttributes.getImageName());
        buildDetails = tags.stream()
                           .map(tag -> {
                             Map<String, String> metadata = new HashMap();
                             metadata.put(ArtifactMetadataKeys.image, repoName + ":" + tag);
                             metadata.put(ArtifactMetadataKeys.tag, tag);
                             return aBuildDetails()
                                 .withNumber(tag)
                                 .withBuildUrl(tagUrl + tag)
                                 .withMetadata(metadata)
                                 .withUiDisplayName("Tag# " + tag)
                                 .build();
                           })
                           .collect(toList());
        log.info(
            "Retrieving docker tags for repoKey {} imageName {} success. Retrieved tags {}", repoKey, imageName, tags);
      }

    } catch (Exception e) {
      log.info("Exception occurred while retrieving the docker docker tags for Image {}", imageName);
      handleAndRethrow(e, USER);
    }

    // Sorting at build tag for docker artifacts.
    return buildDetails.stream().sorted(new BuildDetailsComparatorAscending()).collect(toList());
  }

  @Override
  public List<BuildDetails> getFilePaths(ArtifactoryConfig artifactoryConfig,
      List<EncryptedDataDetail> encryptionDetails, String repositoryName, String artifactPath, String repositoryType,
      int maxVersions) {
    log.info("Retrieving file paths for repositoryName {} artifactPath {}", repositoryName, artifactPath);
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
        ArtifactoryResponse artifactoryResponse = artifactory.restCall(repositoryRequest);
        if (artifactoryResponse.getStatusLine().getStatusCode() == 403
            || artifactoryResponse.getStatusLine().getStatusCode() == 400) {
          log.warn(
              "User not authorized to perform or using OSS version deep level search. Trying with different search api. Message {}",
              artifactoryResponse.getStatusLine().getReasonPhrase());
          return getBuildDetails(artifactoryConfig, artifactory, repositoryName, artifactPath, maxVersions);
        }
        Map<String, List> response = artifactoryResponse.parseBody(Map.class);
        if (response != null) {
          List<Map<String, String>> results = response.get(RESULTS);
          if (results != null) {
            for (Map<String, String> result : results) {
              String createdBy = result.get(CREATED_BY);
              if (createdBy == null || !createdBy.equals(SYSTEM)) {
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
        log.info("Artifact paths order from Artifactory Server" + artifactPaths);
        Collections.reverse(artifactPaths);
        String finalArtifactPath = artifactPath;
        return artifactPaths.stream()
            .map(path
                -> aBuildDetails()
                       .withNumber(constructBuildNumber(finalArtifactPath, path.substring(path.indexOf('/') + 1)))
                       .withArtifactPath(path)
                       .withBuildUrl(getBaseUrl(artifactoryConfig) + path)
                       .withArtifactFileSize(map.get(path))
                       .withUiDisplayName(
                           "Build# " + constructBuildNumber(finalArtifactPath, path.substring(path.indexOf('/') + 1)))
                       .build())
            .collect(toList());
      } else {
        throw new ArtifactoryServerException("Artifact path can not be empty", INVALID_ARTIFACT_SERVER, USER);
      }
    } catch (Exception e) {
      log.error("Error occurred while retrieving File Paths from Artifactory server {}",
          artifactoryConfig.getArtifactoryUrl(), e);
      handleAndRethrow(e, USER);
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
    log.info("Retrieving file paths for repoKey {} artifactPath {}", repoKey, artifactPath);
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
        throw new ArtifactoryServerException("Artifact path can not be empty", INVALID_ARTIFACT_SERVER);
      }
      log.info("Artifact paths order from Artifactory Server" + artifactPaths);
      return artifactPaths;
    } catch (Exception e) {
      log.error(
          format("Error occurred while retrieving File Paths from Artifactory server %s", artifactory.getUsername()),
          e);
      handleAndRethrow(e, USER);
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
      ArtifactoryResponse artifactoryResponse = artifactory.restCall(repositoryRequest);
      handleErrorResponse(artifactoryResponse);
      LinkedHashMap<String, Object> response = artifactoryResponse.parseBody(LinkedHashMap.class);
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
      log.error("Exception occurred in retrieving folder paths", e);
    }
    return folderPaths;
  }

  @Override
  public ListNotifyResponseData downloadArtifacts(ArtifactoryConfig artifactoryConfig,
      List<EncryptedDataDetail> encryptionDetails, String repositoryName, Map<String, String> metadata,
      String delegateId, String taskId, String accountId) {
    ListNotifyResponseData res = new ListNotifyResponseData();
    String artifactPath = metadata.get(ArtifactMetadataKeys.artifactPath).replaceFirst(repositoryName, "").substring(1);
    String artifactName = metadata.get(ArtifactMetadataKeys.artifactFileName);
    try {
      log.info(DOWNLOAD_FILE_FOR_GENERIC_REPO);
      InputStream inputStream = downloadArtifacts(artifactoryConfig, encryptionDetails, repositoryName, metadata);
      artifactCollectionTaskHelper.addDataToResponse(
          new ImmutablePair<>(artifactName, inputStream), artifactPath, res, delegateId, taskId, accountId);
      return res;
    } catch (Exception e) {
      String msg =
          "Failed to download the latest artifacts  of repo [" + repositoryName + "] artifactPath [" + artifactPath;
      prepareAndThrowException(msg + REASON + ExceptionUtils.getRootCauseMessage(e), USER, e);
    }
    return res;
  }

  @Override
  public Pair<String, InputStream> downloadArtifact(ArtifactoryConfig artifactoryConfig,
      List<EncryptedDataDetail> encryptionDetails, String repositoryName, Map<String, String> metadata) {
    Pair<String, InputStream> pair = null;
    String artifactPath = metadata.get(ArtifactMetadataKeys.artifactPath).replaceFirst(repositoryName, "").substring(1);
    String artifactName = metadata.get(ArtifactMetadataKeys.artifactFileName);
    try {
      log.info(DOWNLOAD_FILE_FOR_GENERIC_REPO);
      InputStream inputStream = downloadArtifacts(artifactoryConfig, encryptionDetails, repositoryName, metadata);
      pair = new ImmutablePair<>(artifactName, inputStream);
    } catch (Exception e) {
      String msg =
          "Failed to download the latest artifacts  of repo [" + repositoryName + "] artifactPath [" + artifactPath;
      prepareAndThrowException(msg + REASON + ExceptionUtils.getRootCauseMessage(e), USER, e);
    }
    return pair;
  }

  @Override
  public boolean validateArtifactPath(ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails,
      String repositoryName, String artifactPath, String repositoryType) {
    log.info("Validating artifact path {} for repository {} and repositoryType {}", artifactPath, repositoryName,
        repositoryType);
    if (isBlank(artifactPath)) {
      throw new ArtifactoryServerException("Artifact Pattern can not be empty", ARTIFACT_SERVER_ERROR, USER);
    }
    List<BuildDetails> filePaths =
        getFilePaths(artifactoryConfig, encryptionDetails, repositoryName, artifactPath, repositoryType, 1);

    if (isEmpty(filePaths)) {
      prepareAndThrowException("No artifact files matching with the artifact path [" + artifactPath + "]", USER, null);
    }
    log.info("Validating whether directory exists or not for Generic repository type by fetching file paths");
    return true;
  }

  private void prepareAndThrowException(String message, EnumSet<ReportTarget> reportTargets, Exception e) {
    throw new ArtifactoryServerException(message, ErrorCode.INVALID_ARTIFACT_SERVER, reportTargets, e);
  }

  private InputStream downloadArtifacts(ArtifactoryConfig artifactoryConfig,
      List<EncryptedDataDetail> encryptionDetails, String repoKey, Map<String, String> metadata) {
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig, encryptionDetails);
    Set<String> artifactNames = new HashSet<>();
    String artifactPath = metadata.get(ArtifactMetadataKeys.artifactPath).replaceFirst(repoKey, "").substring(1);
    String artifactName = metadata.get(ArtifactMetadataKeys.artifactFileName);

    try {
      log.info("Artifact name {}", artifactName);
      if (artifactNames.add(artifactName)) {
        if (metadata.get(ArtifactMetadataKeys.artifactPath) != null) {
          log.info(DOWNLOAD_FILE_FOR_GENERIC_REPO);
          log.info("Downloading file {} ", artifactPath);
          InputStream inputStream = artifactory.repository(repoKey).download(artifactPath).doDownload();
          log.info("Downloading file {} success", artifactPath);
          return inputStream;
        }
      }
    } catch (Exception e) {
      log.error("Failed to download the artifact of repository {} from path {}", repoKey, artifactPath, e);
      String msg =
          "Failed to download the latest artifacts  of repository [" + repoKey + "] file path [" + artifactPath;
      throw new ArtifactoryServerException(
          msg + REASON + ExceptionUtils.getRootCauseMessage(e), ARTIFACT_SERVER_ERROR, USER);
    }
    log.info(
        "Downloading artifacts from artifactory for repository  {} and file path {} success", repoKey, artifactPath);
    return null;
  }

  /**
   * Get Artifactory Client
   *
   * @param artifactoryConfig
   * @return Artifactory returns artifactory client
   */

  @VisibleForTesting
  Artifactory getArtifactoryClient(ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails) {
    encryptionService.decrypt(artifactoryConfig, encryptionDetails, false);
    ArtifactoryClientBuilder builder = ArtifactoryClientBuilder.create();
    try {
      builder.setUrl(getBaseUrl(artifactoryConfig));
      if (artifactoryConfig.hasCredentials()) {
        if (isEmpty(artifactoryConfig.getPassword())) {
          throw new ArtifactoryServerException(
              "Password is a required field along with Username", ErrorCode.INVALID_ARTIFACT_SERVER, USER);
        }
        builder.setUsername(artifactoryConfig.getUsername());
        builder.setPassword(new String(artifactoryConfig.getPassword()));
      } else {
        log.info("Username is not set for artifactory config {} . Will use anonymous access.",
            artifactoryConfig.getArtifactoryUrl());
      }

      checkIfUseProxyAndAppendConfig(builder, artifactoryConfig);
      builder.setSocketTimeout(30000);
      builder.setConnectionTimeout(30000);
    } catch (Exception ex) {
      handleAndRethrow(ex, USER);
    }
    return builder.build();
  }

  protected void checkIfUseProxyAndAppendConfig(ArtifactoryClientBuilder builder, ArtifactoryConfig artifactoryConfig) {
    HttpHost httpProxyHost = Http.getHttpProxyHost(artifactoryConfig.getArtifactoryUrl());
    if (httpProxyHost != null && !Http.shouldUseNonProxy(artifactoryConfig.getArtifactoryUrl())) {
      builder.setProxy(new ProxyConfig(httpProxyHost.getHostName(), httpProxyHost.getPort(), Http.getProxyScheme(),
          Http.getProxyUserName(), Http.getProxyPassword()));
    }
  }

  private String getBaseUrl(ArtifactoryConfig artifactoryConfig) {
    return artifactoryConfig.getArtifactoryUrl().endsWith("/") ? artifactoryConfig.getArtifactoryUrl()
                                                               : artifactoryConfig.getArtifactoryUrl() + "/";
  }

  private void handleAndRethrow(Exception e, EnumSet<ReportTarget> reportTargets) {
    if (e instanceof HttpResponseException) {
      throw new ArtifactoryServerException(e.getMessage(), ErrorCode.INVALID_ARTIFACT_SERVER, reportTargets);
    }
    if (e instanceof SocketTimeoutException) {
      String serverMayNotBeRunningMessaage = e.getMessage() + "."
          + "SocketTimeout: Artifactory server may not be running";
      throw new ArtifactoryServerException(
          serverMayNotBeRunningMessaage, ErrorCode.INVALID_ARTIFACT_SERVER, reportTargets);
    }
    if (e instanceof WingsException) {
      throw(WingsException) e;
    }
    throw new ArtifactoryServerException(ExceptionUtils.getMessage(e), ARTIFACT_SERVER_ERROR, reportTargets, e);
  }

  @Override
  public Long getFileSize(
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails, Map<String, String> metadata) {
    String artifactPath = metadata.get(ArtifactMetadataKeys.artifactPath);
    log.info("Retrieving file paths for artifactPath {}", artifactPath);
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig, encryptionDetails);
    try {
      String apiStorageQuery = "api/storage/" + artifactPath;

      ArtifactoryRequest repositoryRequest =
          new ArtifactoryRequestImpl().apiUrl(apiStorageQuery).method(GET).requestType(TEXT).responseType(JSON);
      ArtifactoryResponse artifactoryResponse = artifactory.restCall(repositoryRequest);
      handleErrorResponse(artifactoryResponse);
      LinkedHashMap<String, String> response = artifactoryResponse.parseBody(LinkedHashMap.class);
      if (response != null && isNotBlank(response.get("size"))) {
        return Long.valueOf(response.get("size"));
      } else {
        throw new ArtifactoryServerException(
            "Unable to get artifact file size. The file probably does not exist", INVALID_ARTIFACT_SERVER, USER);
      }
    } catch (Exception e) {
      log.error("Error occurred while retrieving File Paths from Artifactory server {}",
          artifactoryConfig.getArtifactoryUrl(), e);
      handleAndRethrow(e, USER);
    }
    return 0L;
  }

  @Override
  public boolean isRunning(ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails) {
    log.info("Validating artifactory server");
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig, encryptionDetails);
    ArtifactoryRequest repositoryRequest =
        new ArtifactoryRequestImpl().apiUrl("api/repositories/").method(GET).responseType(JSON);
    try {
      ArtifactoryResponse artifactoryResponse = artifactory.restCall(repositoryRequest);
      handleErrorResponse(artifactoryResponse);
      log.info("Validating artifactory server success");
    } catch (RuntimeException e) {
      log.error("Runtime exception occurred while validating artifactory", e);
      handleAndRethrow(e, USER);
    } catch (SocketTimeoutException e) {
      log.error("Exception occurred while validating artifactory", e);
      return true;
    } catch (Exception e) {
      log.error("Exception occurred while validating artifactory", e);
      handleAndRethrow(e, USER);
    }
    return true;
  }

  private void handleErrorResponse(ArtifactoryResponse artifactoryResponse) throws java.io.IOException {
    if (!artifactoryResponse.isSuccessResponse()) {
      if (artifactoryResponse.getStatusLine().getStatusCode() == 407) {
        throw new InvalidRequestException(artifactoryResponse.getStatusLine().getReasonPhrase());
      }
      ArtifactoryErrorResponse errorResponse = artifactoryResponse.parseBody(ArtifactoryErrorResponse.class);
      String errorMessage =
          "Request to server failed with status code: " + artifactoryResponse.getStatusLine().getStatusCode();
      if (isNotEmpty(errorResponse.getErrors())) {
        errorMessage +=
            " with message - " + errorResponse.getErrors().stream().map(ArtifactoryError::getMessage).findFirst().get();
      }
      throw new ArtifactoryServerException(errorMessage, ErrorCode.INVALID_ARTIFACT_SERVER, USER);
    }
  }

  private List<BuildDetails> getBuildDetails(ArtifactoryConfig artifactoryConfig, Artifactory artifactory,
      String repositoryName, String artifactPath, int maxVersions) {
    List<String> artifactPaths = getFilePathsForAnonymousUser(artifactory, repositoryName, artifactPath, maxVersions);
    return artifactPaths.stream()
        .map(path
            -> aBuildDetails()
                   .withNumber(constructBuildNumber(artifactPath, path.substring(path.indexOf('/') + 1)))
                   .withArtifactPath(path)
                   .withBuildUrl(getBaseUrl(artifactoryConfig) + path)
                   .withUiDisplayName(
                       "Build# " + constructBuildNumber(artifactPath, path.substring(path.indexOf('/') + 1)))
                   .build())
        .collect(toList());
  }

  @Data
  public static class ArtifactoryErrorResponse {
    List<ArtifactoryError> errors;
  }

  @Data
  public static class ArtifactoryError {
    String message;
    int status;
  }
}
