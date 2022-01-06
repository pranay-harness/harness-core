/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
