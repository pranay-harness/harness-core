package io.harness.ng.outbox;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ORGANIZATION_ENTITY;

import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.eventsframework.entity_crud.organization.OrganizationEntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.ng.core.entities.Organization;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NextGenOutboxEventHandler implements OutboxEventHandler {
  private final Producer eventProducer;

  @Inject
  public NextGenOutboxEventHandler(@Named(EventsFrameworkConstants.ENTITY_CRUD) Producer eventProducer) {
    this.eventProducer = eventProducer;
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    if (ORGANIZATION_ENTITY.equals(outboxEvent.getType())) {
      Organization organization = (Organization) outboxEvent.getObject();
      try {
        eventProducer.send(
            Message.newBuilder()
                .putAllMetadata(ImmutableMap.of("accountId", organization.getAccountIdentifier(), ENTITY_TYPE,
                    ORGANIZATION_ENTITY, ACTION, outboxEvent.getAdditionalData().get(ACTION)))
                .setData(getOrganizationPayload(organization))
                .build());
      } catch (ProducerShutdownException e) {
        log.error("Failed to send event to events framework orgIdentifier: " + organization.getIdentifier(), e);
        return false;
      }
    }
    return true;
  }

  private ByteString getOrganizationPayload(Organization organization) {
    return OrganizationEntityChangeDTO.newBuilder()
        .setIdentifier(organization.getIdentifier())
        .setAccountIdentifier(organization.getAccountIdentifier())
        .build()
        .toByteString();
  }
}
