package io.harness.eventsframework;

import io.harness.logging.AutoLogContext;

public class NgEventLogContext extends AutoLogContext {
  private static String MESSAGE_ID = "messageId";

  public NgEventLogContext(String messageId, OverrideBehavior behavior) {
    super(MESSAGE_ID, messageId, behavior);
  }
}
