/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.testframework.restutils;

import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.beans.loginSettings.LoginSettings;
import software.wings.beans.loginSettings.PasswordStrengthPolicy;
import software.wings.beans.loginSettings.UserLockoutPolicy;

import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import javax.ws.rs.core.GenericType;

public class LoginSettingsUtils {
  private static String ACCOUNT_ID = "accountId";
  private static String LOGIN_SETTINGS_ENDPOINT = "/loginSettings";

  public static LoginSettings passwordStrengthPolicyUpdate(
      String bearerToken, String accountId, PasswordStrengthPolicy passwordStrengthPolicy) {
    final String passwordStrengthPolicyEndpoint = LOGIN_SETTINGS_ENDPOINT + "/update-password-strength-settings";
    RestResponse<LoginSettings> loginSettingsRestResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam(ACCOUNT_ID, accountId)
            .body(passwordStrengthPolicy, ObjectMapperType.GSON)
            .contentType(ContentType.JSON)
            .put(passwordStrengthPolicyEndpoint)
            .as(new GenericType<RestResponse<LoginSettings>>() {}.getType());
    return loginSettingsRestResponse.getResource();
  }

  public static LoginSettings userLockoutPolicyUpdate(
      String bearerToken, String accountId, UserLockoutPolicy userLockoutPolicy) {
    final String userLockoutPolicyEndpoint = LOGIN_SETTINGS_ENDPOINT + "/update-lockout-settings";
    RestResponse<LoginSettings> loginSettingsRestResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam(ACCOUNT_ID, accountId)
            .body(userLockoutPolicy, ObjectMapperType.GSON)
            .contentType(ContentType.JSON)
            .put(userLockoutPolicyEndpoint)
            .as(new GenericType<RestResponse<LoginSettings>>() {}.getType());
    return loginSettingsRestResponse.getResource();
  }
}
