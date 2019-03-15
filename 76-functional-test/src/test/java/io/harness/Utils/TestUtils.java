package io.harness.Utils;

import io.harness.RestUtils.MailinatorRestUtils;
import io.harness.framework.Retry;
import io.harness.framework.email.mailinator.MailinatorInbox;
import io.harness.framework.matchers.MailinatorEmailMatcher;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.helpers.ext.mail.SmtpConfig;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

public class TestUtils {
  final int MAX_RETRIES = 5;
  final int DELAY_IN_MS = 6000;
  final Retry<Object> retry = new Retry<>(MAX_RETRIES, DELAY_IN_MS);
  MailinatorRestUtils mailinatorRestUtils = new MailinatorRestUtils();
  final String EXPECTED_SUBJECT = "You are invited to join Harness at Harness platform";
  private static final Logger logger = LoggerFactory.getLogger(TestUtils.class);

  public String generateUniqueInboxId() {
    String emailId = UUID.randomUUID().toString() + System.currentTimeMillis();
    int count = 0;
    while (count < 10) {
      String finalEmailId = emailId;
      MailinatorInbox inbox = (MailinatorInbox) retry.executeWithRetry(
          () -> mailinatorRestUtils.retrieveInbox(finalEmailId), new MailinatorEmailMatcher<>(), null);
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
}
