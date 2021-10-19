package io.harness.timeout;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.IteratorConfig;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceRequiredProvider;
import io.harness.registries.timeout.TimeoutRegistry;
import io.harness.repositories.TimeoutInstanceRepository;
import io.harness.timeout.TimeoutInstance.TimeoutInstanceKeys;
import io.harness.timeout.contracts.Dimension;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutParameters;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutTrackerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class TimeoutEngine implements Handler<TimeoutInstance> {
  private static final Duration MAX_CALLBACK_PROCESSING_TIME = Duration.ofMinutes(1);

  @Inject private TimeoutInstanceRepository timeoutInstanceRepository;
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MongoTemplate mongoTemplate;
  @Inject private Injector injector;
  @Inject private TimeoutRegistry timeoutRegistry;

  private PersistenceIterator<TimeoutInstance> iterator;

  public TimeoutInstance registerTimeout(@NotNull Dimension dimension, @NotNull TimeoutParameters timeoutParameters,
      @NotNull TimeoutCallback timeoutCallback) {
    TimeoutTrackerFactory timeoutTrackerFactory = timeoutRegistry.obtain(dimension);
    TimeoutTracker timeoutTracker = timeoutTrackerFactory.create(timeoutParameters);
    return registerTimeout(timeoutTracker, timeoutCallback);
  }

  public TimeoutInstance registerAbsoluteTimeout(Duration timeoutDuration, TimeoutCallback timeoutCallback) {
    return registerTimeout(AbsoluteTimeoutTrackerFactory.DIMENSION,
        AbsoluteTimeoutParameters.builder().timeoutMillis(timeoutDuration.toMillis()).build(), timeoutCallback);
  }

  @VisibleForTesting
  TimeoutInstance registerTimeout(@NotNull TimeoutTracker timeoutTracker, @NotNull TimeoutCallback timeoutCallback) {
    TimeoutInstance timeoutInstance =
        TimeoutInstance.builder().uuid(generateUuid()).tracker(timeoutTracker).callback(timeoutCallback).build();
    timeoutInstance.resetNextIteration();
    TimeoutInstance savedTimeoutInstance = timeoutInstanceRepository.save(timeoutInstance);
    log.info(format("Registered timeout with uuid: %s, currentTime: %d, expiryTime: %d, diff: %d",
        timeoutInstance.getUuid(), System.currentTimeMillis(), timeoutInstance.getNextIteration(),
        timeoutInstance.getNextIteration() - System.currentTimeMillis()));
    if (iterator != null) {
      iterator.wakeup();
    }
    return savedTimeoutInstance;
  }

  public void deleteTimeouts(List<String> timeoutInstanceIds) {
    if (EmptyPredicate.isNotEmpty(timeoutInstanceIds)) {
      timeoutInstanceRepository.deleteByUuidIn(timeoutInstanceIds);
    }
  }

  public void deleteTimeout(@NonNull String timeoutInstanceId) {
    deleteTimeouts(Collections.singletonList(timeoutInstanceId));
  }

  public void onEvent(List<String> timeoutInstanceIds, TimeoutEvent event) {
    if (EmptyPredicate.isEmpty(timeoutInstanceIds)) {
      return;
    }

    for (TimeoutInstance timeoutInstance : timeoutInstanceRepository.findAllById(timeoutInstanceIds)) {
      if (timeoutInstance.tracker.onEvent(event)) {
        timeoutInstance.resetNextIteration();
        timeoutInstanceRepository.save(timeoutInstance);
        if (iterator != null) {
          iterator.wakeup();
        }
      }
    }
  }

  public void registerIterators() {
    IteratorConfig iteratorConfig =
        IteratorConfig.builder().enabled(true).targetIntervalInSeconds(10).threadPoolCount(5).build();
    registerIterators(iteratorConfig);
  }

  public void registerIterators(IteratorConfig iteratorConfig) {
    PersistenceIteratorFactory.PumpExecutorOptions options =
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .interval(Duration.ofSeconds(iteratorConfig.getTargetIntervalInSeconds()))
            .poolSize(iteratorConfig.getThreadPoolCount())
            .name("TimeoutEngineHandler-%d")
            .build();
    iterator = persistenceIteratorFactory.createLoopIteratorWithDedicatedThreadPool(options, TimeoutInstance.class,
        MongoPersistenceIterator.<TimeoutInstance, SpringFilterExpander>builder()
            .clazz(TimeoutInstance.class)
            .fieldName(TimeoutInstanceKeys.nextIteration)
            // targetInterval is just to add retry mechanism. The document should ideally be deleted after callback is
            // executed.
            .targetInterval(Duration.ofMinutes(2))
            .acceptableNoAlertDelay(Duration.ofSeconds(45))
            .acceptableExecutionTime(Duration.ofSeconds(30))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(new SpringPersistenceRequiredProvider<>(mongoTemplate))
            .redistribute(true));
  }

  @Override
  public void handle(TimeoutInstance timeoutInstance) {
    try (TimeoutInstanceLogContext ignore0 = new TimeoutInstanceLogContext(timeoutInstance.getUuid(), OVERRIDE_ERROR)) {
      final long now = System.currentTimeMillis();
      log.info("TimeoutInstance handle started");

      TimeoutCallback callback = timeoutInstance.getCallback();
      injector.injectMembers(callback);
      try {
        callback.onTimeout(timeoutInstance);
        log.info("TimeoutInstance callback finished");
      } catch (Exception ex) {
        // TODO(gpahal): What to do in case callback throws an exception. Should we retry?
        log.error("TimeoutInstance callback failed", ex);
      }

      try {
        timeoutInstanceRepository.deleteById(timeoutInstance.getUuid());
      } catch (Exception ex) {
        log.error("TimeoutInstance delete failed", ex);
      }

      final long passed = System.currentTimeMillis() - now;
      if (passed > MAX_CALLBACK_PROCESSING_TIME.toMillis()) {
        log.error(
            "TimeoutInstanceHandler: It took more than {} ms before we processed the callback. THIS IS VERY BAD!!!",
            MAX_CALLBACK_PROCESSING_TIME.toMillis());
      }
    }
  }
}
