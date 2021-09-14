/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.client.impl;

import java.time.Duration;
import lombok.experimental.UtilityClass;
import net.openhft.chronicle.queue.RollCycles;

@UtilityClass
public class EventPublisherConstants {
  public static final String DEFAULT_QUEUE_FILE_PATH = "eventQueue";
  public static final RollCycles QUEUE_ROLL_CYCLE = RollCycles.MINUTELY;
  public static final long QUEUE_TIMEOUT_MS = Duration.ofSeconds(30).toMillis();
}
