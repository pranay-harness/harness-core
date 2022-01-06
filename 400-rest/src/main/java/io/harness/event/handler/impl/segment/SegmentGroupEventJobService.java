/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
