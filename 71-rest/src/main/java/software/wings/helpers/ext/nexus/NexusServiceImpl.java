package software.wings.helpers.ext.nexus;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.ARTIFACT_SERVER_ERROR;
import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.network.Http;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.waiter.ListNotifyResponseData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.config.NexusConfig;
import software.wings.exception.InvalidArtifactServerException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.utils.ArtifactType;
import software.wings.utils.RepositoryFormat;
import software.wings.utils.RepositoryType;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.xml.stream.XMLStreamException;

/**
 * Created by srinivas on 3/28/17.
 */
@Singleton
@Slf4j
public class NexusServiceImpl implements NexusService {
  @Inject private NexusThreeServiceImpl nexusThreeService;
  @Inject private NexusTwoServiceImpl nexusTwoService;
  @Inject private TimeLimiter timeLimiter;

  public static void handleException(IOException e) {
    throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", ExceptionUtils.getMessage(e));
  }

  public static boolean isSuccessful(Response<?> response) {
    if (response == null) {
      return false;
    }
    if (!response.isSuccessful()) {
      logger.error("Request not successful. Reason: {}", response);
      int code = response.code();
      switch (code) {
        case 404:
          return false;
        case 401:
          throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", "Invalid Nexus credentials");
        case 405:
          throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
              .addParam("message", "Method not allowed" + response.message());
        default:
          throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", response.message());
      }
    }
    return true;
  }

  public static String getBaseUrl(NexusConfig nexusConfig) {
    return nexusConfig.getNexusUrl().endsWith("/") ? nexusConfig.getNexusUrl() : nexusConfig.getNexusUrl() + "/";
  }

  public static Retrofit getRetrofit(String baseUrl, Converter.Factory converterFactory) {
    return new Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(converterFactory)
        .client(Http.getUnsafeOkHttpClient(baseUrl))
        .build();
  }

  @Override
  public Map<String, String> getRepositories(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails) {
    return getRepositories(nexusConfig, encryptionDetails, null);
  }

  @Override
  public Map<String, String> getRepositories(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repositoryFormat) {
    try {
      boolean isNexusTwo = nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x");
      return timeLimiter.callWithTimeout(() -> {
        if (isNexusTwo) {
          if (RepositoryFormat.docker.name().equals(repositoryFormat)) {
            throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
                .addParam("message", "Nexus 2.x does not support Docker artifact type");
          }
          return nexusTwoService.getRepositories(nexusConfig, encryptionDetails, repositoryFormat);
        } else {
          if (repositoryFormat == null) {
            throw new InvalidRequestException("Not supported for nexus 3.x", USER);
          }
          return nexusThreeService.getRepositories(nexusConfig, encryptionDetails, repositoryFormat);
        }
      }, 20L, TimeUnit.SECONDS, true);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Error occurred while retrieving Repositories from Nexus server " + nexusConfig.getNexusUrl(), e);
      if (e.getCause() != null && e.getCause() instanceof XMLStreamException) {
        throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", "Nexus may not be running");
      }
      return emptyMap();
    }
  }

  @Override
  public List<String> getGroupIdPaths(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId, String repositoryFormat) {
    List<String> groupIds = new ArrayList<>();
    try {
      boolean isNexusTwo = nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x");
      if (isNexusTwo) {
        timeLimiter.callWithTimeout(
            ()
                -> nexusTwoService.collectGroupIds(nexusConfig, encryptionDetails, repoId, groupIds, repositoryFormat),
            20L, TimeUnit.SECONDS, true);
      } else {
        if (repositoryFormat != null) {
          switch (repositoryFormat) {
            case "nuget":
            case "npm":
              return timeLimiter.callWithTimeout(()
                                                     -> nexusThreeService.getPackageNames(nexusConfig,
                                                         encryptionDetails, repoId, repositoryFormat, groupIds),
                  20L, TimeUnit.SECONDS, true);
            case "maven":
              return timeLimiter.callWithTimeout(()
                                                     -> nexusThreeService.getGroupIds(nexusConfig, encryptionDetails,
                                                         repoId, repositoryFormat, groupIds),
                  20L, TimeUnit.SECONDS, true);
            case "docker":
              return timeLimiter.callWithTimeout(
                  ()
                      -> nexusThreeService.getDockerImages(nexusConfig, encryptionDetails, repoId, groupIds),
                  20L, TimeUnit.SECONDS, true);
            default:
              throw new WingsException("Unsupported repositoryFormat for Nexus 3.x");
          }
        } else {
          // for backward compatibility  with old UI when repositoryFormat is null
          return timeLimiter.callWithTimeout(
              ()
                  -> nexusThreeService.getDockerImages(nexusConfig, encryptionDetails, repoId, groupIds),
              20L, TimeUnit.SECONDS, true);
        }
      }
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      logger.error(
          "Failed to fetch images/groups from Nexus server " + nexusConfig.getNexusUrl() + " under repo " + repoId, e);
      if (e.getCause() != null && e.getCause() instanceof XMLStreamException) {
        throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", "Nexus may not be running");
      }
    }
    return groupIds;
  }

