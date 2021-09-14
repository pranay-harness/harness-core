/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.migrations.Migration;

import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.UserService;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

/**
 * Migration script to migrate all the existing user groups
 * This script is meant to be idempotent, so it could be run any number of times.
 * @author rktummala on 3/15/18
 */
@Slf4j
public class CreateDefaultUserGroupsAndAddToExistingUsers implements Migration {
  @Inject private AuthHandler authHandler;
  @Inject private UserService userService;

  @Override
  public void migrate() {
    PageRequest<User> userPageRequest = aPageRequest().withLimit(UNLIMITED).build();
    PageResponse<User> userPageResponse = userService.list(userPageRequest, false);
    List<User> userList = userPageResponse.getResponse();

    if (userList != null) {
      userList.forEach(user -> {
        List<Account> accounts = user.getAccounts();
        if (CollectionUtils.isEmpty(accounts)) {
          log.info("User {} is not associated to any account", user.getName());
          return;
        }

        accounts.forEach(account -> authHandler.addUserToDefaultAccountAdminUserGroup(user, account, false));
      });
    }
  }
}
