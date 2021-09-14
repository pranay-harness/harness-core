/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.utils;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.InvalidRequestException;
import io.harness.rest.RestResponse;
import io.harness.rest.RestResponse.Builder;

import software.wings.beans.User;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.HarnessUserGroupService;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author marklu on 2019-02-08
 */
@OwnedBy(PL)
@Singleton
public class AccountPermissionUtils {
  @Inject private HarnessUserGroupService harnessUserGroupService;

  public <T> RestResponse<T> checkIfHarnessUser(String errorMessage) {
    User existingUser = UserThreadLocal.get();
    if (existingUser == null) {
      throw new InvalidRequestException("Invalid User");
    }

    RestResponse<T> errorResponse = null;
    if (!harnessUserGroupService.isHarnessSupportUser(existingUser.getUuid())) {
      errorResponse =
          Builder.aRestResponse()
              .withResponseMessages(Lists.newArrayList(ResponseMessage.builder().message(errorMessage).build()))
              .build();
    }
    return errorResponse;
  }
}
