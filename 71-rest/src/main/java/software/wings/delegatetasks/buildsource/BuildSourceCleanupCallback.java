package software.wings.delegatetasks.buildsource;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static software.wings.beans.artifact.ArtifactStreamType.ACR;
import static software.wings.beans.artifact.ArtifactStreamType.AMI;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.persistence.HIterator;
import io.harness.waiter.NotifyCallback;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.FeatureName;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FeatureFlagService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@Slf4j
public class BuildSourceCleanupCallback implements NotifyCallback {
  private String accountId;
  private String artifactStreamId;
  private List<BuildDetails> builds;

  @Inject private transient ArtifactService artifactService;
  @Inject FeatureFlagService featureFlagService;
  @Inject private transient ArtifactStreamService artifactStreamService;
  @Inject @Named("BuildSourceCleanupCallbackExecutor") private ExecutorService executorService;
  @Inject private ArtifactCollectionUtils artifactCollectionUtils;

  public BuildSourceCleanupCallback() {}

  public BuildSourceCleanupCallback(String accountId, String artifactStreamId) {
    this.accountId = accountId;
    this.artifactStreamId = artifactStreamId;
  }

  @VisibleForTesting
  void handleResponseForSuccessInternal(ResponseData notifyResponseData, ArtifactStream artifactStream) {
    logger.info("Processing response for BuildSourceCleanupCallback for accountId:[{}] artifactStreamId:[{}]",
        accountId, artifactStreamId);

    BuildSourceExecutionResponse buildSourceExecutionResponse = (BuildSourceExecutionResponse) notifyResponseData;
    if (buildSourceExecutionResponse.getBuildSourceResponse() != null) {
      builds = buildSourceExecutionResponse.getBuildSourceResponse().getBuildDetails();
    } else {
      logger.warn(
          "ASYNC_ARTIFACT_CLEANUP: null BuildSourceResponse in buildSourceExecutionResponse:[{}] for artifactStreamId [{}]",
          buildSourceExecutionResponse, artifactStreamId);
    }
    try {
      if (isEmpty(builds)) {
        logger.warn(
            "ASYNC_ARTIFACT_CLEANUP: Skipping because of empty builds list for accountId:[{}] artifactStreamId:[{}]",
            accountId, artifactStreamId);
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
  }

  private void handleResponseForSuccess(ResponseData notifyResponseData, ArtifactStream artifactStream) {
    try {
      executorService.submit(() -> {
        try {
          handleResponseForSuccessInternal(notifyResponseData, artifactStream);
        } catch (Exception ex) {
          logger.error(
              "Error while processing response for BuildSourceCleanupCallback for accountId:[{}] artifactStreamId:[{}]",
              accountId, artifactStreamId, ex);
        }
      });
    } catch (RejectedExecutionException ex) {
      logger.error("RejectedExecutionException for BuildSourceCleanupCallback for accountId:[{}] artifactStreamId:[{}]",
          accountId, artifactStreamId, ex);
    }
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    ResponseData notifyResponseData = response.values().iterator().next();
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
    if (notifyResponseData instanceof BuildSourceExecutionResponse) {
      if (SUCCESS == ((BuildSourceExecutionResponse) notifyResponseData).getCommandExecutionStatus()) {
        handleResponseForSuccess(notifyResponseData, artifactStream);
      } else {
        logger.info("Request failed :[{}]", ((BuildSourceExecutionResponse) notifyResponseData).getErrorMessage());
      }
    } else {
      notifyError(response);
    }
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    ResponseData notifyResponseData = response.values().iterator().next();
    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      logger.info("Request failed :[{}]", ((ErrorNotifyResponseData) notifyResponseData).getErrorMessage());
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
    } else if (AMI.name().equals(artifactStreamType)) {
      cleanupAMIArtifacts(artifactStream, deletedArtifacts);
    } else if (ARTIFACTORY.name().equals(artifactStreamType) || GCR.name().equals(artifactStreamType)
        || ECR.name().equals(artifactStreamType) || ACR.name().equals(artifactStreamType)
        || NEXUS.name().equals(artifactStreamType)) {
      // This might not work for Nexus as we are also calling update nexus status
      List<Artifact> deletedArtifactsNew = cleanupStaleArtifacts(artifactStream, builds);
      deletedArtifacts.addAll(deletedArtifactsNew);
    }

    return deletedArtifacts;
  }

  private List<Artifact> cleanupStaleArtifacts(ArtifactStream artifactStream, List<BuildDetails> buildDetails) {
    logger.info("Artifact Stream {} cleanup started with type {} name {}", artifactStreamId,
        artifactStream.getArtifactStreamType(), artifactStream.getSourceName());
    ArtifactStreamAttributes artifactStreamAttributes =
        artifactCollectionUtils.getArtifactStreamAttributes(artifactStream,
            featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, artifactStream.getAccountId()));
    Map<String, BuildDetails> buildDetailsMap;
    if (isEmpty(buildDetails)) {
      buildDetailsMap = Collections.emptyMap();
    } else {
      Function<BuildDetails, String> buildDetailsKeyFn = ArtifactCollectionUtils.getBuildDetailsKeyFn(
          artifactStream.getArtifactStreamType(), artifactStreamAttributes);
      buildDetailsMap = buildDetails.stream().collect(Collectors.toMap(buildDetailsKeyFn, Function.identity()));
    }

    Function<Artifact, String> artifactKeyFn =
        ArtifactCollectionUtils.getArtifactKeyFn(artifactStream.getArtifactStreamType(), artifactStreamAttributes);
    List<Artifact> toBeDeletedArtifacts = new ArrayList<>();
    try (HIterator<Artifact> artifactHIterator =
             new HIterator<>(artifactService.prepareCleanupQuery(artifactStream).fetch())) {
      for (Artifact artifact : artifactHIterator) {
        if (!buildDetailsMap.containsKey(artifactKeyFn.apply(artifact))) {
          toBeDeletedArtifacts.add(artifact);
        }
      }
    }

    artifactService.deleteArtifacts(toBeDeletedArtifacts);

    logger.info("Artifact Stream {} cleanup complete with type {}, count {}", artifactStreamId,
        artifactStream.getArtifactStreamType(), toBeDeletedArtifacts.size());
    return toBeDeletedArtifacts;
  }

