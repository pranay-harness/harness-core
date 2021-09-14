/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.eventsframework.api;

import io.harness.eventsframework.producer.Message;

public interface Producer {
  String send(Message message);
  void shutdown();
}
