package io.harness.workers.background.critical.iterator;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.codahale.metrics.InstrumentedExecutorService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.WingsException;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIterator.ProcessMode;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.logging.AutoLogContext;
import io.harness.logging.ExceptionLogger;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.Permit;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamKeys;
import software.wings.delegatetasks.buildsource.ArtifactStreamLogContext;
import software.wings.service.impl.PermitServiceImpl;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.ArtifactCollectionService;
import software.wings.service.intfc.PermitService;

import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class ArtifactCollectionHandler implements Handler<ArtifactStream> {
  public static final String GROUP = "ARTIFACT_STREAM_CRON_GROUP";

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private PermitService permitService;
  @Inject private HarnessMetricRegistry harnessMetricRegistry;
  @Inject @Named("AsyncArtifactCollectionService") private ArtifactCollectionService artifactCollectionServiceAsync;
  @Inject private ArtifactCollectionUtils artifactCollectionUtils;

  public void registerIterators(ScheduledThreadPoolExecutor artifactCollectionExecutor) {
    InstrumentedExecutorService instrumentedExecutorService = new InstrumentedExecutorService(
        artifactCollectionExecutor, harnessMetricRegistry.getThreadPoolMetricRegistry(), "Iterator-ArtifactCollection");
    PersistenceIterator iterator = persistenceIteratorFactory.createIterator(ArtifactCollectionHandler.class,
        MongoPersistenceIterator.<ArtifactStream>builder()
            .mode(ProcessMode.PUMP)
            .clazz(ArtifactStream.class)
            .fieldName(ArtifactStreamKeys.nextIteration)
            .targetInterval(ofMinutes(1))
            .acceptableNoAlertDelay(ofSeconds(30))
            .executorService(instrumentedExecutorService)
            .semaphore(new Semaphore(25))
            .handler(this)
            .schedulingType(REGULAR)
            .redistribute(true));

    if (iterator != null) {
      artifactCollectionExecutor.scheduleAtFixedRate(() -> iterator.process(), 0, 10, TimeUnit.SECONDS);
    }
  }

  @Override
  public void handle(ArtifactStream artifactStream) {
    try (AutoLogContext ignore2 = new ArtifactStreamLogContext(
             artifactStream.getUuid(), artifactStream.getArtifactStreamType(), OVERRIDE_ERROR)) {
      logger.info("Received the artifact collection for ArtifactStream");
    }
    executeInternal(artifactStream);
  }

  private void executeInternal(ArtifactStream artifactStream) {
    String artifactStreamId = artifactStream.getUuid();
    try {
      if (artifactCollectionUtils.skipArtifactStreamIteration(artifactStream, true)) {
        return;
      }

      int leaseDuration = (int) (TimeUnit.MINUTES.toMillis(2)
          * PermitServiceImpl.getBackoffMultiplier(artifactStream.getFailedCronAttempts()));
      String permitId = permitService.acquirePermit(Permit.builder()
                                                        .appId(artifactStream.fetchAppId())
                                                        .group(GROUP)
                                                        .key(artifactStreamId)
                                                        .expireAt(new Date(System.currentTimeMillis() + leaseDuration))
                                                        .leaseDuration(leaseDuration)
                                                        .build());
      if (isNotEmpty(permitId)) {
        logger.info("Permit [{}] acquired for artifactStream [failedCount: {}] for [{}] minutes", permitId,
            artifactStream.getFailedCronAttempts(), TimeUnit.MILLISECONDS.toMinutes(leaseDuration));
        artifactCollectionServiceAsync.collectNewArtifactsAsync(artifactStream, permitId);
      } else {
        logger.info("Permit already exists for artifactStream");
      }
    } catch (WingsException exception) {
      logger.warn("Failed to collect artifacts for artifact stream. Reason {}", exception.getMessage());
      if (artifactStream.getAccountId() != null) {
        exception.addContext(Account.class, artifactStream.getAccountId());
      }
      exception.addContext(ArtifactStream.class, artifactStreamId);
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception e) {
      logger.warn("Failed to collect artifacts for artifactStream. Reason {}", e.getMessage());
    }
  }
}
