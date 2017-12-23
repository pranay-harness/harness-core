package software.wings.helpers.ext.nexus;

import static java.lang.String.format;
import static org.awaitility.Awaitility.with;
import static org.hamcrest.CoreMatchers.notNullValue;
import static software.wings.beans.ErrorCode.INVALID_ARTIFACT_SERVER;
import static software.wings.utils.ArtifactType.DOCKER;
import static software.wings.utils.ArtifactType.WAR;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.awaitility.Duration;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.beans.config.NexusConfig;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.ArtifactType;
import software.wings.utils.HttpUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.xml.stream.XMLStreamException;

/**
 * Created by srinivas on 3/28/17.
 */
@Singleton
public class NexusServiceImpl implements NexusService {
  private final static Logger logger = LoggerFactory.getLogger(NexusServiceImpl.class);

  @Inject private NexusThreeServiceImpl nexusThreeService;

  @Inject private NexusTwoServiceImpl nexusTwoService;

  public static void handleException(IOException e) {
    List<ResponseMessage> responseMessages = new ArrayList<>();
    responseMessages.add(prepareResponseMessage(INVALID_ARTIFACT_SERVER, e.getMessage()));
    throw new WingsException(responseMessages, e.getMessage(), e);
  }

  /**
   * prepareResponseMessage
   */
  public static ResponseMessage prepareResponseMessage(final ErrorCode errorCode, final String errorMsg) {
    return ResponseMessage.builder().code(errorCode).errorType(ResponseTypeEnum.ERROR).message(errorMsg).build();
  }

