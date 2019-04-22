package software.wings.scheduler;

import static io.harness.exception.WingsException.ExecutionContext.MANAGER;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.mongo.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.service.impl.PermitServiceImpl;
import software.wings.service.intfc.ArtifactCleanupService;

@Slf4j
public class ArtifactCleanupHandler implements Handler<ArtifactStream> {
  public static final String GROUP = "ARTIFACT_STREAM_CRON_GROUP";

  @Inject @Named("AsyncArtifactCleanupService") private ArtifactCleanupService artifactCleanupServiceAsync;

  @Override
  public void handle(ArtifactStream artifactStream) {
    logger.info("Received the artifact cleanup for ArtifactStreamId {}", artifactStream.getUuid());
    executeInternal(artifactStream.getAppId(), artifactStream);
  }

  private void executeInternal(String appId, ArtifactStream artifactStream) {
    String artifactStreamId = artifactStream.getUuid();
    try {
      if (artifactStream.getFailedCronAttempts() > PermitServiceImpl.MAX_FAILED_ATTEMPTS) {
        logger.info("Not running cleanup as artifact collection disabled disabled for artifactStream:[id:{}, type:{}]",
            artifactStreamId, artifactStream.getArtifactStreamType());
        return;
      }
      artifactCleanupServiceAsync.cleanupArtifactsAsync(appId, artifactStream);
    } catch (WingsException exception) {
      logger.warn("Failed to cleanup artifacts for appId {}, artifact stream {}. Reason {}", appId, artifactStreamId,
          exception.getMessage());
      exception.addContext(Application.class, appId);
      exception.addContext(ArtifactStream.class, artifactStreamId);
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception e) {
      logger.warn("Failed to cleanup artifacts for appId {}, artifactStream {}. Reason {}", appId, artifactStreamId,
          e.getMessage());
    }
  }
}
