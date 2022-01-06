/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.security.auth;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.ACCESS_DENIED;
import static io.harness.exception.WingsException.USER;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;

import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.intfc.UserService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(PL)
@TargetModule(HarnessModule.UNDEFINED)
public class DefaultsAuthHandler {
  @Inject UserService userService;

  public void authorizeUpdate(String appId, String accountId) {
    if (isEmpty(appId)) {
      throw new InvalidRequestException("Invalid argument, appId cannot be null/empty.");
    }
    if (!GLOBAL_APP_ID.equals(appId)) {
      if (!userService.hasPermission(accountId, PermissionType.MANAGE_APPLICATIONS)) {
        throw new InvalidRequestException(
            "User doesn't have rights to manage application defaults", ACCESS_DENIED, USER);
      }
    } else {
      if (!userService.hasPermission(accountId, PermissionType.MANAGE_ACCOUNT_DEFAULTS)) {
        throw new InvalidRequestException("User doesn't have rights to manage account defaults", ACCESS_DENIED, USER);
      }
    }
  }
}
