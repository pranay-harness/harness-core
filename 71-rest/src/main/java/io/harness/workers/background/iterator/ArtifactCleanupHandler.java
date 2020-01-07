package io.harness.workers.background.iterator;

import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;
import static java.util.Arrays.asList;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.exception.WingsException;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIterator.ProcessMode;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.logging.ExceptionLogger;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamKeys;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.ArtifactCleanupService;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ArtifactCleanupHandler implements Handler<ArtifactStream> {
  public static final String GROUP = "ARTIFACT_STREAM_CRON_GROUP";

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject @Named("AsyncArtifactCleanupService") private ArtifactCleanupService artifactCleanupServiceAsync;
  @Inject ArtifactCollectionUtils artifactCollectionUtils;

  public void registerIterators(ScheduledThreadPoolExecutor artifactCollectionExecutor) {
    PersistenceIterator iterator = persistenceIteratorFactory.createIterator(ArtifactCleanupHandler.class,
        MongoPersistenceIterator.<ArtifactStream>builder()
            .clazz(ArtifactStream.class)
            .fieldName(ArtifactStreamKeys.nextCleanupIteration)
            .targetInterval(ofHours(2))
            .acceptableNoAlertDelay(ofMinutes(15))
            .executorService(artifactCollectionExecutor)
            .semaphore(new Semaphore(5))
            .handler(this)
            .filterExpander(query
                -> query.field(ArtifactStreamKeys.artifactStreamType)
                       .in(asList(ArtifactStreamType.DOCKER.name(), ArtifactStreamType.AMI.name(),
                           ArtifactStreamType.ARTIFACTORY.name(), ArtifactStreamType.ECR.name(),
                           ArtifactStreamType.GCR.name(), ArtifactStreamType.ACR.name(),
                           ArtifactStreamType.NEXUS.name())))
            .schedulingType(REGULAR)
            .redistribute(true));

    if (iterator != null) {
      artifactCollectionExecutor.scheduleAtFixedRate(() -> iterator.process(ProcessMode.PUMP), 0, 5, TimeUnit.MINUTES);
    }
  }

  @Override
  public void handle(ArtifactStream artifactStream) {
    logger.info("Received the artifact cleanup for ArtifactStream");
    executeInternal(artifactStream);
  }

  private void executeInternal(ArtifactStream artifactStream) {
    String artifactStreamId = artifactStream.getUuid();
    try {
      if (artifactCollectionUtils.skipArtifactStreamIteration(artifactStream, false)) {
        return;
      }

      artifactCleanupServiceAsync.cleanupArtifactsAsync(artifactStream);
    } catch (WingsException exception) {
      logger.warn("Failed to cleanup artifacts for artifact stream. Reason {}", exception.getMessage());
      exception.addContext(Account.class, artifactStream.getAccountId());
      exception.addContext(ArtifactStream.class, artifactStreamId);
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception e) {
      logger.warn("Failed to cleanup artifacts for artifactStream. Reason {}", e.getMessage());
    }
  }
}
