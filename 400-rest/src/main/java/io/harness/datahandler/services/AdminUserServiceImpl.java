/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.datahandler.services;

import software.wings.beans.User;
import software.wings.service.intfc.UserService;

import com.google.inject.Inject;
import java.util.Objects;

public class AdminUserServiceImpl implements AdminUserService {
  @Inject private UserService userService;

  @Override
  public boolean enableOrDisableUser(String accountId, String userIdOrEmail, boolean enabled) {
    User user = userService.getUserByEmail(userIdOrEmail);
    if (Objects.isNull(user)) {
      user = userService.get(userIdOrEmail);
    }
    return userService.enableUser(accountId, user.getUuid(), enabled);
  }
}
