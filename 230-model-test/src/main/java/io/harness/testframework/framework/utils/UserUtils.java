/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.framework.utils;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.UUIDGenerator;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.constants.UserConstants;
import io.harness.testframework.framework.email.mailinator.MailinatorMessageDetails;
import io.harness.testframework.framework.email.mailinator.MailinatorMetaMessage;
import io.harness.testframework.restutils.HTMLUtils;
import io.harness.testframework.restutils.MailinatorRestUtils;
import io.harness.testframework.restutils.UserRestUtils;

import software.wings.beans.Account;
import software.wings.beans.PublicUser;
import software.wings.beans.UserInvite;

import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

@Slf4j
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class UserUtils {
  static final String EXPECTED_RESET_PWD_SUBJECT = "Reset your HARNESS PLATFORM password";

  public static PublicUser getUser(String bearerToken, String accountId, String emailId) {
    List<PublicUser> userList = UserRestUtils.getUserList(bearerToken, accountId);
    for (PublicUser user : userList) {
      if (user.getUser().getEmail().equals(emailId)) {
        return user;
      }
    }
    return null;
  }

  public static List<UserInvite> inviteUserAndValidateInviteMail(Account account, String bearerToken, String emailId,
      String domainName, String EXPECTED_SUBJECT) throws IOException, MessagingException {
    List<UserInvite> userInvitationList = UserRestUtils.inviteUser(account, bearerToken, emailId + domainName);
    assertThat(userInvitationList).isNotNull();
    assertThat(userInvitationList.size() == 1).isTrue();
    // Verify if email is sent, received and has signup link
    // Email check will run every 6 seconds upto 2 mins to see if email is delivered.
    log.info("Attempting to retrieve signup mail from inbox : " + emailId);
    MailinatorMetaMessage message = MailinatorRestUtils.retrieveMessageFromInbox(emailId, EXPECTED_SUBJECT);
    log.info("Signup mail retrieved");
    log.info("Reading the retrieved email");
    String emailFetchId = message.getId();
    MailinatorMessageDetails messageDetails = MailinatorRestUtils.readEmail(emailId, emailFetchId);
    assertThat(messageDetails).isNotNull();
    String inviteUrl =
        HTMLUtils.retrieveInviteUrlFromEmail(messageDetails.getData().getParts().get(0).getBody(), "SIGN UP");
    assertThat(inviteUrl).isNotNull();
    assertThat(StringUtils.isNotBlank(inviteUrl)).isTrue();
    log.info("Email read and Signup URL is available for user signup");

    messageDetails = MailinatorRestUtils.deleteEmail(emailId, emailFetchId);
    log.info("Email deleted for the inbox : " + emailId);
    assertThat(messageDetails.getAdditionalProperties()).isNotNull();
    assertThat(messageDetails.getAdditionalProperties().containsKey("status")).isNotNull();
    assertThat(messageDetails.getAdditionalProperties().get("status").toString().equals("ok")).isTrue();
    return userInvitationList;
  }

  public static UserInvite completeSignupAndValidateLogin(
      Account account, String bearerToken, List<UserInvite> userInvitationList) {
    // Complete registration using the API
    log.info("Entering user invite validation");
    UserInvite incomplete = userInvitationList.get(0);
    UserInvite completed = UserRestUtils.completeUserRegistration(account, bearerToken, incomplete);
    UserRestUtils.completePaidUserSignupAndSignin(bearerToken, account.getUuid(), "dummy", incomplete);
    assertThat(completed).isNotNull();
    assertThat(incomplete.isAgreement()).isFalse();
    assertThat(incomplete.isCompleted()).isFalse();
    assertThat(completed.isCompleted()).isTrue();
    // Assert.assertThat("Error : Agreement is false after signup",completed.isAgreement()).isTrue();
    log.info(incomplete.getAccountId() + ":" + incomplete.getEmail());
    assertThat(incomplete.getEmail().equals(completed.getEmail())).isTrue();
    assertThat(incomplete.getName().equals(completed.getName())).isTrue();
    assertThat(incomplete.getAccountId().equals(completed.getAccountId())).isTrue();
    // Verify if the signed-up user can login
    String newBearerToken = Setup.getAuthToken(completed.getEmail(), UserConstants.DEFAULT_PASSWORD);
    assertThat(newBearerToken).isNotNull();
    int statusCode = Setup.signOut(completed.getUuid(), newBearerToken);
    assertThat(statusCode == HttpStatus.SC_OK).isTrue();
    return completed;
  }

  public static void resetPasswordAndValidateLogin(UserInvite completed, String emailId, String domainName)
      throws IOException, MessagingException {
    UserRestUtils.sendResetPasswordMail(emailId + domainName);
    log.info("Attempting to retrieve reset password mail from inbox : " + emailId);
    MailinatorMetaMessage message = MailinatorRestUtils.retrieveMessageFromInbox(emailId, EXPECTED_RESET_PWD_SUBJECT);
    log.info("Reset password mail retrieved");
    log.info("Reading the retrieved email");
    String emailFetchId = message.getId();
    MailinatorMessageDetails messageDetails = MailinatorRestUtils.readEmail(emailId, emailFetchId);
    assertThat(messageDetails).isNotNull();
    String resetUrl =
        HTMLUtils.retrieveResetUrlFromEmail(messageDetails.getData().getParts().get(0).getBody(), "RESET PASSWORD");
    assertThat(StringUtils.isNotBlank(resetUrl)).isTrue();
    log.info(""
        + " URL is available for user password reset");
    UserRestUtils.resetPasswordWith(TestUtils.getResetTokenFromUrl(resetUrl), UserConstants.RESET_PASSWORD);
    // Verify if the user can login through the reset password
    String bearerToken = Setup.getAuthToken(completed.getEmail(), UserConstants.RESET_PASSWORD);
    assertThat(bearerToken).isNotNull();
    int statusCode = Setup.signOut(completed.getUuid(), bearerToken);
    assertThat(statusCode == HttpStatus.SC_OK).isTrue();
    // Delete Email
    messageDetails = MailinatorRestUtils.deleteEmail(emailId, emailFetchId);
    log.info("Email deleted for the inbox : " + emailId);
    assertThat(messageDetails.getAdditionalProperties()).isNotNull();
    assertThat(messageDetails.getAdditionalProperties().containsKey("status")).isNotNull();
    assertThat(messageDetails.getAdditionalProperties().get("status").toString().equals("ok")).isTrue();
    log.info("All validation completed");
    log.info("All validation for reset also done");
  }

  public static UserInvite createUserInvite(Account account, String emailId) {
    UserInvite invite = new UserInvite();
    invite.setAccountId(account.getUuid());
    List<String> emailList = new ArrayList<>();
    emailList.add(emailId);
    invite.setEmails(emailList);
    invite.setName(emailId.replace("@harness.mailinator.com", ""));
    invite.setAppId(account.getAppId());
    return invite;
  }

  public static boolean attemptInvite(UserInvite invite, Account account, String bearerToken, int status) {
    return Setup.portal()
               .auth()
               .oauth2(bearerToken)
               .queryParam("accountId", account.getUuid())
               .body(invite, ObjectMapperType.GSON)
               .contentType(ContentType.JSON)
               .post("/users/invites")
               .getStatusCode()
        == status;
  }

  public static UserInvite getTrialSignUpInvite(String email, String password) {
    String inviteId = UUIDGenerator.generateUuid();
    UserInvite invite = new UserInvite();
    invite.setEmail(email);
    invite.setName(email.replace("@harness.mailinator.com", ""));
    invite.setUuid(inviteId);
    String accountName = TestUtils.generateRandomUUID();
    String companyName = TestUtils.generateRandomUUID();
    invite.setAccountName(accountName);
    invite.setCompanyName(companyName);
    invite.setPassword(password.toCharArray());
    return invite;
  }
}
