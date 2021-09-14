/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.jobs;

import io.harness.eventsframework.consumer.Message;

public interface ConsumerMessageProcessor {
  void processMessage(Message message);
}
