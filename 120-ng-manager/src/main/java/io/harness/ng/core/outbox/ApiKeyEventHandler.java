/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CREATE_ACTION;
import static io.harness.ng.core.events.ApiKeyCreateEvent.API_KEY_CREATED;
import static io.harness.ng.core.events.ApiKeyDeleteEvent.API_KEY_DELETED;
import static io.harness.ng.core.events.ApiKeyUpdateEvent.API_KEY_UPDATED;
import static io.harness.ng.core.utils.NGYamlUtils.getYamlString;
import static io.harness.remote.NGObjectMapperHelper.NG_DEFAULT_OBJECT_MAPPER;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.beans.ResourceDTO;
import io.harness.audit.beans.ResourceScopeDTO;
import io.harness.audit.client.api.AuditClientService;
import io.harness.context.GlobalContext;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.entity_crud.apikey.ApiKeyEntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.dto.ApiKeyDTO;
import io.harness.ng.core.dto.ApiKeyRequest;
import io.harness.ng.core.events.ApiKeyCreateEvent;
import io.harness.ng.core.events.ApiKeyDeleteEvent;
import io.harness.ng.core.events.ApiKeyUpdateEvent;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PL)
public class ApiKeyEventHandler implements OutboxEventHandler {
  private final ObjectMapper objectMapper;
  private final Producer eventProducer;
  private final AuditClientService auditClientService;

  @Inject
  public ApiKeyEventHandler(
      @Named(EventsFrameworkConstants.ENTITY_CRUD) Producer eventProducer, AuditClientService auditClientService) {
    this.objectMapper = NG_DEFAULT_OBJECT_MAPPER;
    this.eventProducer = eventProducer;
    this.auditClientService = auditClientService;
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    try {
      switch (outboxEvent.getEventType()) {
        case API_KEY_CREATED:
          return handleApiKeyCreateEvent(outboxEvent);
        case API_KEY_UPDATED:
          return handleApiKeyUpdateEvent(outboxEvent);
        case API_KEY_DELETED:
          return handleApiKeyDeleteEvent(outboxEvent);
        default:
          throw new InvalidArgumentsException(String.format("Not supported event type %s", outboxEvent.getEventType()));
      }
    } catch (IOException exception) {
      log.error("Failed to handle " + outboxEvent.getEventType() + " event", exception);
      return false;
    }
  }

  private boolean handleApiKeyCreateEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    ApiKeyCreateEvent apiKeyCreateEvent = objectMapper.readValue(outboxEvent.getEventData(), ApiKeyCreateEvent.class);
    boolean publishedToRedis = publishEvent(apiKeyCreateEvent.getApiKey(), CREATE_ACTION);
    AuditEntry auditEntry =
        AuditEntry.builder()
            .action(Action.CREATE)
            .module(ModuleType.CORE)
            .newYaml(getYamlString(ApiKeyRequest.builder().apiKey(apiKeyCreateEvent.getApiKey()).build()))
            .timestamp(outboxEvent.getCreatedAt())
            .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
            .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
            .insertId(outboxEvent.getId())
            .build();
    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleApiKeyUpdateEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();

    ApiKeyUpdateEvent apiKeyUpdateEvent = objectMapper.readValue(outboxEvent.getEventData(), ApiKeyUpdateEvent.class);
    boolean publishedToRedis =
        publishEvent(apiKeyUpdateEvent.getNewApiKey(), EventsFrameworkMetadataConstants.UPDATE_ACTION);
    AuditEntry auditEntry =
        AuditEntry.builder()
            .action(Action.UPDATE)
            .module(ModuleType.CORE)
            .newYaml(getYamlString(ApiKeyRequest.builder().apiKey(apiKeyUpdateEvent.getNewApiKey()).build()))
            .oldYaml(getYamlString(ApiKeyRequest.builder().apiKey(apiKeyUpdateEvent.getOldApiKey()).build()))
            .timestamp(outboxEvent.getCreatedAt())
            .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
            .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
            .insertId(outboxEvent.getId())
            .build();
    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleApiKeyDeleteEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();

    ApiKeyDeleteEvent apiKeyDeleteEvent = objectMapper.readValue(outboxEvent.getEventData(), ApiKeyDeleteEvent.class);
    boolean publishedToRedis =
        publishEvent(apiKeyDeleteEvent.getApiKey(), EventsFrameworkMetadataConstants.DELETE_ACTION);
    AuditEntry auditEntry =
        AuditEntry.builder()
            .action(Action.DELETE)
            .module(ModuleType.CORE)
            .oldYaml(getYamlString(ApiKeyRequest.builder().apiKey(apiKeyDeleteEvent.getApiKey()).build()))
            .timestamp(outboxEvent.getCreatedAt())
            .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
            .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
            .insertId(outboxEvent.getId())
            .build();
    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean publishEvent(ApiKeyDTO apiKeyDTO, String action) {
    try {
      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", apiKeyDTO.getAccountIdentifier(),
                  EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkMetadataConstants.API_KEY_ENTITY,
                  EventsFrameworkMetadataConstants.ACTION, action))
              .setData(ApiKeyEntityChangeDTO.newBuilder()
                           .setIdentifier(apiKeyDTO.getIdentifier())
                           .setOrgIdentifier(apiKeyDTO.getOrgIdentifier() != null ? apiKeyDTO.getOrgIdentifier() : "")
                           .setProjectIdentifier(
                               apiKeyDTO.getProjectIdentifier() != null ? apiKeyDTO.getProjectIdentifier() : "")
                           .setApiKeyType(apiKeyDTO.getApiKeyType().name())
                           .setParentIdentifier(apiKeyDTO.getParentIdentifier())
                           .setIdentifier(apiKeyDTO.getIdentifier())
                           .build()
                           .toByteString())
              .build());
      return true;
    } catch (EventsFrameworkDownException e) {
      log.error(
          "Failed to send " + action + " event to events framework api key identifier: " + apiKeyDTO.getIdentifier(),
          e);
      return false;
    }
  }
}
