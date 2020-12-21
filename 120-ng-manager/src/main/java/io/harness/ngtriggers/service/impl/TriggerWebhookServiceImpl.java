package io.harness.ngtriggers.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.SCM_SERVICE_CONNECTION_FAILED;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceRequiredProvider;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventProcessingResult;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent.TriggerWebhookEventsKeys;
import io.harness.ngtriggers.beans.response.WebhookEventResponse;
import io.harness.ngtriggers.helper.NGTriggerWebhookExecutionHelper;
import io.harness.ngtriggers.helpers.WebhookEventResponseHelper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.ngtriggers.service.TriggerWebhookService;
import io.harness.repositories.ng.core.spring.TriggerEventHistoryRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;
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
  @Inject private TriggerEventHistoryRepository triggerEventHistoryRepository;

  @Override
  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("WebhookEventProcessor")
            .poolSize(2)
            .interval(ofSeconds(10))
            .build(),
        TriggerWebhookService.class,
        MongoPersistenceIterator.<TriggerWebhookEvent, SpringFilterExpander>builder()
            .clazz(TriggerWebhookEvent.class)
            .fieldName(TriggerWebhookEventsKeys.nextIteration)
            .targetInterval(ofMinutes(5))
            .acceptableExecutionTime(ofMinutes(1))
            .acceptableNoAlertDelay(ofSeconds(30))
            .handler(this)
            .filterExpander(query
                -> query.addCriteria(new Criteria()
                                         .and(TriggerWebhookEventsKeys.attemptCount)
                                         .lte(2)
                                         .andOperator(Criteria.where(TriggerWebhookEventsKeys.processing).is(false))))
            .schedulingType(REGULAR)
            .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
            .redistribute(true));
  }

  @Override
  public void handle(TriggerWebhookEvent event) {
    try {
      updateTriggerEventProcessingStatus(event, true); // start processing
      WebhookEventProcessingResult result = ngTriggerWebhookExecutionHelper.handleTriggerWebhookEvent(event);

      List<WebhookEventResponse> responseList = result.getResponses();

      // Remove any null values if present in list
      if (isNotEmpty(responseList)) {
        responseList.stream().filter(response -> response != null).collect(Collectors.toList());
      }

      if (isEmpty(responseList)) {
        ngTriggerService.deleteTriggerWebhookEvent(event);
      } else if (!result.isMappedToTriggers()) {
        handleTriggerNotFoundCase(event, result);
      } else {
        responseList.forEach(
            response -> triggerEventHistoryRepository.save(WebhookEventResponseHelper.toEntity(response)));
        ngTriggerService.deleteTriggerWebhookEvent(event);
      }
    } catch (Exception e) {
      event.setAttemptCount(event.getAttemptCount() + 1);
      ngTriggerService.updateTriggerWebhookEvent(event);
      log.error("Exception while handling webhook event. Please check", e);
    }
  }

  private void handleTriggerNotFoundCase(TriggerWebhookEvent event, WebhookEventProcessingResult result) {
    if (isScmConnectivityFailed(result)) {
      event.setAttemptCount(event.getAttemptCount() + 1);
      updateTriggerEventProcessingStatus(event, false);
    } else {
      triggerEventHistoryRepository.save(WebhookEventResponseHelper.toEntity(result.getResponses().get(0)));
      ngTriggerService.deleteTriggerWebhookEvent(event);
    }
  }

  private boolean isScmConnectivityFailed(WebhookEventProcessingResult result) {
    return isNotEmpty(result.getResponses())
        || result.getResponses().get(0).getFinalStatus() == SCM_SERVICE_CONNECTION_FAILED;
  }

  protected void updateTriggerEventProcessingStatus(TriggerWebhookEvent event, boolean status) {
    event.setProcessing(status);
    ngTriggerService.updateTriggerWebhookEvent(event);
  }
}
