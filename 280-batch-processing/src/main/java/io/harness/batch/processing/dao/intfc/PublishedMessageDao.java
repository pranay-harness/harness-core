/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.dao.intfc;

import io.harness.ccm.commons.entities.events.PublishedMessage;

import java.util.List;

public interface PublishedMessageDao {
  List<PublishedMessage> fetchPublishedMessage(
      String accountId, String messageType, Long startTime, Long endTime, int batchSize);
}
