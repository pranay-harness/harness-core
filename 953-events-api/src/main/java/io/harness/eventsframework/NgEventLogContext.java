/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.eventsframework;

import io.harness.logging.AutoLogContext;

public class NgEventLogContext extends AutoLogContext {
  private static String MESSAGE_ID = "messageId";

  public NgEventLogContext(String messageId, OverrideBehavior behavior) {
    super(MESSAGE_ID, messageId, behavior);
  }
}
