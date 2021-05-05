package io.harness.ng.core.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.user.UserMembershipUpdateSource.SYSTEM;
import static io.harness.ng.core.utils.NGYamlUtils.getYamlString;
import static io.harness.remote.NGObjectMapperHelper.NG_DEFAULT_OBJECT_MAPPER;
import static io.harness.security.SourcePrincipalContextData.SOURCE_PRINCIPAL;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.beans.ResourceDTO;
import io.harness.audit.beans.ResourceScopeDTO;
import io.harness.audit.client.api.AuditClientService;
import io.harness.beans.Scope;
import io.harness.context.GlobalContext;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.entity_crud.project.ProjectEntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.dto.ProjectRequest;
import io.harness.ng.core.events.ProjectCreateEvent;
import io.harness.ng.core.events.ProjectDeleteEvent;
import io.harness.ng.core.events.ProjectRestoreEvent;
import io.harness.ng.core.events.ProjectUpdateEvent;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.remote.client.NGRestUtils;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroupclient.ResourceGroupResponse;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
import io.harness.security.SourcePrincipalContextData;
import io.harness.utils.ScopeUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.io.IOException;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class ProjectEventHandler implements OutboxEventHandler {
  private static final String DEFAULT_RESOURCE_GROUP_NAME = "All Resources";
  private static final String DEFAULT_RESOURCE_GROUP_IDENTIFIER = "_all_resources";
  private static final String DESCRIPTION_FORMAT = "All the resources in this %s are included in this resource group.";
  private static final String PROJECT_ADMIN_ROLE = "_project_admin";
  private final ObjectMapper objectMapper;
  private final Producer eventProducer;
  private final AuditClientService auditClientService;
  private final NgUserService ngUserService;
  private final ResourceGroupClient resourceGroupClient;

  @Inject
  public ProjectEventHandler(@Named(EventsFrameworkConstants.ENTITY_CRUD) Producer eventProducer,
      AuditClientService auditClientService, NgUserService ngUserService,
      @Named("PRIVILEGED") ResourceGroupClient resourceGroupClient) {
    this.objectMapper = NG_DEFAULT_OBJECT_MAPPER;
    this.eventProducer = eventProducer;
    this.auditClientService = auditClientService;
    this.ngUserService = ngUserService;
    this.resourceGroupClient = resourceGroupClient;
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
          throw new InvalidArgumentsException(String.format("Not supported event type %s", outboxEvent.getEventType()));
      }
    } catch (IOException exception) {
      return false;
    }
  }

  private boolean handleProjectCreateEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    String accountIdentifier;
    String orgIdentifier;
    // TODO {karan} remove this if condition in a few days
    if ("org".equals(outboxEvent.getResourceScope().getScope())) {
      accountIdentifier = ((OrgScope) outboxEvent.getResourceScope()).getAccountIdentifier();
      orgIdentifier = ((OrgScope) outboxEvent.getResourceScope()).getOrgIdentifier();
    } else {
      accountIdentifier = ((ProjectScope) outboxEvent.getResourceScope()).getAccountIdentifier();
      orgIdentifier = ((ProjectScope) outboxEvent.getResourceScope()).getOrgIdentifier();
    }
    boolean publishedToRedis = publishEvent(accountIdentifier, orgIdentifier, outboxEvent.getResource().getIdentifier(),
        EventsFrameworkMetadataConstants.CREATE_ACTION);
    ProjectCreateEvent projectCreateEvent =
        objectMapper.readValue(outboxEvent.getEventData(), ProjectCreateEvent.class);
    AuditEntry auditEntry =
        AuditEntry.builder()
            .action(Action.CREATE)
            .module(ModuleType.CORE)
            .newYaml(getYamlString(ProjectRequest.builder().project(projectCreateEvent.getProject()).build()))
            .timestamp(outboxEvent.getCreatedAt())
            .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
            .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
            .insertId(outboxEvent.getId())
            .build();
    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext)
        && setupProjectForUserAuthz(
            accountIdentifier, orgIdentifier, projectCreateEvent.getProject().getIdentifier(), globalContext);
  }

  private boolean setupProjectForUserAuthz(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, GlobalContext globalContext) {
    createDefaultResourceGroup(accountIdentifier, orgIdentifier, projectIdentifier);
    if (!(globalContext.get(SOURCE_PRINCIPAL) instanceof SourcePrincipalContextData)) {
      return false;
    }
    String userId = ((SourcePrincipalContextData) globalContext.get(SOURCE_PRINCIPAL)).getPrincipal().getName();
    ngUserService.addUserToScope(userId,
        Scope.builder()
            .accountIdentifier(accountIdentifier)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .build(),
        PROJECT_ADMIN_ROLE, SYSTEM);
    return true;
  }

  private void createDefaultResourceGroup(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    try {
      ResourceGroupResponse resourceGroupResponse = NGRestUtils.getResponse(resourceGroupClient.getResourceGroup(
          DEFAULT_RESOURCE_GROUP_IDENTIFIER, accountIdentifier, orgIdentifier, projectIdentifier));
      if (resourceGroupResponse != null) {
        return;
      }
      ResourceGroupDTO resourceGroupDTO = getResourceGroupDTO(accountIdentifier, orgIdentifier, projectIdentifier);
      NGRestUtils.getResponse(resourceGroupClient.createManagedResourceGroup(
          accountIdentifier, orgIdentifier, projectIdentifier, resourceGroupDTO));
    } catch (Exception e) {
      log.error("Couldn't create default resource group for {}",
          ScopeUtils.toString(accountIdentifier, orgIdentifier, projectIdentifier));
    }
  }

  private ResourceGroupDTO getResourceGroupDTO(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return ResourceGroupDTO.builder()
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .name(DEFAULT_RESOURCE_GROUP_NAME)
        .identifier(DEFAULT_RESOURCE_GROUP_IDENTIFIER)
        .description(String.format(DESCRIPTION_FORMAT,
            ScopeUtils.getMostSignificantScope(accountIdentifier, orgIdentifier, projectIdentifier)
                .toString()
                .toLowerCase()))
        .resourceSelectors(Collections.emptyList())
        .fullScopeSelected(true)
        .build();
  }

  private boolean handleProjectUpdateEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    String accountIdentifier;
    String orgIdentifier;
    if ("org".equals(outboxEvent.getResourceScope().getScope())) {
      accountIdentifier = ((OrgScope) outboxEvent.getResourceScope()).getAccountIdentifier();
      orgIdentifier = ((OrgScope) outboxEvent.getResourceScope()).getOrgIdentifier();
    } else {
      accountIdentifier = ((ProjectScope) outboxEvent.getResourceScope()).getAccountIdentifier();
      orgIdentifier = ((ProjectScope) outboxEvent.getResourceScope()).getOrgIdentifier();
    }
    boolean publishedToRedis = publishEvent(accountIdentifier, orgIdentifier, outboxEvent.getResource().getIdentifier(),
        EventsFrameworkMetadataConstants.UPDATE_ACTION);
    ProjectUpdateEvent projectUpdateEvent =
        objectMapper.readValue(outboxEvent.getEventData(), ProjectUpdateEvent.class);
    AuditEntry auditEntry =
        AuditEntry.builder()
            .action(Action.UPDATE)
            .module(ModuleType.CORE)
            .newYaml(getYamlString(ProjectRequest.builder().project(projectUpdateEvent.getNewProject()).build()))
            .oldYaml(getYamlString(ProjectRequest.builder().project(projectUpdateEvent.getOldProject()).build()))
            .timestamp(outboxEvent.getCreatedAt())
            .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
            .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
            .insertId(outboxEvent.getId())
            .build();
    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleProjectDeleteEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    String accountIdentifier;
    String orgIdentifier;
    if ("org".equals(outboxEvent.getResourceScope().getScope())) {
      accountIdentifier = ((OrgScope) outboxEvent.getResourceScope()).getAccountIdentifier();
      orgIdentifier = ((OrgScope) outboxEvent.getResourceScope()).getOrgIdentifier();
    } else {
      accountIdentifier = ((ProjectScope) outboxEvent.getResourceScope()).getAccountIdentifier();
      orgIdentifier = ((ProjectScope) outboxEvent.getResourceScope()).getOrgIdentifier();
    }
    boolean publishedToRedis = publishEvent(accountIdentifier, orgIdentifier, outboxEvent.getResource().getIdentifier(),
        EventsFrameworkMetadataConstants.DELETE_ACTION);
    ProjectDeleteEvent projectDeleteEvent =
        objectMapper.readValue(outboxEvent.getEventData(), ProjectDeleteEvent.class);
    AuditEntry auditEntry =
        AuditEntry.builder()
            .action(Action.DELETE)
            .module(ModuleType.CORE)
            .oldYaml(getYamlString(ProjectRequest.builder().project(projectDeleteEvent.getProject()).build()))
            .timestamp(outboxEvent.getCreatedAt())
            .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
            .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
            .insertId(outboxEvent.getId())
            .build();
    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleProjectRestoreEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    String accountIdentifier;
    String orgIdentifier;
    if ("org".equals(outboxEvent.getResourceScope().getScope())) {
      accountIdentifier = ((OrgScope) outboxEvent.getResourceScope()).getAccountIdentifier();
      orgIdentifier = ((OrgScope) outboxEvent.getResourceScope()).getOrgIdentifier();
    } else {
      accountIdentifier = ((ProjectScope) outboxEvent.getResourceScope()).getAccountIdentifier();
      orgIdentifier = ((ProjectScope) outboxEvent.getResourceScope()).getOrgIdentifier();
    }
    boolean publishedToRedis = publishEvent(accountIdentifier, orgIdentifier, outboxEvent.getResource().getIdentifier(),
        EventsFrameworkMetadataConstants.RESTORE_ACTION);
    ProjectRestoreEvent projectRestoreEvent =
        objectMapper.readValue(outboxEvent.getEventData(), ProjectRestoreEvent.class);
    AuditEntry auditEntry =
        AuditEntry.builder()
            .action(Action.RESTORE)
            .module(ModuleType.CORE)
            .newYaml(getYamlString(ProjectRequest.builder().project(projectRestoreEvent.getProject()).build()))
            .timestamp(outboxEvent.getCreatedAt())
            .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
            .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
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
    } catch (EventsFrameworkDownException e) {
      log.error("Failed to send event to events framework projectIdentifier: " + identifier, e);
      return false;
    }
  }
}