  public static boolean isSuccessful(Response<?> response) {
    if (response == null) {
      return false;
    }
    if (response != null && !response.isSuccessful()) {
      logger.error("Request not successful. Reason: {}", response);
      int code = response.code();
      switch (code) {
        case 404:
          return false;
        case 401:
          throw new WingsException(INVALID_ARTIFACT_SERVER, "message", "Invalid Nexus credentials");
        case 405:
          throw new WingsException(INVALID_ARTIFACT_SERVER, "message", "Method not allowed" + response.message());
        default:
          throw new WingsException(INVALID_ARTIFACT_SERVER, "message", response.message());
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
        .client(HttpUtil.getUnsafeOkHttpClient())
        .build();
  }

  @Override
  public Map<String, String> getRepositories(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails) {
    return getRepositories(nexusConfig, encryptionDetails, WAR);
  }

  public Map<String, String> getRepositories(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, ArtifactType artifactType) {
    try {
      return with().atMost(new Duration(20L, TimeUnit.SECONDS)).until(() -> {
        try {
          if (nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x")) {
            if (!DOCKER.equals(artifactType)) {
              return nexusTwoService.getRepositories(nexusConfig, encryptionDetails);
            }
            throw new WingsException(
                INVALID_ARTIFACT_SERVER, "message", "Nexus 2.x does not support Docker artifact type");
          } else {
            if (DOCKER.equals(artifactType)) {
              return nexusThreeService.getRepositories(nexusConfig, encryptionDetails);
            } else {
              throw new WingsException(INVALID_ARTIFACT_SERVER, "message", "Not supported for Nexus 3.x version");
            }
          }
        } catch (WingsException e) {
          throw e;
        } catch (Exception e) {
          logger.error(
              "Error occurred while retrieving Repositories from Nexus server " + nexusConfig.getNexusUrl(), e);
          if (e.getCause() != null || e.getCause() instanceof XMLStreamException) {
            throw new WingsException(INVALID_ARTIFACT_SERVER, "message", "Nexus may not be running");
          }
          throw new WingsException(
              INVALID_ARTIFACT_SERVER, "message", e.getMessage() == null ? "Unknown error" : e.getMessage());
        }
      }, notNullValue());

    } catch (ConditionTimeoutException e) {
      logger.warn("Nexus server request did not succeed within 20 secs", e);
      throw new WingsException(INVALID_ARTIFACT_SERVER, "message", "Nexus server took too long to respond");
    }
  }

  @Override
  public List<String> getGroupIdPaths(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId) {
    try {
      return with().atMost(new Duration(20L, TimeUnit.SECONDS)).until(() -> {
        try {
          if (nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x")) {
            return nexusTwoService.getGroupIdPaths(nexusConfig, encryptionDetails, repoId);
          } else {
            return nexusThreeService.getDockerImages(nexusConfig, encryptionDetails, repoId);
          }
        } catch (WingsException e) {
          throw e;
        } catch (Exception e) {
          logger.error(
              "Failed to fetch images/groups from Nexus server " + nexusConfig.getNexusUrl() + " under repo " + repoId,
              e);
          if (e.getCause() != null || e.getCause() instanceof XMLStreamException) {
            throw new WingsException(INVALID_ARTIFACT_SERVER, "message", "Nexus may not be running");
          }
          throw new WingsException(
              INVALID_ARTIFACT_SERVER, "message", e.getMessage() == null ? "Unknown error" : e.getMessage());
        }
      }, notNullValue());

    } catch (ConditionTimeoutException e) {
      logger.warn("Nexus server request did not succeed within 20 secs", e);
      throw new WingsException(INVALID_ARTIFACT_SERVER, "message", "Nexus server took too long to respond");
    }
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
  public Pair<String, InputStream> downloadArtifact(NexusConfig nexusConfig,
      List<EncryptedDataDetail> encryptionDetails, String repoType, String groupId, String artifactName,
      String version) {
    try {
      return nexusTwoService.downloadArtifact(nexusConfig, encryptionDetails, repoType, groupId, artifactName, version);
    } catch (IOException e) {
      logger.error("Error occurred while downloading the artifact", e);
      return null;
    }
  }

  @Override
  public Pair<String, InputStream> downloadArtifact(NexusConfig nexusConfig,
      List<EncryptedDataDetail> encryptionDetails, String repoType, String groupId, String artifactName) {
    // First Get the maven pom model
    return downloadArtifact(nexusConfig, encryptionDetails, repoType, groupId, artifactName, null);
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
  public List<BuildDetails> getVersions(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoId, String groupId, String artifactName) {
    try {
      return nexusTwoService.getVersions(nexusConfig, encryptionDetails, repoId, groupId, artifactName);
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
  public BuildDetails getLatestVersion(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoId, String groupId, String artifactName) {
    return nexusTwoService.getLatestVersion(nexusConfig, encryptionDetails, repoId, groupId, artifactName);
  }

  @Override
  public List<BuildDetails> getBuilds(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoKey, String imageName, int maxNumberOfBuilds) {
    try {
      return nexusThreeService.getDockerTags(nexusConfig, encryptionDetails, repoKey, imageName);
    } catch (IOException e) {
      logger.error(format("Error occurred while retrieving tags from Nexus server %s for repository %s under image %s",
                       nexusConfig.getNexusUrl(), repoKey, imageName),
          e);
      handleException(e);
    }
    return new ArrayList<>();
  }

  @Override
  public boolean isRunning(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails) {
    List<String> images = new ArrayList<>();
    if (nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x")) {
      return getRepositories(nexusConfig, Collections.emptyList()) != null;
    } else {
      Map<String, String> repositories;

      try {
        repositories = getRepositories(nexusConfig, encryptionDetails, ArtifactType.DOCKER);
      } catch (WingsException e) {
        if (e.getMessage() != null && e.getMessage().contains("Invalid Nexus credentials")) {
          throw e;
        }
        return true;
      } catch (Exception e) {
        logger.warn(
            "Failed to retrieve repositories. Ignoring validation for Nexus 3 for now. User can give custom path");
        return true;
      }
      Optional<String> repoKey = repositories.keySet().stream().findFirst();
      if (repoKey.isPresent()) {
        images = getGroupIdPaths(nexusConfig, encryptionDetails, repoKey.get());
      }
    }
    return images != null;
  }
}
