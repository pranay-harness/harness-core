package io.harness.pms.sdk.core.response.publishers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.execution.SdkResponseEvent;
import io.harness.queue.QueuePublisher;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.PIPELINE)
public class MongoSdkResponseEventPublisher implements SdkResponseEventPublisher {
  @Inject private QueuePublisher<SdkResponseEvent> pmsExecutionResponseEventQueuePublisher;

  @Override
  public void publishEvent(SdkResponseEvent event) {
    pmsExecutionResponseEventQueuePublisher.send(event);
  }
}
