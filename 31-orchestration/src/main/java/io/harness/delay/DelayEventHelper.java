package io.harness.delay;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.queue.QueuePublisher;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;

@Singleton
@Slf4j
public class DelayEventHelper {
  @Inject private QueuePublisher<DelayEvent> delayQueue;

  public String delay(long delayTimeInSeconds, Map<String, String> context) {
    try {
      String resumeId = generateUuid();
      delayQueue.send(new DelayEvent(
          resumeId, Date.from(OffsetDateTime.now().plusSeconds(delayTimeInSeconds).toInstant()), context));
      logger.info("DelayEvent with resumeId {} queued - delayTimeInSeconds: {}", resumeId, delayTimeInSeconds);
      return resumeId;
    } catch (Exception exception) {
      logger.error("Failed to create Delay event", exception);
    }
    return null;
  }
}
