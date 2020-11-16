package io.harness.ng.ngtriggers.service;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.codahale.metrics.InstrumentedExecutorService;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceRequiredProvider;
import io.harness.ng.ngtriggers.helpers.NGTriggerWebhookExecutionHelper;
import io.harness.ng.ngtriggers.intfc.TriggerWebhookService;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent.TriggerWebhookEventsKeys;
import io.harness.workers.background.critical.iterator.ArtifactCollectionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class TriggerWebhookServiceImpl
    implements TriggerWebhookService, MongoPersistenceIterator.Handler<TriggerWebhookEvent> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject @Named("orchestrationMongoTemplate") private MongoTemplate mongoTemplate;
  @Inject private NGTriggerWebhookExecutionHelper ngTriggerWebhookExecutionHelper;
  @Inject private HarnessMetricRegistry harnessMetricRegistry;
  @Inject private MorphiaPersistenceRequiredProvider<TriggerWebhookEvent> persistenceProvider;

  @Override
  public void registerIterators(ScheduledThreadPoolExecutor webhookEventExecutor) {
    //    PersistenceIterator<TriggerWebhookEvent> iterator =
    //    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
    //            PersistenceIteratorFactory.PumpExecutorOptions.builder()
    //                    .name("WebhookEventProcessor")
    //                    .poolSize(1)
    //                    .interval(ofSeconds(100))
    //                    .build(),
    //            TriggerWebhookService.class,
    //            MongoPersistenceIterator.<TriggerWebhookEvent, SpringFilterExpander>builder()
    //                    .clazz(TriggerWebhookEvent.class)
    //                    .fieldName(TriggerWebhookEventsKeys.nextIteration)
    //                    .targetInterval(ofMinutes(1))
    //                    .acceptableNoAlertDelay(ofSeconds(30))
    //                    .handler(this::handle)
    //                    .schedulingType(REGULAR)
    //                    .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
    //                    .redistribute(true));

    InstrumentedExecutorService instrumentedExecutorService = new InstrumentedExecutorService(
        webhookEventExecutor, harnessMetricRegistry.getThreadPoolMetricRegistry(), "Iterator-webhookEvent");
    PersistenceIterator iterator = persistenceIteratorFactory.createIterator(ArtifactCollectionHandler.class,
        MongoPersistenceIterator.<TriggerWebhookEvent, MorphiaFilterExpander<TriggerWebhookEvent>>builder()
            .mode(PersistenceIterator.ProcessMode.PUMP)
            .clazz(TriggerWebhookEvent.class)
            .fieldName(TriggerWebhookEventsKeys.nextIteration)
            .targetInterval(ofMinutes(1))
            .acceptableNoAlertDelay(ofSeconds(30))
            .executorService(instrumentedExecutorService)
            .semaphore(new Semaphore(25))
            .handler(this)
            //.entityProcessController(new AccountStatusBasedEntityProcessController<>(accountService))
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));

    if (iterator != null) {
      webhookEventExecutor.scheduleAtFixedRate(() -> iterator.process(), 0, 10, TimeUnit.SECONDS);
    }
  }

  @Override
  public void handle(TriggerWebhookEvent event) {
    // handle event
    ngTriggerWebhookExecutionHelper.handleTriggerWebhookEvent(event);
  }
}
