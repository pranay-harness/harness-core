/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
