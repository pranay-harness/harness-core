package io.harness.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.harness.framework.Retry;
import io.harness.framework.email.mailinator.MailinatorInbox;
import io.harness.framework.matchers.MailinatorEmailMatcher;
import io.harness.restutils.MailinatorRestUtils;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.alert.AlertNotificationRule;
import software.wings.helpers.ext.mail.SmtpConfig;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;
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
        logger.info("Email id collission detected : " + emailId);
        logger.info("Recreating email id");
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

  public static String getInviteIdFromUrl(String url) {
    URLConnection con = null;
    String referredUrl = "";
    try {
      con = new URL(url).openConnection();
      InputStream inputStream = con.getInputStream();
    } catch (IOException e) {
      if (con.getURL().toString().contains("inviteId")) {
        referredUrl = con.getURL().toString();
      }
    }
    if (con.getURL().toString().contains("inviteId")) {
      referredUrl = con.getURL().toString();
    }

    return referredUrl;
  }

  public static String getResetTokenFromUrl(String url) {
    URLConnection connection = null;
    String resetToken = "";
    String referredUrl = "";
    try {
      connection = new URL(url).openConnection();
      InputStream inputStream = connection.getInputStream();
    } catch (IOException ie) {
      if (connection.getURL().toString().contains("reset-password")) {
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
    JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
    return jsonObject;
  }
}
