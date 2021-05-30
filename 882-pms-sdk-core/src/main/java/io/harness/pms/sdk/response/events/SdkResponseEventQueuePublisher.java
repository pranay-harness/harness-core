package io.harness.pms.sdk.response.events;

import io.harness.pms.execution.SdkResponseEvent;
import io.harness.queue.QueuePublisher;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SdkResponseEventQueuePublisher {
  @Inject private QueuePublisher<SdkResponseEvent> pmsExecutionResponseEventQueuePublisher;

  public void send(SdkResponseEvent event) {
    pmsExecutionResponseEventQueuePublisher.send(event);
  }
}
