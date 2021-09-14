/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.grpc;

import io.harness.ccm.commons.entities.events.PublishedMessage;

public interface MessageProcessor {
  void process(PublishedMessage publishedMessage);
}
