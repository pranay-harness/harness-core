/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.service.intfc;

import io.harness.ccm.commons.entities.events.PublishedMessage;

import java.util.List;

public interface LastReceivedPublishedMessageRepository {
  void updateLastReceivedPublishedMessages(List<PublishedMessage> publishedMessages);
}
