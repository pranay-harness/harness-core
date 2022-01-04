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
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Getter
@NoArgsConstructor
public class RoleDeleteEvent implements Event {
  public static final String ROLE_DELETE_EVENT = "RoleDeleted";
  private String accountIdentifier;
  private RoleDTO role;
  private ScopeDTO scope;

  public RoleDeleteEvent(String accountIdentifier, RoleDTO role, ScopeDTO scope) {
    this.accountIdentifier = accountIdentifier;
    this.role = role;
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
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, role.getName());
    return Resource.builder().identifier(role.getIdentifier()).type(ROLE).labels(labels).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return ROLE_DELETE_EVENT;
  }
}
