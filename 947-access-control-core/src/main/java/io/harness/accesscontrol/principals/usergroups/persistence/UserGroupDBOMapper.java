/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.principals.usergroups.persistence;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.principals.usergroups.UserGroup;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class UserGroupDBOMapper {
  public static UserGroupDBO toDBO(UserGroup object) {
    return UserGroupDBO.builder()
        .identifier(object.getIdentifier())
        .scopeIdentifier(object.getScopeIdentifier())
        .name(object.getName())
        .users(object.getUsers())
        .createdAt(object.getCreatedAt())
        .lastModifiedAt(object.getLastModifiedAt())
        .version(object.getVersion())
        .build();
  }

  public static UserGroup fromDBO(UserGroupDBO object) {
    return UserGroup.builder()
        .identifier(object.getIdentifier())
        .scopeIdentifier(object.getScopeIdentifier())
        .name(object.getName())
        .users(object.getUsers())
        .createdAt(object.getCreatedAt())
        .lastModifiedAt(object.getLastModifiedAt())
        .version(object.getVersion())
        .build();
  }
}
