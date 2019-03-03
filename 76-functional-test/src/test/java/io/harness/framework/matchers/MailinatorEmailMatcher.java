package io.harness.framework.matchers;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

import io.harness.framework.email.mailinator.MailinatorInbox;
import io.harness.framework.email.mailinator.MailinatorMetaMessage;

import java.util.List;

public class MailinatorEmailMatcher<T> implements Matcher {
  @Override
  public boolean matches(Object expected, Object actual) {
    String subject = (String) expected;
    if (actual instanceof MailinatorInbox) {
      MailinatorInbox inbox = (MailinatorInbox) actual;
      assertNotNull(inbox);
      List<MailinatorMetaMessage> messages = inbox.getMessages();
      assertTrue(messages != null && messages.size() > 0);
      for (MailinatorMetaMessage message : messages) {
        if (message.getSubject().equals(subject)) {
          return true;
        }
      }
    }
    return false;
  }
}
