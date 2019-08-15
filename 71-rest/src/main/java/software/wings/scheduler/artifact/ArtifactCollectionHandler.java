package software.wings.scheduler.artifact;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.mongo.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.Permit;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.service.impl.PermitServiceImpl;
import software.wings.service.intfc.ArtifactCollectionService;
import software.wings.service.intfc.PermitService;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ArtifactCollectionHandler implements Handler<ArtifactStream> {
  public static final String GROUP = "ARTIFACT_STREAM_CRON_GROUP";

  @Inject private PermitService permitService;

  @Inject @Named("AsyncArtifactCollectionService") private ArtifactCollectionService artifactCollectionServiceAsync;

  @Override
  public void handle(ArtifactStream artifactStream) {
    logger.info("Received the artifact collection for ArtifactStreamId {}", artifactStream.getUuid());
    executeInternal(artifactStream);
  }

  private void executeInternal(ArtifactStream artifactStream) {
    String artifactStreamId = artifactStream.getUuid();
    if (artifactStream.getFailedCronAttempts() > PermitServiceImpl.MAX_FAILED_ATTEMPTS) {
      logger.warn(
          "ASYNC_ARTIFACT_CRON: Artifact collection disabled for artifactStream:[id:{}, type:{}] due to too many failures [{}]",
          artifactStreamId, artifactStream.getArtifactStreamType(), artifactStream.getFailedCronAttempts());
      return;
    }

    try {
      int leaseDuration = (int) (TimeUnit.MINUTES.toMillis(1)
          * PermitServiceImpl.getBackoffMultiplier(artifactStream.getFailedCronAttempts()));
      String permitId = permitService.acquirePermit(Permit.builder()
                                                        .appId(artifactStream.fetchAppId())
                                                        .group(GROUP)
                                                        .key(artifactStreamId)
                                                        .expireAt(new Date(System.currentTimeMillis() + leaseDuration))
                                                        .leaseDuration(leaseDuration)
                                                        .build());
      if (isNotEmpty(permitId)) {
        logger.info("Permit [{}] acquired for artifactStream [id: {}, failedCount: {}] for [{}] minutes", permitId,
            artifactStream.getUuid(), artifactStream.getFailedCronAttempts(),
            TimeUnit.MILLISECONDS.toMinutes(leaseDuration));
        artifactCollectionServiceAsync.collectNewArtifactsAsync(artifactStream, permitId);
      } else {
        logger.info("Permit already exists for artifactStreamId[{}]", artifactStreamId);
      }
    } catch (WingsException exception) {
      logger.warn(
          "Failed to collect artifacts for artifact stream {}. Reason {}", artifactStreamId, exception.getMessage());
      exception.addContext(Account.class, artifactStream.getAccountId());
      exception.addContext(ArtifactStream.class, artifactStreamId);
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception e) {
      logger.warn("Failed to collect artifacts for artifactStream {}. Reason {}", artifactStreamId, e.getMessage());
    }
  }
}
