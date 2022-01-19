/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.datahandler.services;

import static io.harness.NGConstants.DEFAULT_ACCOUNT_LEVEL_RESOURCE_GROUP_IDENTIFIER;

import static java.util.Collections.singletonList;

import io.harness.invites.remote.NgInviteClient;
import io.harness.ng.core.invites.dto.RoleBinding;
import io.harness.ng.core.user.AddUsersDTO;
import io.harness.remote.client.NGRestUtils;

import software.wings.beans.User;
import software.wings.service.intfc.UserService;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Objects;

public class AdminUserServiceImpl implements AdminUserService {
  @Inject private UserService userService;
  @Inject @Named("PRIVILEGED") private NgInviteClient ngInviteClient;
  private static final String ACCOUNT_ADMIN = "_account_admin";

  @Override
  public boolean enableOrDisableUser(String accountId, String userIdOrEmail, boolean enabled) {
    User user = userService.getUserByEmail(userIdOrEmail);
    if (Objects.isNull(user)) {
      user = userService.get(userIdOrEmail);
    }
    return userService.enableUser(accountId, user.getUuid(), enabled);
  }

  @Override
  public boolean assignAdminRoleToUserInNG(String accountId, String userIdOrEmail) {
    User user = userService.getUserByEmail(userIdOrEmail);
    if (Objects.isNull(user)) {
      user = userService.getUserByUserId(userIdOrEmail);
    }
    String email = userIdOrEmail;
    if (!Objects.isNull(user)) {
      email = user.getEmail();
    }
    AddUsersDTO addUsersDTO =
        AddUsersDTO.builder()
            .emails(singletonList(email))
            .roleBindings(singletonList(RoleBinding.builder()
                                            .roleIdentifier(ACCOUNT_ADMIN)
                                            .resourceGroupIdentifier(DEFAULT_ACCOUNT_LEVEL_RESOURCE_GROUP_IDENTIFIER)
                                            .build()))
            .build();
    NGRestUtils.getResponse(ngInviteClient.addUsers(accountId, null, null, addUsersDTO));
    return true;
  }
}
