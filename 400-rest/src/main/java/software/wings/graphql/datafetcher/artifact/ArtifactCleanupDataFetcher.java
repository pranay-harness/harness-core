package software.wings.graphql.datafetcher.artifact;

import static io.harness.exception.WingsException.ExecutionContext.MANAGER;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.workers.background.iterator.ArtifactCleanupHandler;

import software.wings.beans.Account;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.artifact.ArtifactCleanUpPayload;
import software.wings.graphql.schema.mutation.artifact.ArtifactCleanupInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.impl.artifact.ArtifactCleanupServiceSyncImpl;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class ArtifactCleanupDataFetcher extends BaseMutatorDataFetcher<ArtifactCleanupInput, ArtifactCleanUpPayload> {
  private ArtifactCleanupServiceSyncImpl artifactCleanupService;
  private ArtifactCleanupHandler artifactCleanupHandler;
  private WingsPersistence wingsPersistence;

  @Inject
  public ArtifactCleanupDataFetcher(ArtifactCleanupServiceSyncImpl artifactCleanupService,
      WingsPersistence wingsPersistence, ArtifactCleanupHandler artifactCleanupHandler) {
    super(ArtifactCleanupInput.class, ArtifactCleanUpPayload.class);
    this.artifactCleanupService = artifactCleanupService;
    this.wingsPersistence = wingsPersistence;
    this.artifactCleanupHandler = artifactCleanupHandler;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected ArtifactCleanUpPayload mutateAndFetch(ArtifactCleanupInput parameter, MutationContext mutationContext) {
    ArtifactStream artifactStream = wingsPersistence.get(ArtifactStream.class, parameter.getArtifactStreamId());
    if (artifactStream == null) {
      return new ArtifactCleanUpPayload("Artifact stream not found for the id: " + parameter.getArtifactStreamId());
    }
    try {
      artifactCleanupHandler.handleManually(artifactStream);
      return new ArtifactCleanUpPayload(
          "Cleanup successful for Artifact stream with id: " + parameter.getArtifactStreamId());
    } catch (WingsException exception) {
      String failureMessage = "Failed to cleanup artifacts for artifact stream. Reason " + exception.getMessage();
      log.warn(failureMessage);
      exception.addContext(Account.class, artifactStream.getAccountId());
      exception.addContext(ArtifactStream.class, parameter.getArtifactStreamId());
      ExceptionLogger.logProcessedMessages(exception, MANAGER, log);
      return new ArtifactCleanUpPayload(failureMessage);
    } catch (Exception e) {
      String failureMessage = "Failed to cleanup artifacts for artifact stream. Reason " + e.getMessage();
      log.warn(failureMessage);
      return new ArtifactCleanUpPayload(failureMessage);
    }
  }
}
