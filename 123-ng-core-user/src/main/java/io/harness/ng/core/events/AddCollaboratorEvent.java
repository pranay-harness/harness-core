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
import io.harness.beans.Scope;
import io.harness.event.Event;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.user.UserMembershipUpdateSource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
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
  String userName;
  UserMembershipUpdateSource source;

  public AddCollaboratorEvent(String accountIdentifier, Scope scope, String email, String userId, String userName,
      UserMembershipUpdateSource source) {
    this.scope = scope;
    this.accountIdentifier = accountIdentifier;
    this.email = email;
    this.userId = userId;
    this.userName = userName;
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
    Map<String, String> labels = new HashMap<>();
    if (isNotEmpty(userName)) {
      labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, userName);
    }
    labels.put(ResourceConstants.LABEL_KEY_USER_ID, userId);
    return Resource.builder().identifier(email).type(USER).labels(labels).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return "CollaboratorAdded";
  }
}
