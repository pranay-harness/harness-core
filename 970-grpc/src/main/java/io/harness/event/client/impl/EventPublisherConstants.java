package io.harness.event.client.impl;

import lombok.experimental.UtilityClass;
import net.openhft.chronicle.queue.RollCycles;

import java.time.Duration;

@UtilityClass
public class EventPublisherConstants {
  public static final String DEFAULT_QUEUE_FILE_PATH = "eventQueue";
  public static final RollCycles QUEUE_ROLL_CYCLE = RollCycles.MINUTELY;
  public static final long QUEUE_TIMEOUT_MS = Duration.ofSeconds(30).toMillis();
}
