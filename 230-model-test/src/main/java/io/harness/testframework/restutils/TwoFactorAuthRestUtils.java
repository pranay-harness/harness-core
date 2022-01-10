/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import static org.apache.commons.codec.binary.Base64.encodeBase64String;

import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Retry;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.matchers.LoginMatcher;

import software.wings.beans.LoginRequest;
import software.wings.beans.User;
import software.wings.security.authentication.TwoFactorAuthenticationSettings;

import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.core.GenericType;
import org.jboss.aerogear.security.otp.Totp;

public class TwoFactorAuthRestUtils {
  /**
   * @param accountId
   * @param bearerToken
   * @return Two Factor Otp Settings Object
   */
  public static TwoFactorAuthenticationSettings getOtpSettings(String accountId, String bearerToken) {
    GenericType<RestResponse<TwoFactorAuthenticationSettings>> twofatype =
        new GenericType<RestResponse<TwoFactorAuthenticationSettings>>() {};
    RestResponse<TwoFactorAuthenticationSettings> restResponse = Setup.portal()
                                                                     .header("Authorization", "Bearer " + bearerToken)
                                                                     .queryParam("routingId", accountId)
                                                                     .get("/users/two-factor-auth/TOTP")
                                                                     .as(twofatype.getType());
    return restResponse.getResource();
  }

  /**
   * @param accountId
   * @param bearerToken
   * @param twoFaSettings
   * @return User Object
   */
  public static User enableTwoFactorAuthentication(
      String accountId, String bearerToken, TwoFactorAuthenticationSettings twoFaSettings) {
    GenericType<RestResponse<User>> genericType = new GenericType<RestResponse<User>>() {};
    RestResponse<User> userRestResponse = Setup.portal()
                                              .auth()
                                              .oauth2(bearerToken)
                                              .queryParam("routingId", accountId)
                                              .body(twoFaSettings, ObjectMapperType.GSON)
                                              .contentType(ContentType.JSON)
                                              .put("/users/enable-two-factor-auth")
                                              .as(genericType.getType());
    return userRestResponse.getResource();
  }

  /**
   * @param accountId
   * @param bearerToken
   * @return User Object
   */
  public static User enableTwoFactorAuthentication(String accountId, String bearerToken) {
    TwoFactorAuthenticationSettings twoFaSettings = getOtpSettings(accountId, bearerToken);
    GenericType<RestResponse<User>> genericType = new GenericType<RestResponse<User>>() {};
    RestResponse<User> userRestResponse = Setup.portal()
                                              .auth()
                                              .oauth2(bearerToken)
                                              .queryParam("routingId", accountId)
                                              .body(twoFaSettings, ObjectMapperType.GSON)
                                              .contentType(ContentType.JSON)
                                              .put("/users/enable-two-factor-auth")
                                              .as(genericType.getType());
    return userRestResponse.getResource();
  }

  /**
   *
   * @param userName
   * @param password
   * @param accountId
   * @param secretKey
   * @return User Object
   */
  public static User twoFaLogin(String userName, String password, String accountId, String secretKey) {
    GenericType<RestResponse<User>> genericType = new GenericType<RestResponse<User>>() {};
    String basicAuthValue =
        "Basic " + encodeBase64String(String.format("%s:%s", userName, password).getBytes(StandardCharsets.UTF_8));
    LoginRequest loginRequest = LoginRequest.builder().authorization(basicAuthValue).build();
    RestResponse<User> userRestResponse =
        Setup.portal().body(loginRequest).post("/users/login").as(genericType.getType());
    User user = userRestResponse.getResource();
    String twoFactorJwtToken = user.getTwoFactorJwtToken();
    Totp otp = new Totp(secretKey);
    String twoFactorCode = otp.now();
    basicAuthValue = "JWT "
        + encodeBase64String(String.format("%s:%s", twoFactorJwtToken, twoFactorCode).getBytes(StandardCharsets.UTF_8));
    loginRequest = LoginRequest.builder().authorization(basicAuthValue).build();
    userRestResponse = Setup.portal()
                           .body(loginRequest)
                           .queryParam("routingId", accountId)
                           .post("/users/two-factor-login")
                           .as(genericType.getType());
    user = userRestResponse.getResource();
    return user;
  }

  /**
   *
   * @param accountId
   * @param bearerToken
   * @return user object
   */
  public static User disableTwoFactorAuthentication(String accountId, String bearerToken) {
    GenericType<RestResponse<User>> genericType = new GenericType<RestResponse<User>>() {};
    RestResponse<User> userRestResponse = Setup.portal()
                                              .auth()
                                              .oauth2(bearerToken)
                                              .queryParam("routingId", accountId)
                                              .put("/users/disable-two-factor-auth")
                                              .as(genericType.getType());
    return userRestResponse.getResource();
  }

  public static User retryTwoFaLogin(String userName, String password, String accountId, String secretKey) {
    Retry retry = new Retry(5, 5000);
    return (User) retry.executeWithRetry(
        () -> twoFaLogin(userName, password, accountId, secretKey), new LoginMatcher(), null);
  }
}
