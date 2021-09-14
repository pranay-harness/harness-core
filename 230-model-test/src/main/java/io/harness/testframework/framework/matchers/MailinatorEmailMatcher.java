/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.testframework.framework.matchers;

import io.harness.testframework.framework.email.mailinator.MailinatorInbox;
import io.harness.testframework.framework.email.mailinator.MailinatorMetaMessage;

import java.util.List;

public class MailinatorEmailMatcher<T> implements Matcher {
  @Override
  public boolean matches(Object expected, Object actual) {
    if (actual instanceof MailinatorInbox) {
      MailinatorInbox inbox = (MailinatorInbox) actual;
      List<MailinatorMetaMessage> messages = inbox.getMessages();
      if (expected == null) {
        if (messages.size() == 0) {
          return true;
        } else {
          return false;
        }
      }

      String subject = (String) expected;
      for (MailinatorMetaMessage message : messages) {
        if (message.getSubject().equals(subject)) {
          return true;
        }
      }
    }
    return false;
  }
}
