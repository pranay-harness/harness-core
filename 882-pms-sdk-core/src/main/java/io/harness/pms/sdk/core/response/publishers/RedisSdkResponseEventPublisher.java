package io.harness.pms.sdk.core.response.publishers;

import static io.harness.pms.sdk.core.PmsSdkCoreEventsFrameworkConstants.SDK_RESPONSE_EVENT_PRODUCER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@OwnedBy(HarnessTeam.PIPELINE)
public class RedisSdkResponseEventPublisher implements SdkResponseEventPublisher {
  @Inject @Named(SDK_RESPONSE_EVENT_PRODUCER) private Producer eventProducer;

  @Override
  public void publishEvent(SdkResponseEventProto event) {
    eventProducer.send(Message.newBuilder().setData(event.toByteString()).build());
  }
}
