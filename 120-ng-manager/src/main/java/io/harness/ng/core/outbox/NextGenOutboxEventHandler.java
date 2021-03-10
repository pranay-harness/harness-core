package io.harness.ng.core.outbox;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ORGANIZATION_ENTITY;

import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.eventsframework.entity_crud.organization.OrganizationEntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.ng.core.AccountScope;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NextGenOutboxEventHandler implements OutboxEventHandler {
  private final ObjectMapper objectMapper;
  private final Producer eventProducer;

  @Inject
  public NextGenOutboxEventHandler(
      ObjectMapper objectMapper, @Named(EventsFrameworkConstants.ENTITY_CRUD) Producer eventProducer) {
    this.objectMapper = objectMapper;
    this.eventProducer = eventProducer;
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    switch (outboxEvent.getEventType()) {
      case "EntityChange":
        return handleEntityChangeEvent(outboxEvent);
      default:
        return true;
    }
  }

  private boolean handleEntityChangeEvent(OutboxEvent outboxEvent) {
    String action;
    try {
      action = objectMapper.readValue(outboxEvent.getEventData(), String.class);
    } catch (IOException exception) {
      log.error("IOException occurred while parsing EntityChange event");
      return false;
    }
    switch (outboxEvent.getResource().getType()) {
      case ORGANIZATION_ENTITY:
        return publishOrganizationChangeEventToRedis(
            ((AccountScope) outboxEvent.getResourceScope()).getAccountIdentifier(),
            outboxEvent.getResource().getIdentifier(), action);
      default:
        return true;
    }
  }

  private boolean publishOrganizationChangeEventToRedis(String accountIdentifier, String identifier, String action) {
    try {
      eventProducer.send(Message.newBuilder()
                             .putAllMetadata(ImmutableMap.of("accountId", accountIdentifier,
                                 EventsFrameworkMetadataConstants.ENTITY_TYPE, ORGANIZATION_ENTITY,
                                 EventsFrameworkMetadataConstants.ACTION, action))
                             .setData(getOrganizationPayload(accountIdentifier, identifier))
                             .build());
    } catch (ProducerShutdownException e) {
      log.error("Failed to send event to events framework orgIdentifier: " + identifier, e);
      return false;
    }
    return true;
  }

  private ByteString getOrganizationPayload(String accountIdentifier, String identifier) {
    return OrganizationEntityChangeDTO.newBuilder()
        .setIdentifier(identifier)
        .setAccountIdentifier(accountIdentifier)
        .build()
        .toByteString();
  }
}
