package io.harness.restutils;

import static org.junit.Assert.assertNotNull;

import io.harness.beans.PageResponse;
import io.harness.framework.Registration;
import io.harness.framework.Setup;
import io.harness.framework.constants.UserConstants;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.resources.UserResource;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.GenericType;

public class UserRestUtils {
  public static List<User> getUserList(String bearerToken, String accountId) {
    RestResponse<PageResponse<User>> userRestResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .get("/users")
            .as(new GenericType<RestResponse<PageResponse<User>>>() {}.getType());
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
    assertNotNull(inviteList);
    return inviteList;
  }

  public static void sendResetPasswordMail(String emailId) {
    UserResource.ResetPasswordRequest resetPasswordRequest = new UserResource.ResetPasswordRequest();
    resetPasswordRequest.setEmail(emailId);
    Setup.portal().body(resetPasswordRequest, ObjectMapperType.GSON).post("/users/reset-password");
  }

  public static void resetPasswordWith(String token, String password) throws UnsupportedEncodingException {
    UserResource.UpdatePasswordRequest updatePasswordRequest = new UserResource.UpdatePasswordRequest();
    updatePasswordRequest.setPassword(password);
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

  public static UserInvite completeTrialUserSignup(
      String bearerToken, String accountName, String companyName, UserInvite invite) {
    Registration registration = new Registration();
    registration.setAgreement(true);
    registration.setEmail(invite.getEmail());
    registration.setName(invite.getName());
    registration.setPassword(UserConstants.DEFAULT_PASSWORD);
    registration.setUuid(invite.getUuid());

    RestResponse<UserInvite> completed = Setup.portal()
                                             .auth()
                                             .oauth2(bearerToken)
                                             .queryParam("account", accountName)
                                             .queryParam("company", companyName)
                                             .body(registration, ObjectMapperType.GSON)
                                             .contentType(ContentType.JSON)
                                             .put("/users/invites/trial/" + invite.getUuid())
                                             .as(new GenericType<RestResponse<UserInvite>>() {}.getType());

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

  public static User completeTrialUserSignupAndSignin(
      String bearerToken, String accountName, String companyName, UserInvite invite) {
    Registration registration = new Registration();
    registration.setAgreement(true);
    registration.setEmail(invite.getEmail());
    registration.setName(invite.getName());
    registration.setPassword(UserConstants.DEFAULT_PASSWORD);
    registration.setUuid(invite.getUuid());

    RestResponse<User> completed = Setup.portal()
                                       .auth()
                                       .oauth2(bearerToken)
                                       .queryParam("account", accountName)
                                       .queryParam("company", companyName)
                                       .body(registration, ObjectMapperType.GSON)
                                       .contentType(ContentType.JSON)
                                       .put("/users/invites/trial/" + invite.getUuid() + "/signin")
                                       .as(new GenericType<RestResponse<User>>() {}.getType());

    return completed.getResource();
  }

  public static Boolean createTrialInvite(String emailId) {
    RestResponse<Boolean> trialInviteResponse = Setup.portal()
                                                    .body(emailId)
                                                    .contentType(ContentType.TEXT)
                                                    .post("/users/trial")
                                                    .as(new GenericType<RestResponse<Boolean>>() {}.getType());

    return trialInviteResponse.getResource();
  }

  public static Boolean createNewTrialInvite(UserInvite userInvite) {
    RestResponse<Boolean> trialInviteResponse = Setup.portal()
                                                    .body(userInvite, ObjectMapperType.GSON)
                                                    .contentType(ContentType.JSON)
                                                    .post("/users/new-trial")
                                                    .as(new GenericType<RestResponse<Boolean>>() {}.getType());

    return trialInviteResponse.getResource();
  }
}
