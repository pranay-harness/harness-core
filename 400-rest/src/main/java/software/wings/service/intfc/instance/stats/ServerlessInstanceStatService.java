/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.instance.stats;

import software.wings.beans.infrastructure.instance.InvocationCount.InvocationCountKey;
import software.wings.beans.infrastructure.instance.stats.ServerlessInstanceStats;
import software.wings.resources.stats.model.ServerlessInstanceTimeline;

import java.time.Instant;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This service is used to render the timelines and provide aggregates on user dashboard.
 */
@ParametersAreNonnullByDefault
public interface ServerlessInstanceStatService {
  boolean save(ServerlessInstanceStats stats);
  @Nullable Instant getLastSnapshotTime(String accountId);
  @Nullable Instant getFirstSnapshotTime(String accountId);
  ServerlessInstanceTimeline aggregate(String accountId, long from, long to, InvocationCountKey invocationCountKey);
}
