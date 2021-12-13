package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.ResourceType;
import io.harness.beans.Scope;
import io.harness.event.Event;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.user.UserMembershipUpdateSource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Getter
@NoArgsConstructor
public class AddCollaboratorEvent implements Event {
  String accountIdentifier;
  Scope scope;
  String email;
  String userId;
  UserMembershipUpdateSource source;

  public AddCollaboratorEvent(
      String accountIdentifier, Scope scope, String email, String userId, UserMembershipUpdateSource source) {
    this.scope = scope;
    this.accountIdentifier = accountIdentifier;
    this.email = email;
    this.userId = userId;
    this.source = source;
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
    return Resource.builder().identifier(email).type(ResourceType.USER.name()).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return "CollaboratorAdded";
  }
}
