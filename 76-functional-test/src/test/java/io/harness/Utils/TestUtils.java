package io.harness.Utils;

import io.harness.RestUtils.MailinatorRestUtils;
import io.harness.framework.Retry;
import io.harness.framework.email.mailinator.MailinatorInbox;
import io.harness.framework.matchers.MailinatorEmailMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
}
