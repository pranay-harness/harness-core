package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.ResourceType;
import io.harness.event.Event;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.invites.dto.InviteDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
public class UserInviteCreateEvent implements Event {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  InviteDTO invite;

  public UserInviteCreateEvent(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, InviteDTO invite) {
    this.invite = invite;
    this.accountIdentifier = accountIdentifier;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
  }

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    if (isEmpty(orgIdentifier)) {
      return new AccountScope(accountIdentifier);
    } else if (isEmpty(projectIdentifier)) {
      return new OrgScope(accountIdentifier, orgIdentifier);
    }
    return new ProjectScope(accountIdentifier, orgIdentifier, projectIdentifier);
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    return Resource.builder().identifier(invite.getEmail()).type(ResourceType.USER.name()).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return "UserInviteCreated";
  }
}
