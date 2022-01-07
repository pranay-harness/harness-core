/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.framework.utils;

import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.framework.Retry;
import io.harness.testframework.framework.constants.FrameworkConstants;
import io.harness.testframework.framework.email.mailinator.MailinatorInbox;
import io.harness.testframework.framework.matchers.MailinatorEmailMatcher;
import io.harness.testframework.restutils.MailinatorRestUtils;

import software.wings.beans.alert.AlertNotificationRule;
import software.wings.cdn.CdnConfig;
import software.wings.helpers.ext.mail.SmtpConfig;

import com.google.common.base.Splitter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestUtils {
  static final int MAX_RETRIES = 5;
  static final int DELAY_IN_MS = 6000;
  static final Retry<Object> retry = new Retry<>(MAX_RETRIES, DELAY_IN_MS);
  final String EXPECTED_SUBJECT = "You are invited to join Harness at Harness platform";

  public static String generateUniqueInboxId() {
    String emailId = UUID.randomUUID().toString() + System.currentTimeMillis();
    int count = 0;
    while (count < 10) {
      String finalEmailId = emailId;
      MailinatorInbox inbox = (MailinatorInbox) retry.executeWithRetry(
          () -> MailinatorRestUtils.retrieveInbox(finalEmailId), new MailinatorEmailMatcher<>(), null);
      if (inbox.getMessages().size() == 0) {
        return finalEmailId;
      } else {
        log.info("Email id collission detected : " + emailId);
        log.info("Recreating email id");
        emailId = UUID.randomUUID().toString() + System.currentTimeMillis();
      }
    }
    return new StringBuilder(emailId).reverse().toString();
  }

  public static String generateRandomUUID() {
    return UUID.randomUUID().toString();
  }

  public static SmtpConfig getDefaultSmtpConfig() {
    String secret = new ScmSecret().decryptToString(new SecretName("smtp_paid_sendgrid_config_password"));
    // TODO: Use encrypted secrets
    // String encryptedSecret = new ScmSecret().getSecrets().getProperty("smtp_paid_sendgrid_config_password");
    return SmtpConfig.builder()
        .host("smtp.sendgrid.net")
        .port(465)
        .useSSL(true)
        .fromAddress("automation@harness.io")
        .username("apikey")
        .password(secret.toCharArray())
        .build();
  }

  public static CdnConfig getDefaultCdnConfig() {
    String secret = new ScmSecret().decryptToString(new SecretName("cdn_key_secret"));
    CdnConfig cdnConfig = new CdnConfig();
    cdnConfig.setKeySecret(secret);
    return cdnConfig;
  }

  public static String getInviteIdFromUrl(String url) {
    URLConnection con = null;
    String referredUrl = "";
    try {
      con = new URL(url).openConnection();
      con.getInputStream();
    } catch (IOException e) {
      if (con != null && con.getURL().toString().contains("inviteId")) {
        referredUrl = con.getURL().toString();
      }
    }
    if (con != null && con.getURL().toString().contains("inviteId")) {
      referredUrl = con.getURL().toString();
    }

    return referredUrl;
  }

  public static String validateAndGetTrialUserInviteFromUrl(String url) {
    URLConnection con = null;
    String inviteId = "";
    try {
      con = new URL(url).openConnection();
      con.getInputStream();
    } catch (IOException e) {
      if (con != null && con.getURL().toString().contains("userInviteId")) {
        String query = con.getURL().getQuery();
        Map<String, String> split = Splitter.on("&").withKeyValueSeparator("=").split(query);
        inviteId = split.get("userInviteId");
      }
    }
    if (con != null && con.getURL().toString().contains("userInviteId")) {
      String query = con.getURL().getQuery();
      Map<String, String> split = Splitter.on("&").withKeyValueSeparator("=").split(query);
      inviteId = split.get("userInviteId");
    }
    return inviteId;
  }

  public static String getResetTokenFromUrl(String url) {
    URLConnection connection = null;
    String resetToken = "";
    String referredUrl = "";
    try {
      connection = new URL(url).openConnection();
      connection.getInputStream();
      if (connection != null && connection.getURL().toString().contains("reset-password")) {
        referredUrl = connection.getURL().toString();
      }
    } catch (IOException ie) {
      if (connection != null && connection.getURL().toString().contains("reset-password")) {
        referredUrl = connection.getURL().toString();
      }
    }
    if (referredUrl.contains("reset-password")) {
      resetToken = referredUrl.split("reset-password/")[1];
      return resetToken;
    }
    return resetToken;
  }

  public static JsonObject massageAlertNotificationRules(AlertNotificationRule alertNotificationRule) {
    Gson gson = new Gson();
    String jsonString = gson.toJson(alertNotificationRule);
    return gson.fromJson(jsonString, JsonObject.class);
  }

  public static String getExecutionEnvironment() {
    return System.getProperty("test.env", FrameworkConstants.LOCAL_ENV);
  }

  public static String getDecryptedValue(String secretKey) {
    return new ScmSecret().decryptToString(new SecretName(secretKey));
  }

  public static void sleep(int sleepTimeInSeconds) {
    try {
      TimeUnit.SECONDS.sleep(sleepTimeInSeconds);
    } catch (InterruptedException e) {
      log.error("", e);
    }
  }
}