  private void cleanupDockerArtifacts(ArtifactStream artifactStream, List<Artifact> deletedArtifacts) {
    Set<String> buildNumbers =
        isEmpty(builds) ? new HashSet<>() : builds.stream().map(BuildDetails::getNumber).collect(Collectors.toSet());
    List<Artifact> deletedArtifactsNew = new ArrayList<>();
    try (HIterator<Artifact> artifacts = new HIterator<>(artifactService.prepareCleanupQuery(artifactStream).fetch())) {
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

  private void cleanupAMIArtifacts(ArtifactStream artifactStream, List<Artifact> deletedArtifacts) {
    Set<String> revisionNumbers =
        isEmpty(builds) ? new HashSet<>() : builds.stream().map(BuildDetails::getRevision).collect(Collectors.toSet());
    List<Artifact> artifactsToBeDeleted = new ArrayList<>();
    try (HIterator<Artifact> artifacts = new HIterator<>(artifactService.prepareCleanupQuery(artifactStream).fetch())) {
      for (Artifact artifact : artifacts) {
        if (artifact != null && (artifact.getRevision() != null) && !revisionNumbers.contains(artifact.getRevision())) {
          artifactsToBeDeleted.add(artifact);
        }
      }
    }

    if (isEmpty(artifactsToBeDeleted)) {
      return;
    }

    artifactService.deleteArtifacts(artifactsToBeDeleted);
    deletedArtifacts.addAll(artifactsToBeDeleted);
  }
}
