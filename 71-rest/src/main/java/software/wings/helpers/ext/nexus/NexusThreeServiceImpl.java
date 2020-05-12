package software.wings.helpers.ext.nexus;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.helpers.ext.nexus.NexusServiceImpl.getBaseUrl;
import static software.wings.helpers.ext.nexus.NexusServiceImpl.getRetrofit;
import static software.wings.helpers.ext.nexus.NexusServiceImpl.isSuccessful;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifact.ArtifactUtilities;
import io.harness.delegate.beans.artifact.ArtifactFileMetadata;
import io.harness.delegate.exception.ArtifactServerException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.network.Http;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.stream.StreamUtils;
import io.harness.waiter.ListNotifyResponseData;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import retrofit2.Response;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.config.NexusConfig;
import software.wings.common.AlphanumComparator;
import software.wings.delegatetasks.collect.artifacts.ArtifactCollectionTaskHelper;
import software.wings.exception.InvalidArtifactServerException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.nexus.model.Asset;
import software.wings.helpers.ext.nexus.model.DockerImageResponse;
import software.wings.helpers.ext.nexus.model.DockerImageTagResponse;
import software.wings.helpers.ext.nexus.model.Nexus3AssetResponse;
import software.wings.helpers.ext.nexus.model.Nexus3ComponentResponse;
import software.wings.helpers.ext.nexus.model.Nexus3Repository;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.RepositoryFormat;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.net.ssl.HttpsURLConnection;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class NexusThreeServiceImpl {
  @Inject EncryptionService encryptionService;
  @Inject private ArtifactCollectionTaskHelper artifactCollectionTaskHelper;
  @Inject private NexusHelper nexusHelper;

  public Map<String, String> getRepositories(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repositoryFormat) throws IOException {
    logger.info("Retrieving repositories");
    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig, encryptionDetails);
    Response<List<Nexus3Repository>> response;
    if (nexusConfig.hasCredentials()) {
      response =
          nexusThreeRestClient
              .listRepositories(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())))
              .execute();
    } else {
      response = nexusThreeRestClient.listRepositories().execute();
    }

    if (isSuccessful(response)) {
      if (isNotEmpty(response.body())) {
        logger.info("Retrieving {} repositories success", repositoryFormat);
        final Map<String, String> repositories;
        if (repositoryFormat == null) {
          repositories =
              response.body().stream().collect(Collectors.toMap(Nexus3Repository::getName, Nexus3Repository::getName));
        } else {
          final String filterBy = repositoryFormat.equals(RepositoryFormat.maven.name()) ? "maven2" : repositoryFormat;
          repositories = response.body()
                             .stream()
                             .filter(o -> o.getFormat().equals(filterBy))
                             .collect(Collectors.toMap(Nexus3Repository::getName, Nexus3Repository::getName));
        }
        logger.info("Retrieved repositories are {}", repositories.values());
        return repositories;
      } else {
        throw new WingsException(INVALID_ARTIFACT_SERVER, WingsException.USER)
            .addParam("message", "Failed to fetch the repositories");
      }
    }
    logger.info("No repositories found returning empty map");
    return emptyMap();
  }

  public List<String> getPackageNames(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repository, String repositoryFormat, List<String> images) throws IOException {
    logger.info("Retrieving packageNames for repositoryFormat {}", repository);
    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig, encryptionDetails);
    Response<Nexus3ComponentResponse> response;
    boolean hasMoreResults = true;
    String continuationToken = null;
    while (hasMoreResults) {
      hasMoreResults = false;
      if (nexusConfig.hasCredentials()) {
        response = nexusThreeRestClient
                       .search(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())),
                           repository, continuationToken)
                       .execute();
      } else {
        response = nexusThreeRestClient.search(repository, continuationToken).execute();
      }
      Set<String> packages = null;
      if (isSuccessful(response)) {
        if (response.body() != null) {
          if (isNotEmpty(response.body().getItems())) {
            if (repositoryFormat.equals(RepositoryFormat.nuget.name())
                || repositoryFormat.equals(RepositoryFormat.npm.name())) {
              packages = response.body()
                             .getItems()
                             .stream()
                             .map(Nexus3ComponentResponse.Component::getName)
                             .collect(Collectors.toSet());
            }
            if (isNotEmpty(packages)) {
              for (String p : packages) {
                if (!images.contains(p)) {
                  images.add(p);
                }
              }
            }
          }
          if (response.body().getContinuationToken() != null) {
            continuationToken = response.body().getContinuationToken();
            hasMoreResults = true;
          }
        } else {
          throw new WingsException(INVALID_ARTIFACT_SERVER, WingsException.USER)
              .addParam("message", "Failed to fetch the package names");
        }
      }
    }
    return images;
  }

  public List<String> getGroupIds(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repository, String repositoryFormat, List<String> images) throws IOException {
    logger.info("Retrieving groups for repositoryFormat {}", repository);
    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig, encryptionDetails);
    Response<Nexus3ComponentResponse> response;
    boolean hasMoreResults = true;
    String continuationToken = null;
    while (hasMoreResults) {
      hasMoreResults = false;
      if (nexusConfig.hasCredentials()) {
        response = nexusThreeRestClient
                       .search(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())),
                           repository, continuationToken)
                       .execute();
      } else {
        response = nexusThreeRestClient.search(repository, continuationToken).execute();
      }
      Set<String> packages = null;
      if (isSuccessful(response)) {
        if (response.body() != null) {
          if (isNotEmpty(response.body().getItems())) {
            if (repositoryFormat.equals(RepositoryFormat.maven.name())) {
              packages = response.body()
                             .getItems()
                             .stream()
                             .map(Nexus3ComponentResponse.Component::getGroup)
                             .collect(Collectors.toSet());
            }
            if (isNotEmpty(packages)) {
              for (String p : packages) {
                if (!images.contains(p)) {
                  images.add(p);
                }
              }
            }
          }
          if (response.body().getContinuationToken() != null) {
            continuationToken = response.body().getContinuationToken();
            hasMoreResults = true;
          }
        } else {
          throw new WingsException(INVALID_ARTIFACT_SERVER, WingsException.USER)
              .addParam("message", "Failed to fetch the groupIds");
        }
      }
    }
    return images;
  }

  public List<String> getDockerImages(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repository, List<String> images) throws IOException {
    logger.info("Retrieving docker images for repository {} from url {}", repository, nexusConfig.getNexusUrl());
    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig, encryptionDetails);
    Response<DockerImageResponse> response;
    if (nexusConfig.hasCredentials()) {
      response =
          nexusThreeRestClient
              .getDockerImages(
                  Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), repository)
              .execute();
    } else {
      response = nexusThreeRestClient.getDockerImages(repository).execute();
    }
    if (isSuccessful(response)) {
      if (response.body() != null && response.body().getRepositories() != null) {
        images.addAll(response.body().getRepositories().stream().collect(toList()));
        logger.info("Retrieving docker images for repository {} from url {} success. Images are {}", repository,
            nexusConfig.getNexusUrl(), images);
      }
    } else {
      logger.warn("Failed to fetch the docker images as request is not success");
      throw new WingsException(INVALID_ARTIFACT_SERVER, WingsException.USER)
          .addParam("message", "Failed to fetch the docker images");
    }
    logger.info("No images found for repository {}", repository);
    return images;
  }

  public List<BuildDetails> getPackageVersions(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repositoryName, String packageName) throws IOException {
    logger.info("Retrieving package versions for repository {} package {} ", repositoryName, packageName);
    List<String> versions = new ArrayList<>();
    Map<String, Asset> versionToArtifactUrls = new HashMap<>();
    Map<String, List<ArtifactFileMetadata>> versionToArtifactDownloadUrls = new HashMap<>();
    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig, encryptionDetails);
    Response<Nexus3ComponentResponse> response;
    boolean hasMoreResults = true;
    String continuationToken = null;
    while (hasMoreResults) {
      hasMoreResults = false;
      if (nexusConfig.hasCredentials()) {
        response =
            nexusThreeRestClient
                .getPackageVersions(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())),
                    repositoryName, packageName, continuationToken)
                .execute();

      } else {
        response = nexusThreeRestClient.getPackageVersions(repositoryName, packageName, continuationToken).execute();
      }

      if (isSuccessful(response)) {
        if (response.body() != null) {
          if (isNotEmpty(response.body().getItems())) {
            for (Nexus3ComponentResponse.Component component : response.body().getItems()) {
              String version = component.getVersion();
              versions.add(version); // todo: add limit if results are returned in descending order of lastUpdatedTs
              if (isNotEmpty(component.getAssets())) {
                versionToArtifactUrls.put(version, component.getAssets().get(0));
              }
              // for each version - get all assets and store download urls in metadata
              versionToArtifactDownloadUrls.put(version, getDownloadUrlsForPackageVersion(component));
            }
          }
          if (response.body().getContinuationToken() != null) {
            continuationToken = response.body().getContinuationToken();
            hasMoreResults = true;
          }
        }
      } else {
        throw new WingsException(INVALID_ARTIFACT_SERVER, WingsException.USER)
            .addParam("message", "Failed to fetch the versions for package [" + packageName + "]");
      }
    }
    logger.info("Versions come from nexus server {}", versions);
    versions = versions.stream().sorted(new AlphanumComparator()).collect(toList());
    logger.info("After sorting alphanumerically versions {}", versions);

    return versions.stream()
        .map(version -> {
          Map<String, String> metadata = new HashMap<>();
          metadata.put(ArtifactMetadataKeys.repositoryName, repositoryName);
          metadata.put(ArtifactMetadataKeys.nexusPackageName, packageName);
          metadata.put(ArtifactMetadataKeys.version, version);
          String url = null;
          if (versionToArtifactUrls.get(version) != null) {
            url = (versionToArtifactUrls.get(version)).getDownloadUrl();
            metadata.put(ArtifactMetadataKeys.url, url);
            metadata.put(ArtifactMetadataKeys.artifactPath, (versionToArtifactUrls.get(version)).getPath());
          }
          return aBuildDetails()
              .withNumber(version)
              .withRevision(version)
              .withBuildUrl(url)
              .withMetadata(metadata)
              .withUiDisplayName("Version# " + version)
              .withArtifactDownloadMetadata(versionToArtifactDownloadUrls.get(version))
              .build();
        })
        .collect(toList());
  }

  private List<ArtifactFileMetadata> getDownloadUrlsForPackageVersion(Nexus3ComponentResponse.Component component) {
    List<Asset> assets = component.getAssets();
    List<ArtifactFileMetadata> artifactFileMetadata = new ArrayList<>();
    if (isNotEmpty(assets)) {
      for (Asset asset : assets) {
        String url = asset.getDownloadUrl();
        String artifactName;
        if (RepositoryFormat.nuget.name().equals(component.getFormat())) {
          artifactName = component.getName() + "-" + component.getVersion() + ".nupkg";
        } else {
          artifactName = url.substring(url.lastIndexOf('/') + 1);
        }
        if (artifactName.endsWith("pom") || artifactName.endsWith("md5") || artifactName.endsWith("sha1")) {
          continue;
        }
        artifactFileMetadata.add(ArtifactFileMetadata.builder().fileName(artifactName).url(url).build());
      }
    }
    return artifactFileMetadata;
  }

  public List<BuildDetails> getDockerTags(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) throws IOException {
    String repoKey = artifactStreamAttributes.getJobName();
    String imageName = artifactStreamAttributes.getImageName();
    String repoName = ArtifactUtilities.getNexusRepositoryName(nexusConfig.getNexusUrl(),
        artifactStreamAttributes.getNexusDockerPort(), artifactStreamAttributes.getNexusDockerRegistryUrl(),
        artifactStreamAttributes.getImageName());
    logger.info("Retrieving docker tags for repository {} imageName {} ", repoKey, imageName);
    List<BuildDetails> buildDetails = new ArrayList<>();
    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig, encryptionDetails);
    Response<DockerImageTagResponse> response;

    if (nexusConfig.hasCredentials()) {
      response = nexusThreeRestClient
                     .getDockerTags(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())),
                         repoKey, imageName)
                     .execute();

    } else {
      response = nexusThreeRestClient.getDockerTags(repoKey, imageName).execute();
    }

    if (isSuccessful(response)) {
      if (response.body() != null && response.body().getTags() != null) {
        return response.body()
            .getTags()
            .stream()
            .map(tag -> {
              Map<String, String> metadata = new HashMap();
              metadata.put(ArtifactMetadataKeys.image, repoName + ":" + tag);
              metadata.put(ArtifactMetadataKeys.tag, tag);
              return aBuildDetails().withNumber(tag).withMetadata(metadata).withUiDisplayName("Tag# " + tag).build();
            })
            .collect(toList());
      }
    } else {
      throw new WingsException(INVALID_ARTIFACT_SERVER, WingsException.USER)
          .addParam("message", "Failed to fetch the docker tags of image [" + imageName + "]");
    }
    logger.info("No tags found for image name {}", imageName);
    return buildDetails;
  }

  private NexusThreeRestClient getNexusThreeClient(
      final NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails) {
    if (nexusConfig.hasCredentials()) {
      encryptionService.decrypt(nexusConfig, encryptionDetails);
    }
    return getRetrofit(getBaseUrl(nexusConfig), JacksonConverterFactory.create()).create(NexusThreeRestClient.class);
  }

  public Pair<String, InputStream> downloadArtifact(NexusConfig nexusConfig,
      List<EncryptedDataDetail> encryptionDetails, ArtifactStreamAttributes artifactStreamAttributes,
      Map<String, String> artifactMetadata, String delegateId, String taskId, String accountId,
      ListNotifyResponseData notifyResponseData) throws IOException {
    String repositoryFormat = artifactStreamAttributes.getRepositoryFormat();
    if (repositoryFormat != null) {
      if (repositoryFormat.equals(RepositoryFormat.nuget.name())
          || repositoryFormat.equals(RepositoryFormat.npm.name())) {
        final String version = artifactMetadata.get(ArtifactMetadataKeys.buildNo);
        final String packageName = artifactMetadata.get(ArtifactMetadataKeys.nexusPackageName);
        final String repoName = artifactMetadata.get(ArtifactMetadataKeys.repositoryName);
        logger.info("Downloading version {} of package {} from repository {}", version, packageName, repoName);
        NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig, encryptionDetails);
        Response<Nexus3AssetResponse> response;
        if (nexusConfig.hasCredentials()) {
          response = nexusThreeRestClient
                         .getAsset(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())),
                             repoName, packageName, version)
                         .execute();
        } else {
          response = nexusThreeRestClient.getAsset(repoName, packageName, version).execute();
        }

        if (isSuccessful(response)) {
          if (response.body() != null) {
            if (isNotEmpty(response.body().getItems())) {
              response.body().getItems().forEach(item -> {
                String url = item.getDownloadUrl();
                String artifactName = url.substring(url.lastIndexOf('/') + 1);
                if (artifactName.endsWith("pom") || artifactName.endsWith("md5") || artifactName.endsWith("sha1")) {
                  return;
                }
                downloadArtifactByUrl(nexusConfig, encryptionDetails, delegateId, taskId, accountId, notifyResponseData,
                    artifactName, url);
              });
            }
          } else {
            throw new WingsException(INVALID_ARTIFACT_SERVER, WingsException.USER)
                .addParam("message", "Unable to find package [" + packageName + "] version [" + version + "]");
          }
        } else {
          throw new WingsException(INVALID_ARTIFACT_SERVER, WingsException.USER)
              .addParam("message", "Failed to download package [" + packageName + "] version [" + version + "]");
        }
      } else if (repositoryFormat.equals(RepositoryFormat.maven.name())) {
        final String version = artifactMetadata.get(ArtifactMetadataKeys.buildNo);
        final String groupId = artifactStreamAttributes.getGroupId();
        final String artifactName = artifactStreamAttributes.getArtifactName();
        final String repoName = artifactStreamAttributes.getJobName();
        final String extension = artifactStreamAttributes.getExtension();
        final String classifier = artifactStreamAttributes.getClassifier();

        logger.info("Downloading version {} of groupId: {} artifactId: {} from repository {}", version, groupId,
            artifactName, repoName);
        NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig, encryptionDetails);
        Response<Nexus3AssetResponse> response = getNexus3MavenAssets(
            nexusConfig, version, groupId, artifactName, repoName, extension, classifier, nexusThreeRestClient);

        if (isSuccessful(response)) {
          if (response.body() != null) {
            if (isNotEmpty(response.body().getItems())) {
              response.body().getItems().forEach(item -> {
                String url = item.getDownloadUrl();
                String artifactFileName = url.substring(url.lastIndexOf('/') + 1);
                if (artifactFileName.endsWith("pom") || artifactFileName.endsWith("md5")
                    || artifactFileName.endsWith("sha1")) {
                  return;
                }
                downloadArtifactByUrl(nexusConfig, encryptionDetails, delegateId, taskId, accountId, notifyResponseData,
                    artifactFileName, url);
              });
            }
          } else {
            throw new WingsException(INVALID_ARTIFACT_SERVER, WingsException.USER)
                .addParam("message",
                    "Unable to find artifact for groupId [" + groupId + "] artifactId [" + artifactName + "] version ["
                        + version + "]");
          }
        } else {
          throw new WingsException(INVALID_ARTIFACT_SERVER, WingsException.USER)
              .addParam("message",
                  "Failed to download artifact for groupId [" + groupId + "] artifactId [" + artifactName + "]version ["
                      + version + "]");
        }
      }
    }
    return null;
  }

  @SuppressWarnings({"squid:S00107"})
  private Response<Nexus3AssetResponse> getNexus3MavenAssets(NexusConfig nexusConfig, String version, String groupId,
      String artifactName, String repoName, String extension, String classifier,
      NexusThreeRestClient nexusThreeRestClient) throws IOException {
    Response<Nexus3AssetResponse> response;
    if (nexusConfig.hasCredentials()) {
      if (isNotEmpty(extension) || isNotEmpty(classifier)) {
        response = nexusThreeRestClient
                       .getMavenAssetWithExtensionAndClassifier(
                           Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())),
                           repoName, groupId, artifactName, version, extension, classifier)
                       .execute();
      } else {
        response =
            nexusThreeRestClient
                .getMavenAsset(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())),
                    repoName, groupId, artifactName, version)
                .execute();
      }
    } else {
      if (isNotEmpty(extension) || isNotEmpty(classifier)) {
        response = nexusThreeRestClient
                       .getMavenAssetWithExtensionAndClassifier(
                           repoName, groupId, artifactName, version, extension, classifier)
                       .execute();
      } else {
        response = nexusThreeRestClient.getMavenAsset(repoName, groupId, artifactName, version).execute();
      }
    }
    return response;
  }

  private void downloadArtifactByUrl(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String delegateId, String taskId, String accountId, ListNotifyResponseData notifyResponseData,
      String artifactName, String artifactUrl) {
    Pair<String, InputStream> pair = downloadArtifactByUrl(nexusConfig, encryptionDetails, artifactName, artifactUrl);
    try {
      artifactCollectionTaskHelper.addDataToResponse(
          pair, artifactUrl, notifyResponseData, delegateId, taskId, accountId);
    } catch (IOException e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  public List<String> getArtifactNames(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoId, String path) throws IOException {
    logger.info("Retrieving Artifact Names");
    List<String> artifactNames = new ArrayList<>();
    logger.info("Retrieving artifact names for repository {}", repoId);
    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig, encryptionDetails);
    Response<Nexus3ComponentResponse> response;
    boolean hasMoreResults = true;
    String continuationToken = null;
    while (hasMoreResults) {
      hasMoreResults = false;
      if (nexusConfig.hasCredentials()) {
        response =
            nexusThreeRestClient
                .getArtifactNames(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())),
                    repoId, path, continuationToken)
                .execute();
      } else {
        response = nexusThreeRestClient.getArtifactNames(repoId, path, continuationToken).execute();
      }
      Set<String> packages = null;
      if (isSuccessful(response)) {
        if (response.body() != null) {
          if (isNotEmpty(response.body().getItems())) {
            packages = response.body()
                           .getItems()
                           .stream()
                           .map(Nexus3ComponentResponse.Component::getName)
                           .collect(Collectors.toSet());
          }
          if (isNotEmpty(packages)) {
            for (String p : packages) {
              if (!artifactNames.contains(p)) {
                artifactNames.add(p);
              }
            }
          }
          if (response.body().getContinuationToken() != null) {
            continuationToken = response.body().getContinuationToken();
            hasMoreResults = true;
          }
        }
      } else {
        throw new WingsException(INVALID_ARTIFACT_SERVER, WingsException.USER)
            .addParam("message", "Failed to fetch the groupIds");
      }
    }
    logger.info("Retrieving Artifact Names success");
    return artifactNames;
  }

  public List<BuildDetails> getVersions(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoId, String groupId, String artifactName, String extension, String classifier) throws IOException {
    logger.info("Retrieving versions for repoId {} groupId {} and artifactName {}", repoId, groupId, artifactName);
    List<String> versions = new ArrayList<>();
    Map<String, String> versionToArtifactUrls = new HashMap<>();
    Map<String, List<ArtifactFileMetadata>> versionToArtifactDownloadUrls = new HashMap<>();
    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig, encryptionDetails);
    Response<Nexus3ComponentResponse> response;
    boolean hasMoreResults = true;
    String continuationToken = null;
    while (hasMoreResults) {
      hasMoreResults = false;
      if (nexusConfig.hasCredentials()) {
        response = nexusThreeRestClient
                       .getArtifactVersions(
                           Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), repoId,
                           groupId, artifactName, continuationToken)
                       .execute();
      } else {
        response = nexusThreeRestClient.getArtifactVersions(repoId, groupId, artifactName, continuationToken).execute();
      }
      if (isSuccessful(response)) {
        if (response.body() != null) {
          if (isNotEmpty(response.body().getItems())) {
            for (Nexus3ComponentResponse.Component component : response.body().getItems()) {
              String version = component.getVersion();
              versions.add(version); // todo: add limit if results are returned in descending order of lastUpdatedTs
              // get artifact urls for each version
              Response<Nexus3AssetResponse> versionResponse = getNexus3MavenAssets(
                  nexusConfig, version, groupId, artifactName, repoId, extension, classifier, nexusThreeRestClient);
              List<ArtifactFileMetadata> artifactFileMetadata = getDownloadUrlsForMaven(versionResponse);

              if (isNotEmpty(artifactFileMetadata)) {
                versionToArtifactUrls.put(version, artifactFileMetadata.get(0).getUrl());
              }
              versionToArtifactDownloadUrls.put(version, artifactFileMetadata);
            }
          }
          if (response.body().getContinuationToken() != null) {
            continuationToken = response.body().getContinuationToken();
            hasMoreResults = true;
          }
        }
      } else {
        throw new WingsException(INVALID_ARTIFACT_SERVER, WingsException.USER)
            .addParam("message",
                "Failed to fetch the versions for groupId [" + groupId + "] and artifactId [" + artifactName + "]");
      }
    }
    return nexusHelper.constructBuildDetails(
        repoId, groupId, artifactName, versions, versionToArtifactUrls, versionToArtifactDownloadUrls);
  }

  private List<ArtifactFileMetadata> getDownloadUrlsForMaven(Response<Nexus3AssetResponse> response) {
    List<ArtifactFileMetadata> artifactFileMetadata = new ArrayList<>();
    if (isSuccessful(response)) {
      if (response.body() != null && isNotEmpty(response.body().getItems())) {
        for (Asset item : response.body().getItems()) {
          String url = item.getDownloadUrl();
          String artifactFileName = url.substring(url.lastIndexOf('/') + 1);
          if (artifactFileName.endsWith("pom") || artifactFileName.endsWith("md5")
              || artifactFileName.endsWith("sha1")) {
            continue;
          }
          artifactFileMetadata.add(ArtifactFileMetadata.builder().fileName(artifactFileName).url(url).build());
        }
      }
    }
    return artifactFileMetadata;
  }

  public boolean existsVersion(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId,
      String groupId, String artifactName, String extension, String classifier) throws IOException {
    logger.info("Retrieving versions for repoId {} groupId {} and artifactName {}", repoId, groupId, artifactName);
    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig, encryptionDetails);
    Response<Nexus3ComponentResponse> response;
    boolean hasMoreResults = true;
    String continuationToken = null;
    while (hasMoreResults) {
      hasMoreResults = false;
      if (nexusConfig.hasCredentials()) {
        response = nexusThreeRestClient
                       .getArtifactVersionsWithExtensionAndClassifier(
                           Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), repoId,
                           groupId, artifactName, extension, classifier, continuationToken)
                       .execute();
      } else {
        response = nexusThreeRestClient
                       .getArtifactVersionsWithExtensionAndClassifier(
                           repoId, groupId, artifactName, extension, classifier, continuationToken)
                       .execute();
      }
      if (isSuccessful(response)) {
        if (response.body() != null) {
          if (isEmpty(response.body().getItems())) {
            throw new ArtifactServerException(
                "No versions found matching the provided extension/ classifier", null, WingsException.USER);
          }
          if (response.body().getContinuationToken() != null) {
            continuationToken = response.body().getContinuationToken();
            hasMoreResults = true;
          }
        }
      }
    }
    return true;
  }

  public boolean isServerValid(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails)
      throws IOException {
    logger.info("Validate if nexus is running by retrieving repositories");
    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig, encryptionDetails);
    Response<List<Nexus3Repository>> response;
    if (nexusConfig.hasCredentials()) {
      response =
          nexusThreeRestClient
              .listRepositories(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())))
              .execute();
    } else {
      response = nexusThreeRestClient.listRepositories().execute();
    }
    if (response == null) {
      return false;
    }

    if (response.code() == 404) {
      throw new InvalidArtifactServerException("Invalid Artifact server");
    }
    return isSuccessful(response);
  }

  @SuppressWarnings({"squid:S3510"})
  public Pair<String, InputStream> downloadArtifactByUrl(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String artifactName, String artifactUrl) {
    try {
      if (nexusConfig.hasCredentials()) {
        encryptionService.decrypt(nexusConfig, encryptionDetails);
        Authenticator.setDefault(new NexusThreeServiceImpl.MyAuthenticator(
            nexusConfig.getUsername(), new String(nexusConfig.getPassword())));
      }
      URL url = new URL(artifactUrl);
      URLConnection conn = url.openConnection();
      if (conn instanceof HttpsURLConnection) {
        HttpsURLConnection conn1 = (HttpsURLConnection) url.openConnection();
        conn1.setHostnameVerifier((hostname, session) -> true);
        conn1.setSSLSocketFactory(Http.getSslContext().getSocketFactory());
        return ImmutablePair.of(artifactName, conn1.getInputStream());
      } else {
        return ImmutablePair.of(artifactName, conn.getInputStream());
      }
    } catch (IOException ex) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(ex), ex);
    }
  }

  public long getFileSize(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String artifactName, String artifactUrl) {
    logger.info("Getting file size for artifact at path {}", artifactUrl);
    long size;
    Pair<String, InputStream> pair = downloadArtifactByUrl(nexusConfig, encryptionDetails, artifactName, artifactUrl);
    if (pair == null) {
      throw new InvalidArtifactServerException(format("Failed to get file size for artifact: [%s]", artifactUrl));
    }
    try {
      size = StreamUtils.getInputStreamSize(pair.getRight());
      pair.getRight().close();
    } catch (IOException e) {
      throw new InvalidArtifactServerException(ExceptionUtils.getMessage(e), e);
    }
    logger.info(format("Computed file size: [%d] bytes for artifact Path: [%s]", size, artifactUrl));
    return size;
  }

  static class MyAuthenticator extends Authenticator {
    private String username, password;

    MyAuthenticator(String user, String pass) {
      username = user;
      password = pass;
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
      return new PasswordAuthentication(username, password.toCharArray());
    }
  }
}
