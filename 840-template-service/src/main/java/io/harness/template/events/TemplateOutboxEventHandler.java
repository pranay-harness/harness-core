package io.harness.template.events;

import static io.harness.AuthorizationServiceHeader.TEMPLATE_SERVICE;
import static io.harness.audit.beans.AuthenticationInfoDTO.fromSecurityPrincipal;
import static io.harness.security.PrincipalContextData.PRINCIPAL_CONTEXT;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
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
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.security.PrincipalContextData;
import io.harness.security.dto.Principal;
import io.harness.security.dto.ServicePrincipal;
import io.harness.utils.NGObjectMapperHelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.StringValue;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDC)
public class TemplateOutboxEventHandler implements OutboxEventHandler {
  private ObjectMapper objectMapper;
  private final AuditClientService auditClientService;
  private final Producer eventProducer;

  @Inject
  public TemplateOutboxEventHandler(
      AuditClientService auditClientService, @Named(EventsFrameworkConstants.ENTITY_CRUD) Producer eventProducer) {
    this.objectMapper = NGObjectMapperHelper.NG_PIPELINE_OBJECT_MAPPER;
    this.auditClientService = auditClientService;
    this.eventProducer = eventProducer;
  }

  private boolean handleTemplateCreateEvent(OutboxEvent outboxEvent) throws IOException {
    TemplateCreateEvent templateCreateEvent =
        objectMapper.readValue(outboxEvent.getEventData(), TemplateCreateEvent.class);

    boolean publishedToRedis = publishEvent(outboxEvent, EventsFrameworkMetadataConstants.CREATE_ACTION);
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.CREATE)
                                .module(ModuleType.TEMPLATESERVICE)
                                .newYaml(templateCreateEvent.getTemplateEntity().getYaml())
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();
    return publishedToRedis && publishAudit(auditEntry, outboxEvent);
  }

  private boolean handleTemplateUpdateEvent(OutboxEvent outboxEvent) throws IOException {
    TemplateUpdateEvent templateUpdateEvent =
        objectMapper.readValue(outboxEvent.getEventData(), TemplateUpdateEvent.class);
    boolean publishedToRedis = publishEvent(outboxEvent, EventsFrameworkMetadataConstants.UPDATE_ACTION);
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.UPDATE)
                                .module(ModuleType.TEMPLATESERVICE)
                                .oldYaml(templateUpdateEvent.getOldTemplateEntity().getYaml())
                                .newYaml(templateUpdateEvent.getNewTemplateEntity().getYaml())
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();
    return publishedToRedis && publishAudit(auditEntry, outboxEvent);
  }

  private boolean handleTemplateDeleteEvent(OutboxEvent outboxEvent) throws IOException {
    TemplateDeleteEvent templateDeleteEvent =
        objectMapper.readValue(outboxEvent.getEventData(), TemplateDeleteEvent.class);
    boolean publishedToRedis = publishEvent(outboxEvent, EventsFrameworkMetadataConstants.DELETE_ACTION);
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.DELETE)
                                .module(ModuleType.TEMPLATESERVICE)
                                .oldYaml(templateDeleteEvent.getTemplateEntity().getYaml())
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();
    return publishedToRedis && publishAudit(auditEntry, outboxEvent);
  }

  private boolean publishAudit(AuditEntry auditEntry, OutboxEvent outboxEvent) {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    Principal principal = null;
    if (globalContext.get(PRINCIPAL_CONTEXT) == null) {
      principal = new ServicePrincipal(TEMPLATE_SERVICE.getServiceId());
    } else if (globalContext.get(PRINCIPAL_CONTEXT) != null) {
      principal = ((PrincipalContextData) globalContext.get(PRINCIPAL_CONTEXT)).getPrincipal();
    }
    return auditClientService.publishAudit(auditEntry, fromSecurityPrincipal(principal), globalContext);
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    try {
      switch (outboxEvent.getEventType()) {
        case TemplateOutboxEvents.TEMPLATE_VERSION_CREATED:
          return handleTemplateCreateEvent(outboxEvent);
        case TemplateOutboxEvents.TEMPLATE_VERSION_UPDATED:
          return handleTemplateUpdateEvent(outboxEvent);
        case TemplateOutboxEvents.TEMPLATE_VERSION_DELETED:
          return handleTemplateDeleteEvent(outboxEvent);
        default:
          return false;
      }
    } catch (IOException ex) {
      return false;
    }
  }

  private boolean publishEvent(OutboxEvent outboxEvent, String action) {
    try {
      EntityChangeDTO.Builder builder =
          EntityChangeDTO.newBuilder().setIdentifier(StringValue.of(outboxEvent.getResource().getIdentifier()));

      String accountIdentifier;
      if (outboxEvent.getResourceScope() instanceof AccountScope) {
        accountIdentifier = ((AccountScope) outboxEvent.getResourceScope()).getAccountIdentifier();
        builder.setAccountIdentifier(StringValue.of(accountIdentifier));
      } else if (outboxEvent.getResourceScope() instanceof OrgScope) {
        OrgScope resourceScope = (OrgScope) outboxEvent.getResourceScope();
        accountIdentifier = resourceScope.getAccountIdentifier();
        builder.setAccountIdentifier(StringValue.of(accountIdentifier));
        builder.setOrgIdentifier(StringValue.of(resourceScope.getOrgIdentifier()));
      } else {
        ProjectScope resourceScope = (ProjectScope) outboxEvent.getResourceScope();
        accountIdentifier = resourceScope.getAccountIdentifier();
        builder.setAccountIdentifier(StringValue.of(accountIdentifier));
        builder.setOrgIdentifier(StringValue.of(resourceScope.getOrgIdentifier()));
        builder.setProjectIdentifier(StringValue.of(resourceScope.getProjectIdentifier()));
      }

      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", accountIdentifier,
                  EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkMetadataConstants.TEMPLATE_ENTITY,
                  EventsFrameworkMetadataConstants.ACTION, action))
              .setData(builder.build().toByteString())
              .build());
      return true;
    } catch (EventsFrameworkDownException e) {
      log.error(
          "Failed to send event to events framework templateIdentifier: " + outboxEvent.getResource().getIdentifier(),
          e);
      return false;
    }
  }
}
