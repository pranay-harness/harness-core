package software.wings.helpers.ext.nexus;

import static io.harness.eraro.ErrorCode.ARTIFACT_SERVER_ERROR;
import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static software.wings.utils.ArtifactType.DOCKER;
import static software.wings.utils.ArtifactType.WAR;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.network.Http;
import io.harness.waiter.ListNotifyResponseData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.config.NexusConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.ArtifactType;

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
    return getRepositories(nexusConfig, encryptionDetails, WAR);
  }

  public Map<String, String> getRepositories(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, ArtifactType artifactType) {
    try {
      boolean isNexusTwo = nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x");
      return timeLimiter.callWithTimeout(() -> {
        if (isNexusTwo) {
          if (!DOCKER.equals(artifactType)) {
            return nexusTwoService.getRepositories(nexusConfig, encryptionDetails);
          }
          throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
              .addParam("message", "Nexus 2.x does not support Docker artifact type");
        } else {
          if (DOCKER.equals(artifactType)) {
            return nexusThreeService.getRepositories(nexusConfig, encryptionDetails);
          } else {
            throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
                .addParam("message", "Not supported for Nexus 3.x version");
          }
        }
      }, 20L, TimeUnit.SECONDS, true);
    } catch (UncheckedTimeoutException e) {
      logger.warn("Nexus server request did not succeed within 20 secs");
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
          .addParam("message", "Nexus server took too long to respond");
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Error occurred while retrieving Repositories from Nexus server " + nexusConfig.getNexusUrl(), e);
      if (e.getCause() != null && e.getCause() instanceof XMLStreamException) {
        throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", "Nexus may not be running");
      }
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", ExceptionUtils.getMessage(e));
    }
  }

  @Override
  public List<String> getGroupIdPaths(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId) {
    List<String> groupIds = new ArrayList<>();
    try {
      boolean isNexusTwo = nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x");
      timeLimiter.callWithTimeout(()
                                      -> isNexusTwo
              ? nexusTwoService.collectGroupIds(nexusConfig, encryptionDetails, repoId, groupIds)
              : nexusThreeService.getDockerImages(nexusConfig, encryptionDetails, repoId, groupIds),
          20L, TimeUnit.SECONDS, true);
    } catch (UncheckedTimeoutException e) {
      logger.warn("Nexus server request did not succeed within 20 secs. Returning the list so far", e);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      logger.error(
          "Failed to fetch images/groups from Nexus server " + nexusConfig.getNexusUrl() + " under repo " + repoId, e);
      if (e.getCause() != null && e.getCause() instanceof XMLStreamException) {
        throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", "Nexus may not be running");
      }
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", ExceptionUtils.getMessage(e));
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
      List<EncryptedDataDetail> encryptionDetails, String repoType, String groupId, String artifactName, String version,
      String delegateId, String taskId, String accountId, ListNotifyResponseData notifyResponseData) {
    try {
      return nexusTwoService.downloadArtifact(nexusConfig, encryptionDetails, repoType, groupId, artifactName, version,
          delegateId, taskId, accountId, notifyResponseData);
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
      ArtifactStreamAttributes artifactStreamAttributes, int maxNumberOfBuilds) {
    try {
      return nexusThreeService.getDockerTags(nexusConfig, encryptionDetails, artifactStreamAttributes);
    } catch (IOException e) {
      logger.error(format("Error occurred while retrieving tags from Nexus server %s for repository %s under image %s",
                       nexusConfig.getNexusUrl(), artifactStreamAttributes.getJobName(),
                       artifactStreamAttributes.getImageName()),
          e);
      handleException(e);
    }
    return new ArrayList<>();
  }

  @Override
  public boolean isRunning(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails) {
    if (nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x")) {
      return getRepositories(nexusConfig, encryptionDetails) != null;
    } else {
      try {
        return getRepositories(nexusConfig, encryptionDetails, ArtifactType.DOCKER) != null;
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
