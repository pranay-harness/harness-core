package io.harness.accesscontrol.roleassignments.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.scopes.ScopeDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.ResourceType;
import io.harness.event.Event;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Getter
@NoArgsConstructor
public class RoleAssignmentUpdateEvent implements Event {
  public static final String ROLE_ASSIGNMENT_UPDATE_EVENT = "RoleAssignmentUpdated";
  private String accountIdentifier;
  private RoleAssignmentDTO newRoleAssignment;
  private RoleAssignmentDTO oldRoleAssignment;
  private ScopeDTO scope;

  public RoleAssignmentUpdateEvent(String accountIdentifier, RoleAssignmentDTO newRoleAssignment,
      RoleAssignmentDTO oldRoleAssignment, ScopeDTO scope) {
    this.accountIdentifier = accountIdentifier;
    this.newRoleAssignment = newRoleAssignment;
    this.oldRoleAssignment = oldRoleAssignment;
    this.scope = scope;
  }

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    if (isEmpty(scope.getOrgIdentifier())) {
      return new AccountScope(accountIdentifier);
    } else if (isEmpty(scope.getProjectIdentifier())) {
      return new OrgScope(accountIdentifier, scope.getOrgIdentifier());
    }
    return new ProjectScope(accountIdentifier, scope.getOrgIdentifier(), scope.getProjectIdentifier());
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    return Resource.builder()
        .identifier(newRoleAssignment.getIdentifier())
        .type(ResourceType.ROLE_ASSIGNMENT.name())
        .build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return ROLE_ASSIGNMENT_UPDATE_EVENT;
  }
}
