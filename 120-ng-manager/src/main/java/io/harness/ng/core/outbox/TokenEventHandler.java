/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CREATE_ACTION;
import static io.harness.ng.core.events.TokenCreateEvent.TOKEN_CREATED;
import static io.harness.ng.core.events.TokenDeleteEvent.TOKEN_DELETED;
import static io.harness.ng.core.events.TokenUpdateEvent.TOKEN_UPDATED;
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
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.dto.TokenDTO;
import io.harness.ng.core.dto.TokenRequest;
import io.harness.ng.core.events.TokenCreateEvent;
import io.harness.ng.core.events.TokenDeleteEvent;
import io.harness.ng.core.events.TokenUpdateEvent;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.StringValue;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PL)
public class TokenEventHandler implements OutboxEventHandler {
  private final ObjectMapper objectMapper;
  private final Producer eventProducer;
  private final AuditClientService auditClientService;

  @Inject
  public TokenEventHandler(
      @Named(EventsFrameworkConstants.ENTITY_CRUD) Producer eventProducer, AuditClientService auditClientService) {
    this.objectMapper = NG_DEFAULT_OBJECT_MAPPER;
    this.eventProducer = eventProducer;
    this.auditClientService = auditClientService;
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    try {
      switch (outboxEvent.getEventType()) {
        case TOKEN_CREATED:
          return handleTokenCreateEvent(outboxEvent);
        case TOKEN_UPDATED:
          return handleTokenUpdateEvent(outboxEvent);
        case TOKEN_DELETED:
          return handleTokenDeleteEvent(outboxEvent);
        default:
          throw new InvalidArgumentsException(String.format("Not supported event type %s", outboxEvent.getEventType()));
      }
    } catch (IOException exception) {
      log.error("Failed to handle " + outboxEvent.getEventType() + " event", exception);
      return false;
    }
  }

  private boolean handleTokenCreateEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    TokenCreateEvent TokenCreateEvent = objectMapper.readValue(outboxEvent.getEventData(), TokenCreateEvent.class);
    boolean publishedToRedis = publishEvent(TokenCreateEvent.getToken(), CREATE_ACTION);
    AuditEntry auditEntry =
        AuditEntry.builder()
            .action(Action.CREATE)
            .module(ModuleType.CORE)
            .newYaml(getYamlString(TokenRequest.builder().token(TokenCreateEvent.getToken()).build()))
            .timestamp(outboxEvent.getCreatedAt())
            .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
            .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
            .insertId(outboxEvent.getId())
            .build();
    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleTokenUpdateEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();

    TokenUpdateEvent TokenUpdateEvent = objectMapper.readValue(outboxEvent.getEventData(), TokenUpdateEvent.class);
    boolean publishedToRedis =
        publishEvent(TokenUpdateEvent.getNewToken(), EventsFrameworkMetadataConstants.UPDATE_ACTION);
    AuditEntry auditEntry =
        AuditEntry.builder()
            .action(Action.UPDATE)
            .module(ModuleType.CORE)
            .newYaml(getYamlString(TokenRequest.builder().token(TokenUpdateEvent.getNewToken()).build()))
            .oldYaml(getYamlString(TokenRequest.builder().token(TokenUpdateEvent.getOldToken()).build()))
            .timestamp(outboxEvent.getCreatedAt())
            .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
            .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
            .insertId(outboxEvent.getId())
            .build();
    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleTokenDeleteEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();

    TokenDeleteEvent TokenDeleteEvent = objectMapper.readValue(outboxEvent.getEventData(), TokenDeleteEvent.class);
    boolean publishedToRedis =
        publishEvent(TokenDeleteEvent.getToken(), EventsFrameworkMetadataConstants.DELETE_ACTION);
    AuditEntry auditEntry =
        AuditEntry.builder()
            .action(Action.DELETE)
            .module(ModuleType.CORE)
            .oldYaml(getYamlString(TokenRequest.builder().token(TokenDeleteEvent.getToken()).build()))
            .timestamp(outboxEvent.getCreatedAt())
            .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
            .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
            .insertId(outboxEvent.getId())
            .build();
    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean publishEvent(TokenDTO tokenDTO, String action) {
    try {
      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", tokenDTO.getAccountIdentifier(),
                  EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkMetadataConstants.TOKEN_ENTITY,
                  EventsFrameworkMetadataConstants.ACTION, action))
              .setData(EntityChangeDTO.newBuilder()
                           .setIdentifier(StringValue.of(tokenDTO.getIdentifier()))
                           .setOrgIdentifier(tokenDTO.getOrgIdentifier() != null
                                   ? StringValue.of(tokenDTO.getOrgIdentifier())
                                   : StringValue.of(""))
                           .setProjectIdentifier(tokenDTO.getProjectIdentifier() != null
                                   ? StringValue.of(tokenDTO.getProjectIdentifier())
                                   : StringValue.of(""))
                           .setIdentifier(StringValue.of(tokenDTO.getIdentifier()))
                           .build()
                           .toByteString())
              .build());
      return true;
    } catch (EventsFrameworkDownException e) {
      log.error(
          "Failed to send " + action + " event to events framework api key identifier: " + tokenDTO.getIdentifier(), e);
      return false;
    }
  }
}
