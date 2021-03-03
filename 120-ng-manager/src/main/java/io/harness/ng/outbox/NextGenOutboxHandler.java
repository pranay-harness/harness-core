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
import io.harness.outbox.Outbox;
import io.harness.outbox.api.OutboxHandler;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NextGenOutboxHandler implements OutboxHandler {
  private final Producer eventProducer;

  @Inject
  public NextGenOutboxHandler(@Named(EventsFrameworkConstants.ENTITY_CRUD) Producer eventProducer) {
    this.eventProducer = eventProducer;
  }

  @Override
  public boolean handle(Outbox outbox) {
    Organization organization = (Organization) outbox.getObject();
    try {
      eventProducer.send(Message.newBuilder()
                             .putAllMetadata(ImmutableMap.of("accountId", organization.getAccountIdentifier(),
                                 ENTITY_TYPE, ORGANIZATION_ENTITY, ACTION, outbox.getAdditionalData().get(ACTION)))
                             .setData(getOrganizationPayload(organization))
                             .build());
    } catch (ProducerShutdownException e) {
      log.error("Failed to send event to events framework orgIdentifier: " + organization.getIdentifier(), e);
      return false;
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
