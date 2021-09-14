/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.testframework.restutils;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.testframework.framework.Retry;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.email.mailinator.MailinatorInbox;
import io.harness.testframework.framework.email.mailinator.MailinatorMessageDetails;
import io.harness.testframework.framework.email.mailinator.MailinatorMetaMessage;
import io.harness.testframework.framework.matchers.MailinatorEmailMatcher;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
@Slf4j
public class MailinatorRestUtils {
  static final int MAX_RETRIES = 60;
  static final int DELAY_IN_MS = 6000;
  static Retry<Object> retry = new Retry<>(MAX_RETRIES, DELAY_IN_MS);

  public static MailinatorInbox retrieveInbox(String inboxName) {
    return Setup.mailinator().queryParam("to", inboxName).get("/inbox").as(MailinatorInbox.class);
  }

  public static MailinatorMessageDetails readEmail(String inboxName, String emailFetchId) {
    return Setup.mailinator()
        .queryParam("to", inboxName)
        .queryParam("id", emailFetchId)
        .get("/email")
        .as(MailinatorMessageDetails.class);
  }

  public static MailinatorMessageDetails deleteEmail(String inboxName, String emailFetchId) {
    return Setup.mailinator()
        .queryParam("to", inboxName)
        .queryParam("id", emailFetchId)
        // .contentType("text/html;charset=utf-8")
        .get("/delete")
        .as(MailinatorMessageDetails.class);
  }

  public static MailinatorMetaMessage retrieveMessageFromInbox(String inboxName, final String EXPECTED_SUBJECT) {
    MailinatorInbox inbox = (MailinatorInbox) retry.executeWithRetry(
        () -> retrieveInbox(inboxName), new MailinatorEmailMatcher<>(), EXPECTED_SUBJECT);
    assertThat(inbox.getMessages()).isNotNull();
    List<MailinatorMetaMessage> messages = inbox.getMessages();
    MailinatorMetaMessage messageToReturn[] = new MailinatorMetaMessage[1];
    messages.forEach(message -> {
      if (message.getSubject().equals(EXPECTED_SUBJECT)) {
        messageToReturn[0] = message;
      }
    });
    log.info("Message successfully retrieved from inbox");
    return messageToReturn[0];
  }
}
