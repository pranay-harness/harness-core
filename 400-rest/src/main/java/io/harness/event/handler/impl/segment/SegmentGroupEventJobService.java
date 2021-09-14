/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.handler.impl.segment;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.scheduler.events.segment.SegmentGroupEventJobContext;

@OwnedBy(PL)
public interface SegmentGroupEventJobService {
  int ACCOUNT_BATCH_SIZE = 10;

  void scheduleJob(String accountId);

  SegmentGroupEventJobContext get(String uuid);
}
