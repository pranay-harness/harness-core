package io.harness.accesscontrol.roles.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.accesscontrol.roles.api.RoleDTO;
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
public class RoleUpdateEvent implements Event {
  public static final String ROLE_UPDATE_EVENT = "RoleUpdated";
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
    return Resource.builder().identifier(newRole.getIdentifier()).type(ResourceType.ROLE.name()).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return ROLE_UPDATE_EVENT;
  }
}