  @Override
  public List<String> getArtifactPaths(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId) {
    try {
      return nexusTwoService.getArtifactPaths(nexusConfig, encryptionDetails, repoId);
    } catch (final IOException e) {
      logger.error("Error occurred while retrieving repository contents from Nexus Server " + nexusConfig.getNexusUrl()
              + " for repository " + repoId,
          e);
      handleException(e);
    }
    return new ArrayList<>();
  }

  @Override
  public List<String> getArtifactPaths(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId, String name) {
    try {
      return nexusTwoService.getArtifactPaths(nexusConfig, encryptionDetails, repoId, name);
    } catch (final IOException e) {
      logger.error("Error occurred while retrieving Artifact paths from Nexus server " + nexusConfig.getNexusUrl()
              + " for Repository " + repoId,
          e);
      handleException(e);
    }
    return new ArrayList<>();
  }

  @Override
  public Pair<String, InputStream> downloadArtifacts(NexusConfig nexusConfig,
      List<EncryptedDataDetail> encryptionDetails, ArtifactStreamAttributes artifactStreamAttributes,
      Map<String, String> artifactMetadata, String delegateId, String taskId, String accountId,
      ListNotifyResponseData notifyResponseData) {
    try {
      boolean isNexusTwo = nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x");
      if (isNexusTwo) {
        return nexusTwoService.downloadArtifact(nexusConfig, encryptionDetails, artifactStreamAttributes,
            artifactMetadata, delegateId, taskId, accountId, notifyResponseData);
      } else {
        return nexusThreeService.downloadArtifact(nexusConfig, encryptionDetails, artifactStreamAttributes,
            artifactMetadata, delegateId, taskId, accountId, notifyResponseData);
      }
    } catch (IOException e) {
      logger.error("Error occurred while downloading the artifact", e);
      throw new WingsException(ARTIFACT_SERVER_ERROR, USER).addParam("message", ExceptionUtils.getMessage(e));
    }
  }

  @Override
  public List<String> getArtifactNames(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId, String path) {
    try {
      return nexusTwoService.getArtifactNames(nexusConfig, encryptionDetails, repoId, path);
    } catch (final IOException e) {
      logger.error(
          format("Error occurred while retrieving artifact names from Nexus server %s for Repository %s under path %s",
              nexusConfig.getNexusUrl(), repoId, path),
          e);
      handleException(e);
    }
    return new ArrayList<>();
  }

  @Override
  public List<String> getArtifactNames(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoId, String path, String repositoryFormat) {
    try {
      if (repositoryFormat.equals(RepositoryFormat.maven.name())) {
        return nexusThreeService.getArtifactNames(nexusConfig, encryptionDetails, repoId, path);
      }
    } catch (final IOException e) {
      logger.error(
          format("Error occurred while retrieving artifact names from Nexus server %s for Repository %s under path %s",
              nexusConfig.getNexusUrl(), repoId, path),
          e);
      handleException(e);
    }
    return new ArrayList<>();
  }

