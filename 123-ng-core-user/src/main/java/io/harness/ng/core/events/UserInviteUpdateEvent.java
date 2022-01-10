/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeConstants.USER;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.invites.dto.InviteDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
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
    Map<String, String> labels = new HashMap<>();
    if (isNotEmpty(newInvite.getName())) {
      labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, newInvite.getName());
    }
    return Resource.builder().identifier(newInvite.getEmail()).type(USER).labels(labels).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return "UserInviteUpdated";
  }
}
