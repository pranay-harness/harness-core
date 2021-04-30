package io.harness.ng.core.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ORGANIZATION_ENTITY;
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
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.eventsframework.entity_crud.organization.OrganizationEntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.dto.OrganizationRequest;
import io.harness.ng.core.events.OrganizationCreateEvent;
import io.harness.ng.core.events.OrganizationDeleteEvent;
import io.harness.ng.core.events.OrganizationRestoreEvent;
import io.harness.ng.core.events.OrganizationUpdateEvent;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.remote.client.NGRestUtils;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroupclient.ResourceGroupResponse;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
import io.harness.security.SourcePrincipalContextData;
import io.harness.security.dto.Principal;
import io.harness.security.dto.UserPrincipal;
import io.harness.utils.ScopeUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class OrganizationEventHandler implements OutboxEventHandler {
  private static final String DEFAULT_RESOURCE_GROUP_NAME = "All Resources";
  private static final String DEFAULT_RESOURCE_GROUP_IDENTIFIER = "_all_resources";
  private static final String DESCRIPTION_FORMAT = "All the resources in this %s are included in this resource group.";
  private static final String ORG_ADMIN_ROLE = "_organization_admin";
  private final ObjectMapper objectMapper;
  private final Producer eventProducer;
  private final AuditClientService auditClientService;
  private final NgUserService ngUserService;
  private final ResourceGroupClient resourceGroupClient;

  @Inject
  public OrganizationEventHandler(@Named(EventsFrameworkConstants.ENTITY_CRUD) Producer eventProducer,
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
        case "OrganizationCreated":
          return handleOrganizationCreateEvent(outboxEvent);
        case "OrganizationUpdated":
          return handleOrganizationUpdateEvent(outboxEvent);
        case "OrganizationDeleted":
          return handleOrganizationDeleteEvent(outboxEvent);
        case "OrganizationRestored":
          return handleOrganizationRestoreEvent(outboxEvent);
        default:
          throw new InvalidArgumentsException(String.format("Not supported event type %s", outboxEvent.getEventType()));
      }
    } catch (IOException ioe) {
      return false;
    }
  }

  private boolean handleOrganizationCreateEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    String accountIdentifier;
    // TODO {karan} remove this if condition in a few days
    if ("account".equals(outboxEvent.getResourceScope().getScope())) {
      accountIdentifier = ((AccountScope) outboxEvent.getResourceScope()).getAccountIdentifier();
    } else {
      accountIdentifier = ((OrgScope) outboxEvent.getResourceScope()).getAccountIdentifier();
    }
    boolean publishedToRedis = publishOrganizationChangeEventToRedis(
        accountIdentifier, outboxEvent.getResource().getIdentifier(), EventsFrameworkMetadataConstants.CREATE_ACTION);
    OrganizationCreateEvent organizationCreateEvent =
        objectMapper.readValue(outboxEvent.getEventData(), OrganizationCreateEvent.class);
    AuditEntry auditEntry =
        AuditEntry.builder()
            .action(Action.CREATE)
            .module(ModuleType.CORE)
            .newYaml(getYamlString(
                OrganizationRequest.builder().organization(organizationCreateEvent.getOrganization()).build()))
            .timestamp(outboxEvent.getCreatedAt())
            .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
            .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
            .insertId(outboxEvent.getId())
            .build();
    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext)
        && setupOrgForUserAuthz(
            accountIdentifier, organizationCreateEvent.getOrganization().getIdentifier(), globalContext);
  }

  private boolean setupOrgForUserAuthz(String accountIdentifier, String orgIdentifier, GlobalContext globalContext) {
    createDefaultResourceGroup(accountIdentifier, orgIdentifier);
    if (!(globalContext.get(SOURCE_PRINCIPAL) instanceof SourcePrincipalContextData)) {
      return true;
    }
    Principal principal = ((SourcePrincipalContextData) globalContext.get(SOURCE_PRINCIPAL)).getPrincipal();
    if (principal instanceof UserPrincipal) {
      String userId = ((SourcePrincipalContextData) globalContext.get(SOURCE_PRINCIPAL)).getPrincipal().getName();
      ngUserService.addUserToScope(userId,
          Scope.builder().accountIdentifier(accountIdentifier).orgIdentifier(orgIdentifier).build(), ORG_ADMIN_ROLE,
          SYSTEM);
    }
    return true;
  }

  private void createDefaultResourceGroup(String accountIdentifier, String orgIdentifier) {
    try {
      ResourceGroupResponse resourceGroupResponse = NGRestUtils.getResponse(resourceGroupClient.getResourceGroup(
          DEFAULT_RESOURCE_GROUP_IDENTIFIER, accountIdentifier, orgIdentifier, null));
      if (resourceGroupResponse != null) {
        return;
      }
      ResourceGroupDTO resourceGroupDTO = getResourceGroupDTO(accountIdentifier, orgIdentifier);
      NGRestUtils.getResponse(
          resourceGroupClient.createManagedResourceGroup(accountIdentifier, orgIdentifier, null, resourceGroupDTO));
    } catch (Exception e) {
      log.error(
          "Couldn't create default resource group for {}", ScopeUtils.toString(accountIdentifier, orgIdentifier, null));
    }
  }

  private ResourceGroupDTO getResourceGroupDTO(String accountIdentifier, String orgIdentifier) {
    return ResourceGroupDTO.builder()
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(null)
        .name(DEFAULT_RESOURCE_GROUP_NAME)
        .identifier(DEFAULT_RESOURCE_GROUP_IDENTIFIER)
        .description(String.format(DESCRIPTION_FORMAT,
            ScopeUtils.getMostSignificantScope(accountIdentifier, orgIdentifier, null).toString().toLowerCase()))
        .resourceSelectors(Collections.emptyList())
        .fullScopeSelected(true)
        .build();
  }

  private boolean handleOrganizationUpdateEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    String accountIdentifier;
    if ("account".equals(outboxEvent.getResourceScope().getScope())) {
      accountIdentifier = ((AccountScope) outboxEvent.getResourceScope()).getAccountIdentifier();
    } else {
      accountIdentifier = ((OrgScope) outboxEvent.getResourceScope()).getAccountIdentifier();
    }
    boolean publishedToRedis = publishOrganizationChangeEventToRedis(
        accountIdentifier, outboxEvent.getResource().getIdentifier(), EventsFrameworkMetadataConstants.UPDATE_ACTION);
    OrganizationUpdateEvent organizationUpdateEvent =
        objectMapper.readValue(outboxEvent.getEventData(), OrganizationUpdateEvent.class);
    AuditEntry auditEntry =
        AuditEntry.builder()
            .action(Action.UPDATE)
            .module(ModuleType.CORE)
            .newYaml(getYamlString(
                OrganizationRequest.builder().organization(organizationUpdateEvent.getNewOrganization()).build()))
            .oldYaml(getYamlString(
                OrganizationRequest.builder().organization(organizationUpdateEvent.getOldOrganization()).build()))
            .timestamp(outboxEvent.getCreatedAt())
            .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
            .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
            .insertId(outboxEvent.getId())
            .build();

    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleOrganizationDeleteEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    String accountIdentifier;
    if ("account".equals(outboxEvent.getResourceScope().getScope())) {
      accountIdentifier = ((AccountScope) outboxEvent.getResourceScope()).getAccountIdentifier();
    } else {
      accountIdentifier = ((OrgScope) outboxEvent.getResourceScope()).getAccountIdentifier();
    }
    boolean publishedToRedis = publishOrganizationChangeEventToRedis(
        accountIdentifier, outboxEvent.getResource().getIdentifier(), EventsFrameworkMetadataConstants.DELETE_ACTION);
    OrganizationDeleteEvent organizationDeleteEvent =
        objectMapper.readValue(outboxEvent.getEventData(), OrganizationDeleteEvent.class);
    AuditEntry auditEntry =
        AuditEntry.builder()
            .action(Action.DELETE)
            .module(ModuleType.CORE)
            .oldYaml(getYamlString(
                OrganizationRequest.builder().organization(organizationDeleteEvent.getOrganization()).build()))
            .timestamp(outboxEvent.getCreatedAt())
            .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
            .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
            .insertId(outboxEvent.getId())
            .build();

    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleOrganizationRestoreEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    String accountIdentifier;
    if ("account".equals(outboxEvent.getResourceScope().getScope())) {
      accountIdentifier = ((AccountScope) outboxEvent.getResourceScope()).getAccountIdentifier();
    } else {
      accountIdentifier = ((OrgScope) outboxEvent.getResourceScope()).getAccountIdentifier();
    }
    boolean publishedToRedis = publishOrganizationChangeEventToRedis(
        accountIdentifier, outboxEvent.getResource().getIdentifier(), EventsFrameworkMetadataConstants.RESTORE_ACTION);
    OrganizationRestoreEvent organizationRestoreEvent =
        objectMapper.readValue(outboxEvent.getEventData(), OrganizationRestoreEvent.class);
    AuditEntry auditEntry =
        AuditEntry.builder()
            .action(Action.RESTORE)
            .module(ModuleType.CORE)
            .newYaml(getYamlString(
                OrganizationRequest.builder().organization(organizationRestoreEvent.getOrganization()).build()))
            .timestamp(outboxEvent.getCreatedAt())
            .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
            .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
            .insertId(outboxEvent.getId())
            .build();

    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean publishOrganizationChangeEventToRedis(String accountIdentifier, String identifier, String action) {
    try {
      eventProducer.send(Message.newBuilder()
                             .putAllMetadata(ImmutableMap.of("accountId", accountIdentifier,
                                 EventsFrameworkMetadataConstants.ENTITY_TYPE, ORGANIZATION_ENTITY,
                                 EventsFrameworkMetadataConstants.ACTION, action))
                             .setData(getOrganizationPayload(accountIdentifier, identifier))
                             .build());
    } catch (ProducerShutdownException e) {
      log.error("Failed to send event to events framework orgIdentifier: " + identifier, e);
      return false;
    }
    return true;
  }

  private ByteString getOrganizationPayload(String accountIdentifier, String identifier) {
    return OrganizationEntityChangeDTO.newBuilder()
        .setIdentifier(identifier)
        .setAccountIdentifier(accountIdentifier)
        .build()
        .toByteString();
  }
}
