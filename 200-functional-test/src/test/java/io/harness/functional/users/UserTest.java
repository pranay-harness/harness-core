/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.users;

import static io.harness.rule.OwnerRule.NATARAJA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator;
import io.harness.generator.SettingGenerator.Settings;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Retry;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.constants.UserConstants;
import io.harness.testframework.framework.email.mailinator.MailinatorMessageDetails;
import io.harness.testframework.framework.email.mailinator.MailinatorMetaMessage;
import io.harness.testframework.framework.utils.TestUtils;
import io.harness.testframework.restutils.HTMLUtils;
import io.harness.testframework.restutils.MailinatorRestUtils;
import io.harness.testframework.restutils.UserRestUtils;

import software.wings.beans.Account;
import software.wings.beans.PublicUser;
import software.wings.beans.SettingAttribute;
import software.wings.beans.UserInvite;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.List;
import javax.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
@Slf4j
public class UserTest extends AbstractFunctionalTest {
  @Inject private SettingGenerator settingGenerator;
  @Inject private OwnerManager ownerManager;

  final int MAX_RETRIES = 20;
  final int DELAY_IN_MS = 6000;
  final Retry<Object> retry = new Retry<>(MAX_RETRIES, DELAY_IN_MS);
  final String EXPECTED_SUBJECT = "You have been invited to join the Harness account at Harness";
  final String EXPECTED_RESET_PWD_SUBJECT = "Reset your HARNESS PLATFORM password";
  final Seed seed = new Seed(0);
  Owners owners;

  @Before
  public void userTestSetup() {
    owners = ownerManager.create();
    SettingAttribute emailSettingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.PAID_EMAIL_SMTP_CONNECTOR);
    assertThat(emailSettingAttribute).isNotNull();
    log.info("Setup completed successfully");
  }

  @Test()
  @Owner(developers = NATARAJA, intermittent = true)
  @Category(FunctionalTests.class)
  public void listUsers() {
    log.info("Starting the list users test");
    Account account = this.getAccount();
    UserRestUtils urUtil = new UserRestUtils();
    List<PublicUser> userList = urUtil.getUserList(bearerToken, account.getUuid());
    assertThat(userList).isNotNull();
    assertThat(userList.size() > 0).isTrue();
  }

  @Owner(developers = NATARAJA, intermittent = true)
  @Category(FunctionalTests.class)
  public void testUserInvite() throws IOException, MessagingException {
    Account account = this.getAccount();
    String domainName = "@harness.mailinator.com";
    String emailId = TestUtils.generateUniqueInboxId();
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

    messageDetails = null;
    messageDetails = MailinatorRestUtils.deleteEmail(emailId, emailFetchId);
    log.info("Email deleted for the inbox : " + emailId);
    assertThat(messageDetails.getAdditionalProperties()).isNotNull();
    assertThat(messageDetails.getAdditionalProperties().containsKey("status")).isNotNull();
    assertThat(messageDetails.getAdditionalProperties().get("status").toString().equals("ok")).isTrue();

    // Complete registration using the API
    log.info("Entering user invite validation");
    UserInvite incomplete = userInvitationList.get(0);
    UserInvite completed = UserRestUtils.completeUserRegistration(account, bearerToken, incomplete);
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
    String bearerToken = Setup.getAuthToken(completed.getEmail(), UserConstants.DEFAULT_PASSWORD);
    assertThat(bearerToken).isNotNull();
    int statusCode = Setup.signOut(completed.getUuid(), bearerToken);
    assertThat(statusCode == HttpStatus.SC_OK).isTrue();

    // Verify user can reset the password
    emailId = emailId + domainName;
    UserRestUtils.sendResetPasswordMail(emailId);
    log.info("Attempting to retrieve reset password mail from inbox : " + emailId);
    message = MailinatorRestUtils.retrieveMessageFromInbox(emailId, EXPECTED_RESET_PWD_SUBJECT);
    log.info("Reset password mail retrieved");
    log.info("Reading the retrieved email");
    emailFetchId = message.getId();
    messageDetails = MailinatorRestUtils.readEmail(emailId, emailFetchId);
    assertThat(messageDetails).isNotNull();
    String resetUrl =
        HTMLUtils.retrieveResetUrlFromEmail(messageDetails.getData().getParts().get(0).getBody(), "RESET PASSWORD");
    assertThat(StringUtils.isNotBlank(resetUrl)).isTrue();
    log.info(""
        + " URL is available for user password reset");
    UserRestUtils.resetPasswordWith(TestUtils.getResetTokenFromUrl(resetUrl), UserConstants.RESET_PASSWORD);
    // Verify if the user can login through the reset password
    bearerToken = Setup.getAuthToken(completed.getEmail(), UserConstants.RESET_PASSWORD);
    assertThat(bearerToken).isNotNull();
    statusCode = Setup.signOut(completed.getUuid(), bearerToken);
    assertThat(statusCode == HttpStatus.SC_OK).isTrue();
    // Delete Email
    messageDetails = null;
    messageDetails = MailinatorRestUtils.deleteEmail(emailId, emailFetchId);
    log.info("Email deleted for the inbox : " + emailId);
    assertThat(messageDetails.getAdditionalProperties()).isNotNull();
    assertThat(messageDetails.getAdditionalProperties().containsKey("status")).isNotNull();
    assertThat(messageDetails.getAdditionalProperties().get("status").toString().equals("ok")).isTrue();

    log.info("All validation completed");
    log.info("All validation for reset also done");
  }
}
