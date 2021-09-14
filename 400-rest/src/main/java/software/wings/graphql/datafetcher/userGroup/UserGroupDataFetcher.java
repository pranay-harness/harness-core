/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.userGroup;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HIterator;

import software.wings.beans.security.UserGroup;
import software.wings.beans.security.UserGroup.UserGroupKeys;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.type.usergroup.QLUserGroup;
import software.wings.graphql.schema.type.usergroup.QLUserGroup.QLUserGroupBuilder;
import software.wings.graphql.schema.type.usergroup.QLUserGroupQueryParameters;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.UserGroupService;

import com.google.inject.Inject;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class UserGroupDataFetcher extends AbstractObjectDataFetcher<QLUserGroup, QLUserGroupQueryParameters> {
  @Inject UserGroupService userGroupService;
  @Inject UserGroupController userGroupController;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.USER_PERMISSION_READ)
  public QLUserGroup fetch(QLUserGroupQueryParameters qlQuery, String accountId) {
    UserGroup userGroup = null;
    String userGroupId = qlQuery.getUserGroupId();
    if (userGroupId != null) {
      userGroup = userGroupService.get(accountId, userGroupId);
    }
    if (qlQuery.getName() != null) {
      try (HIterator<UserGroup> iterator = new HIterator<>(wingsPersistence.createQuery(UserGroup.class)
                                                               .filter(UserGroupKeys.name, qlQuery.getName())
                                                               .filter(UserGroupKeys.accountId, accountId)
                                                               .fetch())) {
        if (iterator.hasNext()) {
          userGroup = iterator.next();
        }
      }
    }
    if (userGroup == null) {
      throw new InvalidRequestException("No User Group exists");
    }

    final QLUserGroupBuilder builder = QLUserGroup.builder();
    userGroupController.populateUserGroupOutput(userGroup, builder);
    return builder.build();
  }
}
