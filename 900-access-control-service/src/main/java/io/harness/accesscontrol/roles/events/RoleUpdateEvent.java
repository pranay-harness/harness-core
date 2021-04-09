package io.harness.accesscontrol.roles.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeConstants.ROLE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.accesscontrol.roles.api.RoleDTO;
import io.harness.accesscontrol.scopes.ScopeDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;

import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Getter
@NoArgsConstructor
public class RoleUpdateEvent implements Event {
  private String accountIdentifier;
  private RoleDTO newRole;
  private RoleDTO oldRole;
  private ScopeDTO scope;

  public RoleUpdateEvent(String accountIdentifier, RoleDTO newRole, RoleDTO oldRole, ScopeDTO scope) {
    this.accountIdentifier = accountIdentifier;
    this.newRole = newRole;
    this.oldRole = oldRole;
    this.scope = scope;
  }

  @Override
  public ResourceScope getResourceScope() {
    if (isEmpty(scope.getOrgIdentifier())) {
      return new AccountScope(accountIdentifier);
    } else if (isEmpty(scope.getProjectIdentifier())) {
      return new OrgScope(accountIdentifier, scope.getOrgIdentifier());
    }
    return new ProjectScope(accountIdentifier, scope.getOrgIdentifier(), scope.getProjectIdentifier());
  }

  @Override
  public Resource getResource() {
    return Resource.builder().identifier(newRole.getIdentifier()).type(ROLE).build();
  }

  @Override
  public String getEventType() {
    return "RoleUpdated";
  }
}
