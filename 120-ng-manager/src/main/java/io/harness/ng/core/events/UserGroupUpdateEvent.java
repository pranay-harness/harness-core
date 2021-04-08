package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeConstants.USER_GROUP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.dto.UserGroupDTO;

import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Getter
@NoArgsConstructor
public class UserGroupUpdateEvent implements Event {
  private String accountIdentifier;
  private UserGroupDTO newUserGroup;
  private UserGroupDTO oldUserGroup;

  public UserGroupUpdateEvent(String accountIdentifier, UserGroupDTO newUserGroup, UserGroupDTO oldUserGroup) {
    this.accountIdentifier = accountIdentifier;
    this.newUserGroup = newUserGroup;
    this.oldUserGroup = oldUserGroup;
  }

  @Override
  public ResourceScope getResourceScope() {
    if (isEmpty(newUserGroup.getOrgIdentifier())) {
      return new AccountScope(accountIdentifier);
    } else if (isEmpty(newUserGroup.getProjectIdentifier())) {
      return new OrgScope(accountIdentifier, newUserGroup.getOrgIdentifier());
    }
    return new ProjectScope(accountIdentifier, newUserGroup.getOrgIdentifier(), newUserGroup.getIdentifier());
  }

  @Override
  public Resource getResource() {
    return Resource.builder().identifier(newUserGroup.getIdentifier()).type(USER_GROUP).build();
  }

  @Override
  public String getEventType() {
    return "UserGroupUpdated";
  }
}
