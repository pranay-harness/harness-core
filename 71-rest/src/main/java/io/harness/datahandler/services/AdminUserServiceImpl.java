package io.harness.datahandler.services;

import com.google.inject.Inject;

import software.wings.beans.User;
import software.wings.service.intfc.UserService;

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
