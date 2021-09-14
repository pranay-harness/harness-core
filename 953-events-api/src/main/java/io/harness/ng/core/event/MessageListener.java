/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.event;

import io.harness.eventsframework.consumer.Message;

public interface MessageListener {
  boolean handleMessage(Message message);
}
