package io.harness.engine.interrupts;

import io.harness.pms.interrupts.InterruptEvent;
import io.harness.queue.QueuePublisher;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class InterruptEventQueuePublisher {
  @Inject QueuePublisher<InterruptEvent> interruptEventQueuePublisher;

  public void send(String topic, InterruptEvent event) {
    interruptEventQueuePublisher.send(Collections.singletonList(topic), event);
  }
}
