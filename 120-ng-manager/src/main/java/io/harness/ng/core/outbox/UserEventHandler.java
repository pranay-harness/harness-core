package io.harness.ng.core.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.invites.remote.RoleBindingMapper.toAuditRoleBindings;
import static io.harness.remote.NGObjectMapperHelper.NG_DEFAULT_OBJECT_MAPPER;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.beans.ResourceDTO;
import io.harness.audit.beans.ResourceScopeDTO;
import io.harness.audit.beans.custom.user.UserInviteAuditEventData;
import io.harness.audit.client.api.AuditClientService;
import io.harness.context.GlobalContext;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.events.UserInviteCreateEvent;
import io.harness.ng.core.events.UserInviteDeleteEvent;
import io.harness.ng.core.events.UserInviteUpdateEvent;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class UserEventHandler implements OutboxEventHandler {
  private final ObjectMapper objectMapper;
  private final Producer producer;
  private final AuditClientService auditClientService;

  @Inject
  public UserEventHandler(
      @Named(EventsFrameworkConstants.USERMEMBERSHIP) Producer producer, AuditClientService auditClientService) {
    this.objectMapper = NG_DEFAULT_OBJECT_MAPPER;
    this.producer = producer;
    this.auditClientService = auditClientService;
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    try {
      switch (outboxEvent.getEventType()) {
        case "UserInviteCreated":
          return handleUserInviteCreateEvent(outboxEvent);
        case "UserInviteUpdated":
          return handleUserInviteUpdateEvent(outboxEvent);
        case "UserInviteDeleted":
          return handleUserInviteDeleteEvent(outboxEvent);
        default:
          throw new InvalidArgumentsException(String.format("Not supported event type %s", outboxEvent.getEventType()));
      }
    } catch (IOException exception) {
      return false;
    }
  }

  private boolean handleUserInviteCreateEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    UserInviteCreateEvent userInviteCreateEvent =
        objectMapper.readValue(outboxEvent.getEventData(), UserInviteCreateEvent.class);
    UserInviteAuditEventData auditEventData =
        UserInviteAuditEventData.builder()
            .roleBindings(toAuditRoleBindings(userInviteCreateEvent.getInvite().getRoleBindings()))
            .build();
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.INVITE)
                                .module(ModuleType.CORE)
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .auditEventData(auditEventData)
                                .build();
    return auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleUserInviteUpdateEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    UserInviteUpdateEvent userInviteUpdateEvent =
        objectMapper.readValue(outboxEvent.getEventData(), UserInviteUpdateEvent.class);
    UserInviteAuditEventData auditEventData =
        UserInviteAuditEventData.builder()
            .roleBindings(toAuditRoleBindings(userInviteUpdateEvent.getNewInvite().getRoleBindings()))
            .build();
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.RESEND_INVITE)
                                .module(ModuleType.CORE)
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .auditEventData(auditEventData)
                                .build();
    return auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleUserInviteDeleteEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    UserInviteDeleteEvent userInviteDeleteEvent =
        objectMapper.readValue(outboxEvent.getEventData(), UserInviteDeleteEvent.class);
    UserInviteAuditEventData auditEventData =
        UserInviteAuditEventData.builder()
            .roleBindings(toAuditRoleBindings(userInviteDeleteEvent.getInvite().getRoleBindings()))
            .build();
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.REVOKE_INVITE)
                                .module(ModuleType.CORE)
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .auditEventData(auditEventData)
                                .build();
    return auditClientService.publishAudit(auditEntry, globalContext);
  }
}
