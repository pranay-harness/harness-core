package io.harness.accesscontrol.scopes.harness.events;

import static io.harness.accesscontrol.common.filter.ManagedFilter.ONLY_CUSTOM;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;

import io.harness.accesscontrol.roleassignments.RoleAssignmentFilter;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.roles.filter.RoleFilter;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeParams;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeLevel;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.eventsframework.api.EventHandler;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.organization.OrganizationEntityChangeDTO;
import io.harness.eventsframework.entity_crud.project.ProjectEntityChangeDTO;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScopeEventHandler implements EventHandler {
  private final RoleAssignmentService roleAssignmentService;
  private final RoleService roleService;
  private final ScopeService scopeService;

  public ScopeEventHandler(
      RoleAssignmentService roleAssignmentService, RoleService roleService, ScopeService scopeService) {
    this.roleAssignmentService = roleAssignmentService;
    this.roleService = roleService;
    this.scopeService = scopeService;
  }

  @Override
  public boolean handle(Message message) {
    String entityType = message.getMessage().getMetadataMap().get(ENTITY_TYPE);
    if (entityType.equals(HarnessScopeLevel.ORGANIZATION.getEventEntityName())) {
      return handleOrganization(message);
    }
    if (entityType.equals(HarnessScopeLevel.PROJECT.getEventEntityName())) {
      return handleProject(message);
    }
    return true;
  }

  private boolean handleOrganization(Message message) {
    OrganizationEntityChangeDTO organizationEntityChangeDTO = null;
    try {
      organizationEntityChangeDTO = OrganizationEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking OrganizationEntityChangeDTO for key {}", message.getId(), e);
    }
    if (Objects.isNull(organizationEntityChangeDTO)) {
      return true;
    }
    try {
      ScopeParams scopeParams = HarnessScopeParams.builder()
                                    .accountIdentifier(organizationEntityChangeDTO.getAccountIdentifier())
                                    .orgIdentifier(organizationEntityChangeDTO.getIdentifier())
                                    .build();
      Scope scope = scopeService.buildScopeFromParams(scopeParams);
      deleteRoleAssignments(scope);
      deleteRoles(scope);
    } catch (Exception e) {
      log.error("Could not process the organization change event {} due to error", organizationEntityChangeDTO, e);
      return false;
    }
    return true;
  }

  private boolean handleProject(Message message) {
    ProjectEntityChangeDTO projectEntityChangeDTO = null;
    try {
      projectEntityChangeDTO = ProjectEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking ProjectEntityChangeDTO for key {}", message.getId(), e);
    }
    if (Objects.isNull(projectEntityChangeDTO)) {
      return true;
    }
    try {
      ScopeParams scopeParams = HarnessScopeParams.builder()
                                    .accountIdentifier(projectEntityChangeDTO.getAccountIdentifier())
                                    .orgIdentifier(projectEntityChangeDTO.getOrgIdentifier())
                                    .projectIdentifier(projectEntityChangeDTO.getIdentifier())
                                    .build();
      Scope scope = scopeService.buildScopeFromParams(scopeParams);
      deleteRoleAssignments(scope);
      deleteRoles(scope);
    } catch (Exception e) {
      log.error("Could not process the project change event {} due to error", projectEntityChangeDTO, e);
      return false;
    }
    return true;
  }

  private void deleteRoleAssignments(Scope scope) {
    RoleAssignmentFilter roleAssignmentFilter =
        RoleAssignmentFilter.builder().scopeFilter(scope.toString()).includeChildScopes(true).build();
    roleAssignmentService.deleteMulti(roleAssignmentFilter);
  }

  private void deleteRoles(Scope scope) {
    RoleFilter roleFilter = RoleFilter.builder()
                                .scopeIdentifier(scope.toString())
                                .managedFilter(ONLY_CUSTOM)
                                .includeChildScopes(true)
                                .build();
    roleService.deleteMulti(roleFilter);
  }
}
