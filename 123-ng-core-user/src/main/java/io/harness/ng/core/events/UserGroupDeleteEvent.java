/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.dto.UserGroupDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Getter
@NoArgsConstructor
public class UserGroupDeleteEvent implements Event {
  private String accountIdentifier;
  private UserGroupDTO userGroup;

  public UserGroupDeleteEvent(String accountIdentifier, UserGroupDTO userGroup) {
    this.accountIdentifier = accountIdentifier;
    this.userGroup = userGroup;
  }

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    if (isEmpty(userGroup.getOrgIdentifier())) {
      return new AccountScope(accountIdentifier);
    } else if (isEmpty(userGroup.getProjectIdentifier())) {
      return new OrgScope(accountIdentifier, userGroup.getOrgIdentifier());
    }
    return new ProjectScope(accountIdentifier, userGroup.getOrgIdentifier(), userGroup.getProjectIdentifier());
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, userGroup.getName());
    return Resource.builder().identifier(userGroup.getIdentifier()).type(USER_GROUP).labels(labels).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return "UserGroupDeleted";
  }
}
