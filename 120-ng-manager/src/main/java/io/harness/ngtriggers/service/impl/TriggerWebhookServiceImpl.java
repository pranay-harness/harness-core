package io.harness.ngtriggers.service.impl;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceRequiredProvider;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent.TriggerWebhookEventsKeys;
import io.harness.ngtriggers.helpers.NGTriggerWebhookExecutionHelper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.ngtriggers.service.TriggerWebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@Slf4j
public class TriggerWebhookServiceImpl
    implements TriggerWebhookService, MongoPersistenceIterator.Handler<TriggerWebhookEvent> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MongoTemplate mongoTemplate;
  @Inject private NGTriggerWebhookExecutionHelper ngTriggerWebhookExecutionHelper;
  @Inject private HarnessMetricRegistry harnessMetricRegistry;
  @Inject private MorphiaPersistenceRequiredProvider<TriggerWebhookEvent> persistenceProvider;
  @Inject private NGTriggerService ngTriggerService;

  @Override
  public void registerIterators() {
    PersistenceIterator<TriggerWebhookEvent> iterator =
        persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
            PersistenceIteratorFactory.PumpExecutorOptions.builder()
                .name("WebhookEventProcessor")
                .poolSize(1)
                .interval(ofSeconds(100))
                .build(),
            TriggerWebhookService.class,
            MongoPersistenceIterator.<TriggerWebhookEvent, SpringFilterExpander>builder()
                .clazz(TriggerWebhookEvent.class)
                .fieldName(TriggerWebhookEventsKeys.nextIteration)
                .targetInterval(ofSeconds(100))
                .acceptableNoAlertDelay(ofSeconds(30))
                .handler(this)
                .filterExpander(
                    query -> query.addCriteria(Criteria.where(TriggerWebhookEventsKeys.attemptCount).lte(2)))
                .schedulingType(REGULAR)
                .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
                .redistribute(true));
  }

  @Override
  public void handle(TriggerWebhookEvent event) {
    boolean success = ngTriggerWebhookExecutionHelper.handleTriggerWebhookEvent(event);
    if (success) {
      ngTriggerService.deleteTriggerWebhookEvent(event);
    } else {
      event.setAttemptCount(event.getAttemptCount() + 1);
      ngTriggerService.updateTriggerWebhookEvent(event);
    }
  }
}
