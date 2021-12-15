/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.outbox;

import static io.harness.ng.core.utils.NGYamlUtils.getYamlString;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.beans.ResourceDTO;
import io.harness.audit.beans.ResourceScopeDTO;
import io.harness.audit.client.api.AuditClientService;
import io.harness.context.GlobalContext;
import io.harness.delegate.events.DelegateGroupDeleteEvent;
import io.harness.delegate.events.DelegateGroupUpsertEvent;
import io.harness.delegate.events.DelegateNgTokenCreateEvent;
import io.harness.delegate.events.DelegateNgTokenRevokeEvent;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.ng.core.ProjectScope;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.remote.NGObjectMapperHelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.StringValue;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class DelegateOutboxEventHandler implements OutboxEventHandler {
  private final ObjectMapper objectMapper;
  private final AuditClientService auditClientService;
  private final Producer eventProducer;

  @Inject
  DelegateOutboxEventHandler(
      AuditClientService auditClientService, @Named(EventsFrameworkConstants.ENTITY_CRUD) Producer eventProducer) {
    this.objectMapper = NGObjectMapperHelper.NG_DEFAULT_OBJECT_MAPPER;
    this.auditClientService = auditClientService;
    this.eventProducer = eventProducer;
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    try {
      switch (outboxEvent.getEventType()) {
        case "DelegateGroupUpsertEvent":
          return handleDelegateUpsertEvent(outboxEvent);
        case "DelegateGroupDeleteEvent":
          return handleDelegateDeleteEvent(outboxEvent);
        case DelegateNgTokenCreateEvent.DELEGATE_TOKEN_CREATE_EVENT:
          return handleDelegateNgTokenCreateEvent(outboxEvent);
        case DelegateNgTokenRevokeEvent.DELEGATE_TOKEN_REVOKE_EVENT:
          return handleDelegateNgTokenRevokeEvent(outboxEvent);
        default:
          return false;
      }
    } catch (IOException exception) {
      return false;
    }
  }

  private boolean handleDelegateUpsertEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    DelegateGroupUpsertEvent delegateGroupUpsertEvent =
        objectMapper.readValue(outboxEvent.getEventData(), DelegateGroupUpsertEvent.class);
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.UPSERT)
                                .module(ModuleType.CORE)
                                .newYaml(getYamlString(delegateGroupUpsertEvent.getDelegateSetupDetails()))
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();
    return auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleDelegateDeleteEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    DelegateGroupDeleteEvent delegateGroupDeleteEvent =
        objectMapper.readValue(outboxEvent.getEventData(), DelegateGroupDeleteEvent.class);
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.DELETE)
                                .module(ModuleType.CORE)
                                .newYaml(getYamlString(delegateGroupDeleteEvent.getDelegateSetupDetails()))
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();
    return auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleDelegateNgTokenCreateEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();

    String accountIdentifier = ((ProjectScope) outboxEvent.getResourceScope()).getAccountIdentifier();
    String orgIdentifier = ((ProjectScope) outboxEvent.getResourceScope()).getOrgIdentifier();
    String projectIdentifier = ((ProjectScope) outboxEvent.getResourceScope()).getProjectIdentifier();
    boolean publishedToRedis = publishEvent(accountIdentifier, orgIdentifier, projectIdentifier,
        outboxEvent.getResource().getIdentifier(), EventsFrameworkMetadataConstants.CREATE_ACTION);

    DelegateNgTokenCreateEvent delegateNgTokenCreateEvent =
        objectMapper.readValue(outboxEvent.getEventData(), DelegateNgTokenCreateEvent.class);
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.CREATE)
                                .module(ModuleType.CORE)
                                .newYaml(getYamlString(delegateNgTokenCreateEvent.getToken()))
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();
    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleDelegateNgTokenRevokeEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();

    String accountIdentifier = ((ProjectScope) outboxEvent.getResourceScope()).getAccountIdentifier();
    String orgIdentifier = ((ProjectScope) outboxEvent.getResourceScope()).getOrgIdentifier();
    String projectIdentifier = ((ProjectScope) outboxEvent.getResourceScope()).getProjectIdentifier();
    boolean publishedToRedis = publishEvent(accountIdentifier, orgIdentifier, projectIdentifier,
        outboxEvent.getResource().getIdentifier(), EventsFrameworkMetadataConstants.REVOKE_ACTION);

    DelegateNgTokenRevokeEvent delegateNgTokenRevokeEvent =
        objectMapper.readValue(outboxEvent.getEventData(), DelegateNgTokenRevokeEvent.class);
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.REVOKE_TOKEN)
                                .module(ModuleType.CORE)
                                .newYaml(getYamlString(delegateNgTokenRevokeEvent.getToken()))
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();
    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean publishEvent(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier, String action) {
    try {
      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(
                  ImmutableMap.of("accountId", accountIdentifier, EventsFrameworkMetadataConstants.ENTITY_TYPE,
                      EventsFrameworkMetadataConstants.DELEGATE_NG_TOKEN_ENTITY,
                      EventsFrameworkMetadataConstants.ACTION, action))
              .setData(EntityChangeDTO.newBuilder()
                           .setIdentifier(StringValue.of(identifier))
                           .setOrgIdentifier(orgIdentifier != null ? StringValue.of(orgIdentifier) : StringValue.of(""))
                           .setProjectIdentifier(
                               projectIdentifier != null ? StringValue.of(projectIdentifier) : StringValue.of(""))
                           .setIdentifier(StringValue.of(identifier))
                           .build()
                           .toByteString())
              .build());
      return true;
    } catch (EventsFrameworkDownException e) {
      log.error("Failed to send " + action + " event to events framework api for Delegate NG Token: " + identifier, e);
      return false;
    }
  }
}
