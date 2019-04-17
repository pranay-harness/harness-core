package software.wings.delegatetasks.buildsource;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;

import com.google.inject.Inject;

import io.harness.delegate.beans.ResponseData;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.waiter.ErrorNotifyResponseData;
import io.harness.waiter.NotifyCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BuildSourceCleanupCallback implements NotifyCallback {
  private String accountId;
  private String appId;
  private String artifactStreamId;
  private List<BuildDetails> builds;

  @Inject private transient ArtifactService artifactService;
  @Inject private transient ArtifactStreamService artifactStreamService;

  private static final Logger logger = LoggerFactory.getLogger(BuildSourceCleanupCallback.class);

  public BuildSourceCleanupCallback(String accountId, String appId, String artifactStreamId) {
    this.accountId = accountId;
    this.appId = appId;
    this.artifactStreamId = artifactStreamId;
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    ResponseData notifyResponseData = response.values().iterator().next();
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    if (notifyResponseData instanceof BuildSourceExecutionResponse) {
      if (SUCCESS.equals(((BuildSourceExecutionResponse) notifyResponseData).getCommandExecutionStatus())) {
        BuildSourceExecutionResponse buildSourceExecutionResponse = (BuildSourceExecutionResponse) notifyResponseData;
        if (buildSourceExecutionResponse.getBuildSourceResponse() != null) {
          builds = buildSourceExecutionResponse.getBuildSourceResponse().getBuildDetails();
        } else {
          logger.warn(
              "ASYNC_ARTIFACT_CLEANUP: null BuildSourceResponse in buildSourceExecutionResponse:[{}] for artifactStreamId [{}]",
              buildSourceExecutionResponse, artifactStreamId);
        }
        try {
          List<Artifact> artifacts = processBuilds(artifactStream);
          if (isNotEmpty(artifacts)) {
            logger.info("[{}] artifacts deleted for artifactStreamId {}",
                artifacts.stream().map(Artifact::getBuildNo).collect(Collectors.toList()), artifactStream.getUuid());
          }
        } catch (WingsException ex) {
          ex.addContext(Application.class, appId);
          ex.addContext(ArtifactStream.class, artifactStreamId);
          ExceptionLogger.logProcessedMessages(ex, MANAGER, logger);
        }
      } else {
        logger.info("Request for artifactStreamId:[{}] failed :[{}]", artifactStreamId,
            ((BuildSourceExecutionResponse) notifyResponseData).getErrorMessage());
      }
    } else {
      notifyError(response);
    }
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    ResponseData notifyResponseData = response.values().iterator().next();
    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      logger.info("Request for artifactStreamId:[{}] failed :[{}]", artifactStreamId,
          ((ErrorNotifyResponseData) notifyResponseData).getErrorMessage());
    } else {
      logger.error("Unexpected  notify response:[{}] during artifact collection for artifactStreamId {} ", response,
          artifactStreamId);
    }
  }

  private List<Artifact> processBuilds(ArtifactStream artifactStream) {
    List<Artifact> deletedArtifacts = new ArrayList<>();
    if (artifactStream == null) {
      logger.info("Artifact Stream {} does not exist. Returning", artifactStreamId);
      return deletedArtifacts;
    }
    String artifactStreamType = artifactStream.getArtifactStreamType();
    if (DOCKER.name().equals(artifactStreamType)) {
      cleanupDockerArtifacts(artifactStream, deletedArtifacts);
    }
    return deletedArtifacts;
  }

  private void cleanupDockerArtifacts(ArtifactStream artifactStream, List<Artifact> deletedArtifacts) {
    Set<String> buildNumbers = builds.parallelStream().map(BuildDetails::getNumber).collect(Collectors.toSet());
    List<Artifact> deletedArtifactsNew = new ArrayList<>();
    artifactService.prepareArtifactWithMetadataQuery(artifactStream).fetch().forEach(artifact -> {
      if (!buildNumbers.contains(artifact.getBuildNo())) {
        deletedArtifactsNew.add(artifact);
      }
    });

    if (isEmpty(deletedArtifactsNew)) {
      return;
    }

    artifactService.deleteArtifacts(appId, deletedArtifactsNew);
    deletedArtifacts.addAll(deletedArtifactsNew);
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getArtifactStreamId() {
    return artifactStreamId;
  }

  public void setArtifactStreamId(String artifactStreamId) {
    this.artifactStreamId = artifactStreamId;
  }

  public List<BuildDetails> getBuilds() {
    return builds;
  }

  public void setBuilds(List<BuildDetails> builds) {
    this.builds = builds;
  }
}
