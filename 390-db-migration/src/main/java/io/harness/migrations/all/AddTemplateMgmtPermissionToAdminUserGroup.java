/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.UserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.intfc.UserGroupService;

import com.google.inject.Inject;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Add new TEMPLATE_MANAGEMENT permission type to admin user groups.
 * @author rktummala on 03/16/19
 */
@Slf4j
public class AddTemplateMgmtPermissionToAdminUserGroup implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UserGroupService userGroupService;

  @Override
  public void migrate() {
    log.info("Starting updating user groups with new permission type");
    UserGroup userGroup = null;
    try (HIterator<UserGroup> userGroups =
             new HIterator<>(wingsPersistence.createQuery(UserGroup.class, excludeAuthority).fetch())) {
      while (userGroups.hasNext()) {
        try {
          userGroup = userGroups.next();
          AccountPermissions accountPermissions = userGroup.getAccountPermissions();
          if (accountPermissions != null) {
            Set<PermissionType> accountPermissionsSet = accountPermissions.getPermissions();
            if (isNotEmpty(accountPermissionsSet)) {
              if (accountPermissionsSet.contains(PermissionType.ACCOUNT_MANAGEMENT)) {
                accountPermissionsSet.add(PermissionType.TEMPLATE_MANAGEMENT);
                userGroupService.updatePermissions(userGroup);
              }
            }
          }
        } catch (Exception ex) {
          log.error("Error while updating user group {}", userGroup != null ? userGroup.getUuid() : "NA", ex);
        }
      }
    }
    log.info("Completed updating user groups with new permission type");
  }
}
