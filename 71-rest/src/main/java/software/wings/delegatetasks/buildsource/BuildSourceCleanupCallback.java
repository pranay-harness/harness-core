package software.wings.delegatetasks.buildsource;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;

import com.google.inject.Inject;

import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.persistence.HIterator;
import io.harness.waiter.NotifyCallback;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Slf4j
public class BuildSourceCleanupCallback implements NotifyCallback {
  private String accountId;
  private String artifactStreamId;
  private List<BuildDetails> builds;

  @Inject private transient ArtifactService artifactService;
  @Inject private transient ArtifactStreamService artifactStreamService;

  public BuildSourceCleanupCallback(String accountId, String artifactStreamId) {
    this.accountId = accountId;
    this.artifactStreamId = artifactStreamId;
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    ResponseData notifyResponseData = response.values().iterator().next();
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
    if (notifyResponseData instanceof BuildSourceExecutionResponse) {
      if (SUCCESS.equals(((BuildSourceExecutionResponse) notifyResponseData).getCommandExecutionStatus())) {
        BuildSourceExecutionResponse buildSourceExecutionResponse = (BuildSourceExecutionResponse) notifyResponseData;
        if (buildSourceExecutionResponse.getBuildSourceResponse() != null) {
          builds = buildSourceExecutionResponse.getBuildSourceResponse().getBuildDetails();
        } else {
          logger.warn(
              "ASYNC_ARTIFACT_CLEANUP: null BuildSourceResponse in buildSourceExecutionResponse:[{}] for artifactStreamId [{}]",
              buildSourceExecutionResponse, artifactStreamId);
          return;
        }
        try {
          if (isEmpty(builds)) {
            // Do not do cleanup in case of empty builds.
            return;
          }
          List<Artifact> artifacts = processBuilds(artifactStream);
          if (isNotEmpty(artifacts)) {
            logger.info("[{}] artifacts deleted for artifactStreamId {}",
                artifacts.stream().map(Artifact::getBuildNo).collect(Collectors.toList()), artifactStream.getUuid());
          }
        } catch (WingsException ex) {
          ex.addContext(Account.class, accountId);
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
    Set<String> buildNumbers = isEmpty(builds)
        ? new HashSet<>()
        : builds.parallelStream().map(BuildDetails::getNumber).collect(Collectors.toSet());
    List<Artifact> deletedArtifactsNew = new ArrayList<>();
    try (HIterator<Artifact> artifacts =
             new HIterator<>(artifactService.prepareArtifactWithMetadataQuery(artifactStream).fetch())) {
      for (Artifact artifact : artifacts) {
        if (!buildNumbers.contains(artifact.getBuildNo())) {
          deletedArtifactsNew.add(artifact);
        }
      }
    }

    if (isEmpty(deletedArtifactsNew)) {
      return;
    }

    artifactService.deleteArtifacts(deletedArtifactsNew);
    deletedArtifacts.addAll(deletedArtifactsNew);
  }
}
