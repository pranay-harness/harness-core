/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.remote.NGObjectMapperHelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class DelegateOutboxEventHandler implements OutboxEventHandler {
  private ObjectMapper objectMapper;
  private final AuditClientService auditClientService;
  @Inject
  DelegateOutboxEventHandler(AuditClientService auditClientService) {
    this.objectMapper = NGObjectMapperHelper.NG_DEFAULT_OBJECT_MAPPER;
    this.auditClientService = auditClientService;
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    try {
      switch (outboxEvent.getEventType()) {
        case "DelegateGroupUpsertEvent":
          return handleDelegateUpsertEvent(outboxEvent);
        case "DelegateGroupDeleteEvent":
          return handleDelegateDeleteEvent(outboxEvent);
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
}
