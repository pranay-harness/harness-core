/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeConstants.USER;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
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
public class UserInviteUpdateEvent implements Event {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  InviteDTO newInvite;
  InviteDTO oldInvite;

  public UserInviteUpdateEvent(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      InviteDTO newInvite, InviteDTO oldInvite) {
    this.newInvite = newInvite;
    this.oldInvite = oldInvite;
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
    return Resource.builder().identifier(newInvite.getEmail()).type(USER).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return "UserInviteUpdated";
  }
}
