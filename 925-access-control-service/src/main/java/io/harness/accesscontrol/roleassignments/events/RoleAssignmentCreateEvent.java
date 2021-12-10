package io.harness.accesscontrol.roleassignments.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeEnum.ROLE_ASSIGNMENT;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.scopes.ScopeDTO;
import io.harness.annotations.dev.OwnedBy;
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
public class RoleAssignmentCreateEvent implements Event {
  public static final String ROLE_ASSIGNMENT_CREATE_EVENT = "RoleAssignmentCreated";
  private String accountIdentifier;
  private RoleAssignmentDTO roleAssignment;
  private ScopeDTO scope;

  public RoleAssignmentCreateEvent(String accountIdentifier, RoleAssignmentDTO roleAssignment, ScopeDTO scope) {
    this.accountIdentifier = accountIdentifier;
    this.roleAssignment = roleAssignment;
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
    return Resource.builder().identifier(roleAssignment.getIdentifier()).type(Resource.Type.ROLE_ASSIGNMENT).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return ROLE_ASSIGNMENT_CREATE_EVENT;
  }
}
