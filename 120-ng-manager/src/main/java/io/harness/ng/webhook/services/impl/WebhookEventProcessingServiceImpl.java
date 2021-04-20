package io.harness.ng.webhook.services.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.eventsframework.EventsFrameworkConstants.WEBHOOK_TRIGGER_EVENT_DATA;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.webhookpayloads.webhookdata.SourceRepoType;
import io.harness.eventsframework.webhookpayloads.webhookdata.WebhookDTO;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;
import io.harness.ng.webhook.WebhookHelper;
import io.harness.ng.webhook.entities.WebhookEvent;
import io.harness.ng.webhook.entities.WebhookEvent.WebhookEventsKeys;
import io.harness.ng.webhook.services.api.WebhookEventProcessingService;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;
import io.harness.repositories.ng.webhook.spring.WebhookEventRepository;
import io.harness.service.WebhookParserSCMService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

@Singleton
@Slf4j
@OwnedBy(PIPELINE)
public class WebhookEventProcessingServiceImpl
    implements WebhookEventProcessingService, MongoPersistenceIterator.Handler<WebhookEvent> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MongoTemplate mongoTemplate;
  @Inject private WebhookParserSCMService webhookParserSCMService;
  @Inject @Named(WEBHOOK_TRIGGER_EVENT_DATA) private Producer eventProducer;
  @Inject WebhookEventRepository webhookEventRepository;

  @Override
  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("WebhookEventProcessor")
            .poolSize(5)
            .interval(ofSeconds(5))
            .build(),
        WebhookEventProcessingService.class,
        MongoPersistenceIterator.<WebhookEvent, SpringFilterExpander>builder()
            .clazz(WebhookEvent.class)
            .fieldName(WebhookEventsKeys.nextIteration)
            .targetInterval(ofMinutes(5))
            .acceptableExecutionTime(ofMinutes(2))
            .acceptableNoAlertDelay(ofSeconds(30))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
            .redistribute(true));
  }

  @Override
  public void handle(WebhookEvent event) {
    ParseWebhookResponse parseWebhookResponse = null;
    SourceRepoType sourceRepoType = WebhookHelper.getSourceRepoType(event);
    if (sourceRepoType != SourceRepoType.UNRECOGNIZED) {
      parseWebhookResponse = invokeScmService(event);
    }

    try {
      publishWebhookEvent(event, parseWebhookResponse, sourceRepoType);
    } catch (Exception e) {
      log.error(new StringBuilder(128)
                    .append("Error while publishing Webhook Event: ")
                    .append(event.getUuid())
                    .append(", Exception: ")
                    .append(e)
                    .toString());
    }
  }

  public ParseWebhookResponse invokeScmService(WebhookEvent event) {
    try {
      return webhookParserSCMService.parseWebhookUsingSCMAPI(event.getHeaders(), event.getPayload());
    } catch (Exception e) {
      // this means, SCM could not parse payload. This seems like some event SCM does not yet support.
      // We still need to continue, as someone might have configured Custom trigger on this.
      return null;
    }
  }

  public void publishWebhookEvent(WebhookEvent event, ParseWebhookResponse parseWebhookResponse,
      SourceRepoType sourceRepoType) throws ProducerShutdownException {
    WebhookDTO webhookDTO = WebhookHelper.generateWebhookDTO(event, parseWebhookResponse, sourceRepoType);
    eventProducer.send(Message.newBuilder().setData(webhookDTO.toByteString()).build());
    webhookEventRepository.delete(event);
  }
}
