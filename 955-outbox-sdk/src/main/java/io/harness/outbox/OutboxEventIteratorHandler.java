package io.harness.outbox;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static io.harness.outbox.OutboxSDKConstants.DEFAULT_OUTBOX_ITERATOR_CONFIGURATION;

import static java.time.Duration.ofSeconds;

import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;
import io.harness.outbox.OutboxEvent.OutboxEventKeys;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.outbox.api.OutboxService;

import com.google.inject.Inject;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
public class OutboxEventIteratorHandler implements MongoPersistenceIterator.Handler<OutboxEvent> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MongoTemplate mongoTemplate;
  @Inject private OutboxEventHandler outboxEventHandler;
  @Inject private OutboxService outboxService;
  @Inject(optional = true) @Nullable private OutboxEventIteratorConfiguration config;

  @Override
  public void handle(OutboxEvent outbox) {
    assert config != null;
    boolean success = false;
    try {
      success = outboxEventHandler.handle(outbox);
    } catch (Exception exception) {
      log.error("Error occurred while handling outbox event with id {} with response [{}]", outbox.getId(),
          exception.getStackTrace());
    }
    long maximumAttempts = config.getMaximumOutboxEventHandlingAttempts();
    if (success) {
      outboxService.delete(outbox.getId());
    } else {
      long attempts = outbox.getAttempts() == null ? 0 : outbox.getAttempts();
      if (attempts + 1 >= maximumAttempts) {
        outbox.setBlocked(true);
      }
      outbox.setAttempts(attempts + 1);
      outboxService.update(outbox);
    }
  }

  public void registerIterators() {
    if (config == null) {
      config = DEFAULT_OUTBOX_ITERATOR_CONFIGURATION;
    }
    assert config != null;
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("OutboxEventIteratorTask")
            .poolSize(config.getThreadPoolSize())
            .interval(ofSeconds(config.getIntervalInSeconds()))
            .build(),
        OutboxEvent.class,
        MongoPersistenceIterator.<OutboxEvent, SpringFilterExpander>builder()
            .clazz(OutboxEvent.class)
            .fieldName(OutboxEventKeys.nextIteration)
            .targetInterval(ofSeconds(config.getTargetIntervalInSeconds()))
            .acceptableNoAlertDelay(ofSeconds(config.getAcceptableNoAlertDelayInSeconds()))
            .handler(this)
            .filterExpander(query -> query.addCriteria(Criteria.where(OutboxEventKeys.blocked).ne(true)))
            .schedulingType(REGULAR)
            .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
            .redistribute(true));
  }
}
