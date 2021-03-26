package io.harness.ng.core.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.ActionConstants;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.client.api.AuditClientService;
import io.harness.context.GlobalContext;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.eventsframework.entity_crud.project.ProjectEntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.auditevent.ProjectCreateEvent;
import io.harness.ng.core.auditevent.ProjectDeleteEvent;
import io.harness.ng.core.auditevent.ProjectRestoreEvent;
import io.harness.ng.core.auditevent.ProjectUpdateEvent;
import io.harness.ng.core.dto.ProjectRequest;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.scope.ResourceScope;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class ProjectEventHandler implements OutboxEventHandler {
  private final ObjectMapper objectMapper;
  private final Producer eventProducer;
  private final AuditClientService auditClientService;
  private final ObjectMapper yamlObjectMapper;

  @Inject
  public ProjectEventHandler(ObjectMapper objectMapper,
      @Named(EventsFrameworkConstants.ENTITY_CRUD) Producer eventProducer, AuditClientService auditClientService) {
    this.objectMapper = objectMapper;
    this.eventProducer = eventProducer;
    this.auditClientService = auditClientService;
    this.yamlObjectMapper = new ObjectMapper(new YAMLFactory());
  }

  public boolean handle(OutboxEvent outboxEvent) {
    try {
      switch (outboxEvent.getEventType()) {
        case "ProjectCreated":
          return handleProjectCreateEvent(outboxEvent);
        case "ProjectUpdated":
          return handleProjectUpdateEvent(outboxEvent);
        case "ProjectDeleted":
          return handleProjectDeleteEvent(outboxEvent);
        case "ProjectRestored":
          return handleProjectRestoreEvent(outboxEvent);
        default:
          throw new IllegalArgumentException("Not supported event type {}".format(outboxEvent.getEventType()));
      }
    } catch (IOException ioe) {
      return false;
    }
  }

  private boolean handleProjectCreateEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();

    boolean publishedToRedis = publishEvent(((OrgScope) outboxEvent.getResourceScope()).getAccountIdentifier(),
        ((OrgScope) outboxEvent.getResourceScope()).getOrgIdentifier(), outboxEvent.getResource().getIdentifier(),
        EventsFrameworkMetadataConstants.CREATE_ACTION);
    ProjectCreateEvent projectCreateEvent =
        objectMapper.readValue(outboxEvent.getEventData(), ProjectCreateEvent.class);
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(ActionConstants.CREATE_ACTION)
                                .module(ModuleType.CORE)
                                .newYaml(yamlObjectMapper.writeValueAsString(
                                    ProjectRequest.builder().project(projectCreateEvent.getProject()).build()))
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(outboxEvent.getResource())
                                .resourceScope(ResourceScope.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();
    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleProjectUpdateEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();

    boolean publishedToRedis = publishEvent(((OrgScope) outboxEvent.getResourceScope()).getAccountIdentifier(),
        ((OrgScope) outboxEvent.getResourceScope()).getOrgIdentifier(), outboxEvent.getResource().getIdentifier(),
        EventsFrameworkMetadataConstants.UPDATE_ACTION);
    ProjectUpdateEvent projectUpdateEvent =
        objectMapper.readValue(outboxEvent.getEventData(), ProjectUpdateEvent.class);
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(ActionConstants.UPDATE_ACTION)
                                .module(ModuleType.CORE)
                                .newYaml(yamlObjectMapper.writeValueAsString(
                                    ProjectRequest.builder().project(projectUpdateEvent.getNewProject()).build()))
                                .oldYaml(yamlObjectMapper.writeValueAsString(
                                    ProjectRequest.builder().project(projectUpdateEvent.getOldProject()).build()))
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(outboxEvent.getResource())
                                .resourceScope(ResourceScope.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();
    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleProjectDeleteEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();

    boolean publishedToRedis = publishEvent(((OrgScope) outboxEvent.getResourceScope()).getAccountIdentifier(),
        ((OrgScope) outboxEvent.getResourceScope()).getOrgIdentifier(), outboxEvent.getResource().getIdentifier(),
        EventsFrameworkMetadataConstants.DELETE_ACTION);
    ProjectDeleteEvent projectDeleteEvent =
        objectMapper.readValue(outboxEvent.getEventData(), ProjectDeleteEvent.class);
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(ActionConstants.DELETE_ACTION)
                                .module(ModuleType.CORE)
                                .newYaml(yamlObjectMapper.writeValueAsString(
                                    ProjectRequest.builder().project(projectDeleteEvent.getProject()).build()))
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(outboxEvent.getResource())
                                .resourceScope(ResourceScope.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();
    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleProjectRestoreEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();

    boolean publishedToRedis = publishEvent(((OrgScope) outboxEvent.getResourceScope()).getAccountIdentifier(),
        ((OrgScope) outboxEvent.getResourceScope()).getOrgIdentifier(), outboxEvent.getResource().getIdentifier(),
        EventsFrameworkMetadataConstants.RESTORE_ACTION);
    ProjectRestoreEvent projectRestoreEvent =
        objectMapper.readValue(outboxEvent.getEventData(), ProjectRestoreEvent.class);
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(ActionConstants.RESTORE_ACTION)
                                .module(ModuleType.CORE)
                                .newYaml(yamlObjectMapper.writeValueAsString(
                                    ProjectRequest.builder().project(projectRestoreEvent.getProject()).build()))
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(outboxEvent.getResource())
                                .resourceScope(ResourceScope.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();
    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean publishEvent(String accountIdentifier, String orgIdentifier, String identifier, String action) {
    try {
      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(
                  ImmutableMap.of("accountId", accountIdentifier, EventsFrameworkMetadataConstants.ENTITY_TYPE,
                      EventsFrameworkMetadataConstants.PROJECT_ENTITY, EventsFrameworkMetadataConstants.ACTION, action))
              .setData(ProjectEntityChangeDTO.newBuilder()
                           .setIdentifier(identifier)
                           .setOrgIdentifier(orgIdentifier)
                           .setAccountIdentifier(accountIdentifier)
                           .build()
                           .toByteString())
              .build());
      return true;
    } catch (ProducerShutdownException e) {
      log.error("Failed to send event to events framework projectIdentifier: " + identifier, e);
      return false;
    }
  }
}