  @Override
  public List<BuildDetails> getVersions(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoId, String groupId, String artifactName, String extension, String classifier) {
    try {
      boolean isNexusTwo = nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x");
      if (isNexusTwo) {
        return nexusTwoService.getVersions(
            nexusConfig, encryptionDetails, repoId, groupId, artifactName, extension, classifier);
      } else {
        return nexusThreeService.getVersions(
            nexusConfig, encryptionDetails, repoId, groupId, artifactName, extension, classifier);
      }
    } catch (final IOException e) {
      logger.error(
          format(
              "Error occurred while retrieving versions from Nexus server %s for Repository %s under group id %s and artifact name %s",
              nexusConfig.getNexusUrl(), repoId, groupId, artifactName),
          e);
      handleException(e);
    }
    return new ArrayList<>();
  }

  @Override
  public boolean existsVersion(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId,
      String groupId, String artifactName, String extension, String classifier) {
    if (isEmpty(extension) && isEmpty(classifier)) {
      return true;
    }
    try {
      boolean isNexusTwo = nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x");
      if (isNexusTwo) {
        return nexusTwoService.existsVersion(
            nexusConfig, encryptionDetails, repoId, groupId, artifactName, extension, classifier);
      } else {
        return nexusThreeService.existsVersion(
            nexusConfig, encryptionDetails, repoId, groupId, artifactName, extension, classifier);
      }
    } catch (final IOException e) {
      logger.error(
          format(
              "Error occurred while retrieving versions from Nexus server %s for Repository %s under group id %s and artifact name %s",
              nexusConfig.getNexusUrl(), repoId, groupId, artifactName),
          e);
      handleException(e);
    }
    return true;
  }

  @Override
  public List<BuildDetails> getVersions(String repositoryFormat, NexusConfig nexusConfig,
      List<EncryptedDataDetail> encryptionDetails, String repoId, String packageName) {
    try {
      boolean isNexusTwo = nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x");
      if (isNexusTwo) {
        return nexusTwoService.getVersions(repositoryFormat, nexusConfig, encryptionDetails, repoId, packageName);
      } else {
        return nexusThreeService.getPackageVersions(nexusConfig, encryptionDetails, repoId, packageName);
      }
    } catch (final IOException e) {
      logger.error(
          format("Error occurred while retrieving versions from Nexus server %s for Repository %s under package %s",
              nexusConfig.getNexusUrl(), repoId, packageName),
          e);
      handleException(e);
    }
    return new ArrayList<>();
  }

  @Override
  public BuildDetails getLatestVersion(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoId, String groupId, String artifactName) {
    return nexusTwoService.getLatestVersion(nexusConfig, encryptionDetails, repoId, groupId, artifactName);
  }

  @Override
  public List<BuildDetails> getBuilds(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes, int maxNumberOfBuilds) {
    try {
      if ((artifactStreamAttributes.getArtifactType() != null
              && artifactStreamAttributes.getArtifactType().equals(ArtifactType.DOCKER))
          || (artifactStreamAttributes.getRepositoryType() != null
                 && artifactStreamAttributes.getRepositoryType().equals(RepositoryType.docker.name()))) {
        return nexusThreeService.getDockerTags(nexusConfig, encryptionDetails, artifactStreamAttributes);
      }
    } catch (IOException e) {
      logger.error("Error occurred while retrieving tags from Nexus server {} for repository {} under image {}",
          nexusConfig.getNexusUrl(), artifactStreamAttributes.getJobName(), artifactStreamAttributes.getImageName(), e);
      handleException(e);
    }
    return new ArrayList<>();
  }

  @Override
  public boolean isRunning(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails) {
    if (nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x")) {
      return getRepositories(nexusConfig, encryptionDetails, null) != null;
    } else {
      try {
        return nexusThreeService.isServerValid(nexusConfig, encryptionDetails);
      } catch (InvalidArtifactServerException e) {
        throw e;
      } catch (WingsException e) {
        if (ExceptionUtils.getMessage(e).contains("Invalid Nexus credentials")) {
          throw e;
        }
        return true;
      } catch (Exception e) {
        logger.warn(
            "Failed to retrieve repositories. Ignoring validation for Nexus 3 for now. User can give custom path");
        return true;
      }
    }
  }
}
