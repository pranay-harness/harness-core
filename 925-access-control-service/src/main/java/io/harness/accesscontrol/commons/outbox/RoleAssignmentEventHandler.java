/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.commons.outbox;

import static io.harness.accesscontrol.roleassignments.events.RoleAssignmentCreateEvent.ROLE_ASSIGNMENT_CREATE_EVENT;
import static io.harness.accesscontrol.roleassignments.events.RoleAssignmentDeleteEvent.ROLE_ASSIGNMENT_DELETE_EVENT;
import static io.harness.accesscontrol.roleassignments.events.RoleAssignmentUpdateEvent.ROLE_ASSIGNMENT_UPDATE_EVENT;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.remote.NGObjectMapperHelper.NG_DEFAULT_OBJECT_MAPPER;

import io.harness.ModuleType;
import io.harness.accesscontrol.roleassignments.events.RoleAssignmentCreateEvent;
import io.harness.accesscontrol.roleassignments.events.RoleAssignmentDeleteEvent;
import io.harness.accesscontrol.roleassignments.events.RoleAssignmentUpdateEvent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.beans.ResourceDTO;
import io.harness.audit.beans.ResourceScopeDTO;
import io.harness.audit.client.api.AuditClientService;
import io.harness.context.GlobalContext;
import io.harness.exception.InvalidArgumentsException;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class RoleAssignmentEventHandler implements OutboxEventHandler {
  private final ObjectMapper objectMapper;
  private final AuditClientService auditClientService;

  @Inject
  public RoleAssignmentEventHandler(AuditClientService auditClientService) {
    this.objectMapper = NG_DEFAULT_OBJECT_MAPPER;
    this.auditClientService = auditClientService;
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    try {
      switch (outboxEvent.getEventType()) {
        case ROLE_ASSIGNMENT_CREATE_EVENT:
          return handleRoleAssignmentCreateEvent(outboxEvent);
        case ROLE_ASSIGNMENT_UPDATE_EVENT:
          return handleRoleAssignmentUpdateEvent(outboxEvent);
        case ROLE_ASSIGNMENT_DELETE_EVENT:
          return handleRoleAssignmentDeleteEvent(outboxEvent);
        default:
          throw new InvalidArgumentsException(String.format("Not supported event type %s", outboxEvent.getEventType()));
      }
    } catch (IOException exception) {
      log.error(
          String.format("IOException occurred during handling outbox event of type %s", outboxEvent.getEventType()),
          exception);
      return false;
    }
  }

  private boolean handleRoleAssignmentCreateEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    RoleAssignmentCreateEvent roleAssignmentCreateEvent =
        objectMapper.readValue(outboxEvent.getEventData(), RoleAssignmentCreateEvent.class);
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.CREATE)
                                .module(ModuleType.CORE)
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();
    return auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleRoleAssignmentUpdateEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    RoleAssignmentUpdateEvent roleAssignmentUpdateEvent =
        objectMapper.readValue(outboxEvent.getEventData(), RoleAssignmentUpdateEvent.class);
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.UPDATE)
                                .module(ModuleType.CORE)
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();
    return auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleRoleAssignmentDeleteEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    RoleAssignmentDeleteEvent roleAssignmentDeleteEvent =
        objectMapper.readValue(outboxEvent.getEventData(), RoleAssignmentDeleteEvent.class);
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.DELETE)
                                .module(ModuleType.CORE)
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();
    return auditClientService.publishAudit(auditEntry, globalContext);
  }
}
