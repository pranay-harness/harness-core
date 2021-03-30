package io.harness.pms.triggers.scheduled;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.IRREGULAR_SKIP_MISSED;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import io.harness.annotations.dev.OwnedBy;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity.NGTriggerEntityKeys;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
@Singleton
@OwnedBy(PIPELINE)
public class ScheduledTriggerHandler implements Handler<NGTriggerEntity> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MongoTemplate mongoTemplate;

  public void registerIterators() {
    persistenceIteratorFactory.createLoopIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("ScheduledTriggerProcessor")
            .poolSize(2)
            .interval(ofSeconds(15))
            .build(),
        ScheduledTriggerHandler.class,
        MongoPersistenceIterator.<NGTriggerEntity, SpringFilterExpander>builder()
            .clazz(NGTriggerEntity.class)
            .fieldName(NGTriggerEntityKeys.nextIterations)
            .targetInterval(ofMinutes(5))
            .acceptableExecutionTime(ofMinutes(1))
            .acceptableNoAlertDelay(ofSeconds(30))
            .handler(this)
            .filterExpander(query
                -> query.addCriteria(new Criteria()
                                         .and(NGTriggerEntityKeys.enabled)
                                         .is(true)
                                         .andOperator(Criteria.where("metadata.cron").exists(true))))
            .schedulingType(IRREGULAR_SKIP_MISSED)
            .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
            .redistribute(true));
  }

  @Override
  public void handle(NGTriggerEntity entity) {
    try {
      log.info("CDNG-4840 Handling entity: " + entity);
      // PlanExecution response = ngTriggerWebhookExecutionHelper.handleTriggerEntity(entity);
      // TargetExecutionSummary targetExecutionSummary =
      //     WebhookEventResponseHelper.prepareTargetExecutionSummary(response, triggerDetails, runtimeInputYaml);
    } catch (Exception e) {
      log.error("Exception while triggering cron. Please check", e);
    }
  }
}
