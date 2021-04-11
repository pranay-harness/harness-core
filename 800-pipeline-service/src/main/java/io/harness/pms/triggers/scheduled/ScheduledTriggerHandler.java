package io.harness.pms.triggers.scheduled;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.IRREGULAR_SKIP_MISSED;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.PlanExecution;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity.NGTriggerEntityKeys;
import io.harness.ngtriggers.beans.entity.TriggerEventHistory;
import io.harness.ngtriggers.beans.source.NGTriggerType;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.pms.contracts.triggers.Type;
import io.harness.pms.triggers.TriggerExecutionHelper;
import io.harness.repositories.spring.TriggerEventHistoryRepository;

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
  @Inject private TriggerExecutionHelper ngTriggerExecutionHelper;
  @Inject private TriggerEventHistoryRepository triggerEventHistoryRepository;
  @Inject private NGTriggerElementMapper ngTriggerElementMapper;

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
            .maximumDelayForCheck(ofSeconds(30))
            .handler(this)
            .filterExpander(query
                -> query.addCriteria(
                    new Criteria()
                        .and(NGTriggerEntityKeys.enabled)
                        .is(true)
                        .and(NGTriggerEntityKeys.deleted)
                        .is(false)
                        .andOperator(Criteria.where(NGTriggerEntityKeys.type).is(NGTriggerType.SCHEDULED))))
            .schedulingType(IRREGULAR_SKIP_MISSED)
            .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
            .redistribute(true));
  }

  @Override
  public void handle(NGTriggerEntity entity) {
    try {
      PlanExecution response = ngTriggerExecutionHelper.resolveRuntimeInputAndSubmitExecutionRequest(
          TriggerDetails.builder()
              .ngTriggerEntity(entity)
              .ngTriggerConfig(ngTriggerElementMapper.toTriggerConfig(entity.getYaml()))
              .build(),
          TriggerPayload.newBuilder().setType(Type.SCHEDULED).build());
      triggerEventHistoryRepository.save(toHistoryRecord(
          entity, "TARGET_EXECUTION_REQUESTED", "Pipeline execution was requested successfully", false));
      log.info("Execution started for cron trigger: " + entity + " with response " + response);
    } catch (Exception e) {
      triggerEventHistoryRepository.save(toHistoryRecord(entity, "EXCEPTION_WHILE_PROCESSING", e.getMessage(), true));
      log.error("Exception while triggering cron. Please check", e);
    }
  }

  private TriggerEventHistory toHistoryRecord(
      NGTriggerEntity entity, String finalStatus, String message, boolean exceptionOccurred) {
    return TriggerEventHistory.builder()
        .accountId(entity.getAccountId())
        .orgIdentifier(entity.getOrgIdentifier())
        .projectIdentifier(entity.getProjectIdentifier())
        .targetIdentifier(entity.getTargetIdentifier())
        .eventCreatedAt(System.currentTimeMillis())
        .finalStatus(finalStatus)
        .message(message)
        .exceptionOccurred(exceptionOccurred)
        .triggerIdentifier(entity.getIdentifier())
        .build();
  }
}
