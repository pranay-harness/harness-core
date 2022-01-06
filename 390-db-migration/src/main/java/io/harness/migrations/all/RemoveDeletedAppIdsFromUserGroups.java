/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import static software.wings.beans.Application.ApplicationKeys;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Application;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.UserGroup.UserGroupKeys;
import software.wings.dl.WingsPersistence;
import software.wings.security.AppFilter;
import software.wings.service.intfc.UserGroupService;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Migration script to cleanup orphan app ids from user groups
 */
@Slf4j
public class RemoveDeletedAppIdsFromUserGroups implements Migration {
  @Inject private WingsPersistence persistence;

  @Inject private UserGroupService userGroupService;

  @Override
  public void migrate() {
    try {
      log.info("Start - Deleting orphan app ids from user groups");

      Set<String> existingAppIds = persistence.createQuery(Application.class)
                                       .project(ApplicationKeys.appId, true)
                                       .asList()
                                       .stream()
                                       .map(Application::getAppId)
                                       .collect(Collectors.toSet());

      try (HIterator<UserGroup> userGroupIterator =
               new HIterator<>(persistence.createQuery(UserGroup.class, excludeAuthority)
                                   .project(UserGroup.ID_KEY2, true)
                                   .project(UserGroupKeys.accountId, true)
                                   .project(UserGroupKeys.appPermissions, true)
                                   .fetch())) {
        while (userGroupIterator.hasNext()) {
          final UserGroup userGroup = userGroupIterator.next();
          Set<String> deletedAppIds = getDeletedAppIds(userGroup, existingAppIds);
          userGroupService.removeAppIdsFromAppPermissions(userGroup, deletedAppIds);
        }
      }
      log.info("Deleting orphan app ids from user groups finished successfully");
    } catch (Exception ex) {
      log.error("Error while deleting orphan app ids from user groups", ex);
    }
  }

  private Set<String> getDeletedAppIds(UserGroup userGroup, Set<String> existingAppIds) {
    Set<String> deletedIds = new HashSet<>();

    if (isEmpty(existingAppIds)) {
      return deletedIds;
    }

    Set<AppPermission> groupAppPermissions = userGroup.getAppPermissions();

    if (isEmpty(groupAppPermissions)) {
      return deletedIds;
    }

    for (AppPermission permission : groupAppPermissions) {
      AppFilter filter = permission.getAppFilter();
      Set<String> ids = filter.getIds();
      if (isEmpty(ids)) {
        continue;
      }

      for (String id : ids) {
        if (!existingAppIds.contains(id)) {
          deletedIds.add(id);
        }
      }
    }

    return deletedIds;
  }
}
