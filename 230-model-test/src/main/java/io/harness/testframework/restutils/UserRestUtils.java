/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.FeatureFlag;
import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Registration;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.constants.UserConstants;

import software.wings.beans.Account;
import software.wings.beans.LoginRequest;
import software.wings.beans.PublicUser;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.resources.UserResource;

import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.core.GenericType;

public class UserRestUtils {
  public static List<PublicUser> getUserList(String bearerToken, String accountId) {
    RestResponse<PageResponse<PublicUser>> userRestResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .get("/users")
            .as(new GenericType<RestResponse<PageResponse<PublicUser>>>() {}.getType());
    return userRestResponse.getResource().getResponse();
  }

  public static List<UserInvite> inviteUser(Account account, String bearerToken, String email) {
    UserInvite invite = new UserInvite();
    invite.setAccountId(account.getUuid());
    List<String> emailList = new ArrayList<>();
    emailList.add(email);
    invite.setEmails(emailList);
    invite.setName(email.replace("@harness.mailinator.com", ""));
    invite.setAppId(account.getAppId());

    RestResponse<List<UserInvite>> inviteListResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", account.getUuid())
            .body(invite, ObjectMapperType.GSON)
            .contentType(ContentType.JSON)
            .post("/users/invites")
            .as(new GenericType<RestResponse<List<UserInvite>>>() {}.getType());
    List<UserInvite> inviteList = inviteListResponse.getResource();
    assertThat(inviteList).isNotNull();
    return inviteList;
  }

  public static User loginUserOrNull(String email, String password) {
    String basicAuthValue =
        "Basic " + encodeBase64String(String.format("%s:%s", email, password).getBytes(StandardCharsets.UTF_8));
    LoginRequest loginRequest = LoginRequest.builder().authorization(basicAuthValue).build();
    GenericType<RestResponse<User>> genericType = new GenericType<RestResponse<User>>() {};
    RestResponse<User> userRestResponse =
        Setup.portal().body(loginRequest).post("/users/login").as(genericType.getType());
    assertThat(userRestResponse).isNotNull();
    return userRestResponse.getResource();
  }

  public static void sendResetPasswordMail(String emailId) {
    UserResource.ResetPasswordRequest resetPasswordRequest = new UserResource.ResetPasswordRequest();
    resetPasswordRequest.setEmail(emailId);
    Setup.portal().body(resetPasswordRequest, ObjectMapperType.GSON).post("/users/reset-password");
  }

  public static void resetPasswordWith(String token, String password) throws UnsupportedEncodingException {
    UserResource.UpdatePasswordRequest updatePasswordRequest = new UserResource.UpdatePasswordRequest();
    updatePasswordRequest.setPassword(password);
    if (token.contains("?accountId")) {
      token = token.substring(0, token.indexOf("?accountId"));
    }
    Setup.portal()
        .pathParams("token", token)
        .body(updatePasswordRequest, ObjectMapperType.GSON)
        .post("/users/reset-password/{token}");
  }

  public static UserInvite completeUserRegistration(Account account, String bearerToken, UserInvite invite) {
    Registration registration = new Registration();
    registration.setAccountId(account.getAccountKey());
    registration.setAgreement(true);
    registration.setEmail(invite.getEmail());
    registration.setName(invite.getName());
    registration.setPassword(UserConstants.DEFAULT_PASSWORD);
    registration.setUuid(invite.getUuid());

    RestResponse<UserInvite> completed = Setup.portal()
                                             .auth()
                                             .oauth2(bearerToken)
                                             .queryParam("accountId", account.getUuid())
                                             .body(registration, ObjectMapperType.GSON)
                                             .contentType(ContentType.JSON)
                                             .put("/users/invites/" + invite.getUuid())
                                             .as(new GenericType<RestResponse<UserInvite>>() {}.getType());

    return completed.getResource();
  }

  public static User completePaidUserSignupAndSignin(
      String bearerToken, String accountId, String companyName, UserInvite invite) {
    Registration registration = new Registration();
    registration.setAgreement(true);
    registration.setEmail(invite.getEmail());
    registration.setName(invite.getName());
    registration.setPassword(UserConstants.DEFAULT_PASSWORD);
    registration.setUuid(invite.getUuid());

    RestResponse<User> completed = Setup.portal()
                                       .auth()
                                       .oauth2(bearerToken)
                                       .queryParam("company", companyName)
                                       .queryParam("accountId", accountId)
                                       .body(registration, ObjectMapperType.GSON)
                                       .contentType(ContentType.JSON)
                                       .put("/users/invites/" + invite.getUuid() + "/signin")
                                       .as(new GenericType<RestResponse<User>>() {}.getType());

    return completed.getResource();
  }

  public static User completeNewTrialUserSignup(String bearerToken, String inviteId) {
    RestResponse<User> completed = Setup.portal()
                                       .auth()
                                       .oauth2(bearerToken)
                                       .put("/users/invites/trial/" + inviteId + "/new-signin")
                                       .as(new GenericType<RestResponse<User>>() {}.getType());

    return completed.getResource();
  }

  public static Boolean createNewTrialInvite(UserInvite userInvite) {
    RestResponse<Boolean> trialInviteResponse = Setup.portal()
                                                    .body(userInvite, ObjectMapperType.GSON)
                                                    .contentType(ContentType.JSON)
                                                    .post("/users/new-trial")
                                                    .as(new GenericType<RestResponse<Boolean>>() {}.getType());

    return trialInviteResponse.getResource();
  }

  public static User unlockUser(String accountId, String bearerToken, String email) {
    RestResponse<User> userRestResponse = Setup.portal()
                                              .auth()
                                              .oauth2(bearerToken)
                                              .queryParam("accountId", accountId)
                                              .queryParam("email", email)
                                              .contentType(ContentType.JSON)
                                              .put("/users/unlock-user")
                                              .as(new GenericType<RestResponse<User>>() {}.getType());

    return userRestResponse.getResource();
  }

  public static Integer deleteUser(String accountId, String bearerToken, String userId) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .contentType(ContentType.JSON)
        .delete("/users/" + userId)
        .statusCode();
  }

  public static Collection<FeatureFlag> listFeatureFlags(String accountId, String bearerToken) {
    RestResponse<Collection<FeatureFlag>> featureFlagListResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .pathParam("accountId", accountId)
            .get("/users/feature-flags/{accountId}")
            .as(new GenericType<RestResponse<Collection<FeatureFlag>>>() {}.getType());

    return featureFlagListResponse.getResource();
  }
}
