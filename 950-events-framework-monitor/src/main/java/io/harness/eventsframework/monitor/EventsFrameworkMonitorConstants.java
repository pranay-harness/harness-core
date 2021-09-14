/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.eventsframework.monitor;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PL)
@UtilityClass
public class EventsFrameworkMonitorConstants {
  public static String REDIS_STREAMS_LENGTH_METRIC = "redis_streams_length";
  public static String REDIS_STREAMS_MEMORY_USAGE = "redis_streams_memory_usage";
  public static String REDIS_STREAMS_AVERAGE_MESSAGE_SIZE = "redis_streams_average_message_size";
  public static String REDIS_STREAMS_EVENTS_FRAMEWORK_DEADLETTER_QUEUE_SIZE =
      "redis_streams_events_framework_deadletter_queue_size";

  public static String REDIS_STREAMS_CONSUMER_GROUP_PENDING_COUNT = "redis_streams_consumer_group_pending_count";
  public static String REDIS_STREAMS_CONSUMER_GROUP_BEHIND_BY_COUNT = "redis_streams_consumer_group_behind_by_count";
}
